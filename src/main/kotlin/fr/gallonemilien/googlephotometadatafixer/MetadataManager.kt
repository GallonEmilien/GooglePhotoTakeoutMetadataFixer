package fr.gallonemilien.googlephotometadatafixer

import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

/**
 * Facade service that coordinates metadata operations.
 * It delegates specific tasks to specialized services (Image, Video, File Attributes).
 */
class MetadataManager {
    private val fileAttributeService = FileAttributeService()
    private val imageMetadataService = ImageMetadataService()
    private val videoMetadataService = VideoMetadataService()
    private val jsonSidecarService = JsonSidecarService()

    /**
     * Applies metadata from a JSON sidecar file to the target media file.
     *
     * @param jsonPath The path to the source JSON file.
     * @param targetFile The path to the target media file.
     */
    fun applyMetadata(jsonPath: Path, targetFile: Path) {
        val root = jsonSidecarService.parseJson(jsonPath) ?: return

        val timestamp = root.path("photoTakenTime").path("timestamp").asLong(0)
        val geoData = root.path("geoData")
        val lat = geoData.path("latitude").asDouble(0.0)
        val lon = geoData.path("longitude").asDouble(0.0)
        val alt = geoData.path("altitude").asDouble(0.0)

        if (lat != 0.0 || lon != 0.0) {
            try {
                when {
                    targetFile.isPhoto -> {
                        logger.debug { "Applying EXIF to photo: ${targetFile.fileName}" }
                        imageMetadataService.writeExifMetadata(targetFile, lat, lon, alt)
                    }
                    targetFile.isVideo -> {
                        logger.debug { "Applying metadata to video: ${targetFile.fileName}" }
                        videoMetadataService.writeVideoMetadata(targetFile, lat, lon)
                    }
                }
            } catch (e: Exception) {
                logger.warn(e) { "Failed to apply metadata to ${targetFile.fileName}" }
            }
        }

        if (timestamp > 0) {
            fileAttributeService.applyTimestamps(targetFile, timestamp)
        }
    }
}