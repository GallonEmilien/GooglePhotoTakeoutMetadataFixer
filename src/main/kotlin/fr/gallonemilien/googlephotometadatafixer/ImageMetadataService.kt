package fr.gallonemilien.googlephotometadatafixer

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.imaging.Imaging
import org.apache.commons.imaging.common.RationalNumber
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

private val logger = KotlinLogging.logger {}

/**
 * Service responsible for handling image metadata (EXIF).
 */
class ImageMetadataService {

    /**
     * Writes GPS coordinates into the EXIF metadata of the target image.
     *
     * @param targetFile The image file to update.
     * @param lat Latitude in degrees.
     * @param lon Longitude in degrees.
     * @param alt Altitude in meters.
     */
    fun writeExifMetadata(targetFile: Path, lat: Double, lon: Double, alt: Double) {
        if (lat == 0.0 && lon == 0.0) return

        val file = targetFile.toFile()
        val tempFile = File(file.path + ".tmp")

        try {
            val outputSet = getOrCreateOutputSet(file)
            val gpsDirectory = outputSet.getOrCreateGPSDirectory()

            outputSet.setGPSInDegrees(lon, lat)

            gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_ALTITUDE)
            gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_ALTITUDE, RationalNumber.valueOf(Math.abs(alt)))

            // Altitude Ref: 0 = Above Sea Level, 1 = Below Sea Level
            gpsDirectory.removeField(GpsTagConstants.GPS_TAG_GPS_ALTITUDE_REF)
            gpsDirectory.add(GpsTagConstants.GPS_TAG_GPS_ALTITUDE_REF, (if (alt >= 0) 0 else 1).toByte())

            FileOutputStream(tempFile).use { fos ->
                BufferedOutputStream(fos).use { os ->
                    ExifRewriter().updateExifMetadataLossless(file, os, outputSet)
                }
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                Files.move(tempFile.toPath(), targetFile, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: Exception) {
            logger.error(e) { "EXIF Error $targetFile" }
            tempFile.delete()
        }
    }

    /**
     * Retrieves the existing TiffOutputSet from the image or creates a new one.
     * This preserves existing EXIF data while allowing modifications.
     */
    private fun getOrCreateOutputSet(file: File): TiffOutputSet {
        return try {
            val metadata = Imaging.getMetadata(file)
            val jpegMetadata = metadata as? JpegImageMetadata
            jpegMetadata?.exif?.outputSet ?: TiffOutputSet()
        } catch (_: Exception) {
            TiffOutputSet()
        }
    }
}