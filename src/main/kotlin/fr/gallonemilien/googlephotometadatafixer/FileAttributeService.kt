package fr.gallonemilien.googlephotometadatafixer

import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime

private val logger = KotlinLogging.logger {}

/**
 * Service responsible for modifying file system attributes.
 * It handles creation time, last modified time, and last access time.
 */
class FileAttributeService {

    /**
     * Applies the timestamp to the file system attributes of the target file.
     *
     * @param targetFile The file to update.
     * @param timestamp The timestamp in seconds (Unix epoch).
     */
    fun applyTimestamps(targetFile: Path, timestamp: Long) {
        try {
            val millis = timestamp * 1000
            val fileTime = FileTime.fromMillis(millis)
            
            Files.setAttribute(targetFile, "creationTime", fileTime)
            Files.setLastModifiedTime(targetFile, fileTime)
            Files.setAttribute(targetFile, "lastAccessTime", fileTime)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to set file attributes for $targetFile" }
        }
    }
}