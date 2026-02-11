package fr.gallonemilien.googlephotometadatafixer

import io.github.oshai.kotlinlogging.KotlinLogging
import org.mp4parser.IsoFile
import org.mp4parser.boxes.apple.AppleGPSCoordinatesBox
import org.mp4parser.boxes.iso14496.part12.*
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Locale

private val logger = KotlinLogging.logger {}

/**
 * Service responsible for handling video metadata (MP4/MOV).
 * It handles the injection of GPS coordinates into the ISO User Data Box
 * and corrects file offsets (stco/co64) to prevent file corruption.
 */
class VideoMetadataService {

    /**
     * Writes GPS coordinates into the video metadata.
     *
     * @param targetFile The video file to update.
     * @param lat Latitude in degrees.
     * @param lon Longitude in degrees.
     */
    fun writeVideoMetadata(targetFile: Path, lat: Double, lon: Double) {
        if (lat == 0.0 && lon == 0.0) return

        val tempFile = File(targetFile.toFile().path + ".tmp")
        
        try {
            val isoFile = IsoFile(targetFile.toFile().absolutePath)
            
            try {
                val moov = isoFile.movieBox
                
                // 1. Analyze file structure to determine if offset correction is needed
                val (moovIndex, mdatIndex) = getBoxIndices(isoFile)
                val oldMoovSize = moov.size

                // 2. Inject GPS Data
                injectGpsBox(moov, lat, lon)

                // 3. Calculate size change
                val newMoovSize = moov.size
                val delta = newMoovSize - oldMoovSize

                // 4. Correct Chunk Offsets if 'moov' is before 'mdat' and size changed
                if (moovIndex != -1 && mdatIndex != -1 && moovIndex < mdatIndex && delta != 0L) {
                    logger.debug { "Correcting chunk offsets for ${targetFile.fileName} (Delta: $delta bytes)" }
                    correctChunkOffsets(moov, delta)
                }

                // 5. Write to temporary file
                writeToTempFile(tempFile, isoFile)
                
            } finally {
                // CRITICAL: Close the original file BEFORE attempting to overwrite it
                isoFile.close()
            }

            // 6. Replace original file with temp file
            if (tempFile.exists() && tempFile.length() > 0) {
                Files.move(tempFile.toPath(), targetFile, StandardCopyOption.REPLACE_EXISTING)
            }
            
        } catch (e: Exception) {
            logger.error(e) { "Video Metadata Error $targetFile" }
            // Clean up temp file on error
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    private fun getBoxIndices(isoFile: IsoFile): Pair<Int, Int> {
        var moovIndex = -1
        var mdatIndex = -1
        val boxes = isoFile.boxes
        for (i in boxes.indices) {
            if (boxes[i].type == "moov") moovIndex = i
            if (boxes[i].type == "mdat") mdatIndex = i
        }
        return Pair(moovIndex, mdatIndex)
    }

    private fun injectGpsBox(moov: MovieBox, lat: Double, lon: Double) {
        var udta = moov.getBoxes(UserDataBox::class.java).firstOrNull()
        if (udta == null) {
            udta = UserDataBox()
            moov.addBox(udta)
        }

        // Remove existing GPS box to avoid duplicates
        val existingGps = udta.getBoxes(AppleGPSCoordinatesBox::class.java)
        for (box in existingGps) {
            udta.boxes.remove(box)
        }

        // Format: ISO 6709 (e.g., "+48.8566+002.3522/")
        // FORCE US Locale to ensure dot separator is used
        val latStr = String.format(Locale.US, "%+08.4f", lat)
        val lonStr = String.format(Locale.US, "%+09.4f", lon)
        val locationString = "$latStr$lonStr/"

        val gpsBox = AppleGPSCoordinatesBox()
        gpsBox.value = locationString
        udta.addBox(gpsBox)
    }

    private fun correctChunkOffsets(moov: MovieBox, delta: Long) {
        for (trackBox in moov.getBoxes(TrackBox::class.java)) {
            val stbl = trackBox.mediaBox.mediaInformationBox.sampleTableBox
            
            // Fix 32-bit offsets (stco)
            for (stco in stbl.getBoxes(StaticChunkOffsetBox::class.java)) {
                val chunks = stco.chunkOffsets
                for (i in chunks.indices) {
                    chunks[i] += delta
                }
                stco.chunkOffsets = chunks
            }
            
            // Fix 64-bit offsets (co64)
            for (co64 in stbl.getBoxes(ChunkOffset64BitBox::class.java)) {
                val chunks = co64.chunkOffsets
                for (i in chunks.indices) {
                    chunks[i] += delta
                }
                co64.chunkOffsets = chunks
            }
        }
    }

    private fun writeToTempFile(tempFile: File, isoFile: IsoFile) {
        val raf = RandomAccessFile(tempFile, "rw")
        val out = raf.channel
        
        isoFile.writeContainer(out)
        
        out.close()
        raf.close()
    }
}