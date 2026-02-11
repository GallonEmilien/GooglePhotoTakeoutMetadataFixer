package fr.gallonemilien.googlephotometadatafixer

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.nio.file.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.*
private val logger = KotlinLogging.logger {}

/**
 * Processor responsible for the high-level workflow.
 */
class TakeoutProcessor {
    private val metadataManager = MetadataManager()
    private val jsonSidecarService = JsonSidecarService()

    /**
     * Starts the processing of files using maximum parallelism.
     *
     * @param input Source directory path.
     * @param output Destination directory path.
     * @param onProgress Suspending callback for progress updates.
     */
    suspend fun process(
        input: Path, 
        output: Path, 
        onProgress: suspend (Int, Int, Long, Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        logger.info { "Starting processing from input: $input" }

        logger.info { "Building JSON cache..." }
        val jsonCache = jsonSidecarService.buildJsonCache(input)
        logger.info { "JSON cache built." }

        logger.info { "Scanning for media files..." }
        val mediaFiles = Files.walk(input)
            .filter { it.isRegularFile() && !it.toString().endsWith(".json", ignoreCase = true) && !it.name.contains("metadata") }
            .toList()

        val totalFiles = mediaFiles.size
        val totalSize = mediaFiles.sumOf { it.fileSize() }
        logger.info { "Found $totalFiles media files. Starting parallel processing..." }
        
        val completedFiles = AtomicInteger(0)
        val completedSize = AtomicLong(0)

        // Determine Max Parallelism
        // We use (Cores * 4) to saturate both CPU (metadata) and IO (copy)
        // Capped at 64 to avoid "Too many open files" OS errors
        val cores = Runtime.getRuntime().availableProcessors()
        val parallelism = (cores * 4).coerceAtMost(64)
        val semaphore = Semaphore(parallelism)
        
        logger.info { "Parallelism level set to: $parallelism threads" }

        val progressJob = launch {
            while (isActive) {
                onProgress(
                    completedFiles.get(),
                    totalFiles,
                    completedSize.get(),
                    totalSize
                )
                delay(100) // Refresh rate: 10Hz
            }
        }

        // Sliding Window Processing
        // Create a job for every file, but execution is gated by the Semaphore
        val jobs = mediaFiles.map { file ->
            launch {
                semaphore.withPermit {
                    processSingleFile(file, output, jsonCache)
                    completedFiles.incrementAndGet()
                    completedSize.addAndGet(file.fileSize())
                }
            }
        }

        jobs.joinAll()
        
        // Stop the UI ticker and force one last 100% update
        progressJob.cancelAndJoin()
        onProgress(totalFiles, totalFiles, totalSize, totalSize)
        
        logger.info { "Processing completed successfully." }
    }

    /**
     * Processes a single media file: copies it and applies metadata.
     */
    private fun processSingleFile(file: Path, outputDir: Path, jsonCache: Map<String, Path>) {
        try {
            val fileName = file.fileName.toString()
            val target = outputDir.resolve(fileName)
            
            // Copy file first
            Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING)

            // Find and apply metadata
            val jsonPath = jsonSidecarService.findJsonForFile(file, jsonCache)
            if (jsonPath != null) {
                metadataManager.applyMetadata(jsonPath, target)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error processing file $file" }
        }
    }
}