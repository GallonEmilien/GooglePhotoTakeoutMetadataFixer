package fr.gallonemilien.googlephotometadatafixer

import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.Toolkit
import java.io.File
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

/**
 * ViewModel for the main application window.
 * It manages the UI state and orchestrates the background processing.
 * It ensures that all UI updates are performed on the JavaFX Application Thread.
 */
class MainViewModel {
    // UI Properties (Bound to View)
    val inputPath = SimpleStringProperty("")
    val outputPath = SimpleStringProperty("")
    val progress = SimpleDoubleProperty(0.0)
    val isProcessing = SimpleBooleanProperty(false)
    
    // Stats Properties
    val processedFilesCount = SimpleLongProperty(0)
    val totalFilesCount = SimpleLongProperty(0)
    val processedSize = SimpleLongProperty(0)
    val totalSize = SimpleLongProperty(0)
    val estimatedTimeRemaining = SimpleStringProperty("")

    private val processor = TakeoutProcessor()
    // Scope tied to JavaFX thread for UI updates
    private val scope = CoroutineScope(Dispatchers.JavaFx)

    /**
     * Initiates the file processing workflow.
     * Validates inputs and launches a coroutine on the IO dispatcher.
     */
    fun startProcessing() {
        if (inputPath.get().isEmpty() || outputPath.get().isEmpty()) return

        isProcessing.set(true)
        progress.set(0.0)
        processedFilesCount.set(0)
        totalFilesCount.set(0)
        processedSize.set(0)
        totalSize.set(0)
        estimatedTimeRemaining.set("Calculating...")

        scope.launch {
            try {
                val input = Paths.get(inputPath.get())
                val output = Paths.get(outputPath.get())
                val startTime = System.currentTimeMillis()
                
                // Run heavy processing on IO thread
                processor.process(input, output) { current, total, pSize, tSize ->
                    
                    // Switch back to JavaFX thread for UI updates
                    withContext(Dispatchers.JavaFx) {
                        progress.set(current.toDouble() / total.toDouble())
                        processedFilesCount.set(current.toLong())
                        totalFilesCount.set(total.toLong())
                        processedSize.set(pSize)
                        totalSize.set(tSize)
                        
                        updateEstimatedTime(startTime, current, total)
                    }
                }

                Toolkit.getDefaultToolkit().beep()

                openOutputDirectory(output.toFile())
                
            } catch (e: Exception) {
                logger.error(e) { "Processing failed" }
            } finally {
                // Ensure final state is set on JavaFX thread
                withContext(Dispatchers.JavaFx) {
                    isProcessing.set(false)
                    progress.set(1.0)
                    estimatedTimeRemaining.set("Finished")
                }
            }
        }
    }
    
    /**
     * Opens the output directory in the system's file explorer.
     */
    private suspend fun openOutputDirectory(directory: File) = withContext(Dispatchers.IO) {
        try {
            if (Desktop.isDesktopSupported() && directory.exists()) {
                Desktop.getDesktop().open(directory)
                logger.info { "Opened output directory: ${directory.absolutePath}" }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to open output directory" }
        }
    }
    
    /**
     * Calculates and updates the estimated time remaining.
     */
    private fun updateEstimatedTime(startTime: Long, current: Int, total: Int) {
        val elapsedTime = System.currentTimeMillis() - startTime
        if (current > 0 && elapsedTime > 0) {
            val timePerFile = elapsedTime.toDouble() / current
            val remainingFiles = total - current
            val remainingMillis = (remainingFiles * timePerFile).toLong()
            estimatedTimeRemaining.set(formatDuration(remainingMillis))
        }
    }
    
    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return if (hours > 0) {
            String.format("%02dh %02dm", hours, minutes % 60)
        } else {
            String.format("%02dm %02ds", minutes, seconds % 60)
        }
    }
}