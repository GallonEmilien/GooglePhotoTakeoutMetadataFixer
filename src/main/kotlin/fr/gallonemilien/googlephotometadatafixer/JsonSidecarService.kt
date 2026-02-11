package fr.gallonemilien.googlephotometadatafixer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

private val logger = KotlinLogging.logger {}

/**
 * Service responsible for locating and parsing JSON sidecar files.
 * It handles Google Photos specific naming conventions and truncation logic.
 */
class JsonSidecarService {
    private val mapper = jacksonObjectMapper()

    /**
     * Scans the directory and builds a cache of JSON files.
     * The key is the base filename (without .json extension).
     *
     * @param inputDir The directory to scan.
     * @return A map of filename keys to JSON file paths.
     */
    fun buildJsonCache(inputDir: Path): Map<String, Path> {
        return try {
            Files.walk(inputDir)
                .asSequence()
                .filter { it.toString().endsWith(".json", ignoreCase = true) }
                .associateBy { 
                    val name = it.fileName.toString()
                    name.removeSuffix(".json")
                        .substringBeforeLast(".")
                }
        } catch (e: Exception) {
            logger.error(e) { "Failed to build JSON cache from directory: $inputDir" }
            emptyMap()
        }
    }

    /**
     * Attempts to find the corresponding JSON file for a given media file.
     * It tries exact matches, extension-stripped matches, and truncated matches.
     *
     * @param mediaFile The media file (image/video).
     * @param jsonCache The pre-computed cache of JSON files.
     * @return The path to the JSON file, or null if not found.
     */
    fun findJsonForFile(mediaFile: Path, jsonCache: Map<String, Path>): Path? {
        val fileName = mediaFile.fileName.toString()

        // Try exact match (e.g., "IMG.jpg" -> "IMG.jpg.json")
        // The cache key for "IMG.jpg.json" is "IMG.jpg"
        var jsonPath = jsonCache[fileName]

        // Try name without extension (e.g., "IMG.jpg" -> "IMG.json")
        // The cache key for "IMG.json" is "IMG"
        if (jsonPath == null) {
            jsonPath = jsonCache[fileName.substringBeforeLast(".")]
        }

        // Try truncated match (Google truncates at 46 chars)
        if (jsonPath == null && fileName.length > 46) {
            val truncated = fileName.take(46)
            jsonPath = jsonCache[truncated]
            if (jsonPath != null) {
                logger.debug { "Found truncated match for $fileName -> ${jsonPath.fileName}" }
            }
        }

        return jsonPath
    }

    /**
     * Parses the JSON file and returns the root node.
     */
    fun parseJson(jsonPath: Path): JsonNode? {
        return try {
            mapper.readTree(jsonPath.toFile())
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse JSON ${jsonPath.fileName}" }
            null
        }
    }
}