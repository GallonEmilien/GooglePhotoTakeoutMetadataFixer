package fr.gallonemilien.googlephotometadatafixer

import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

class TakeoutApp : Application() {
    private val vm = MainViewModel()

    override fun start(stage: Stage) {
        val root = VBox(20.0).apply {
            padding = Insets(25.0)
            prefWidth = 500.0
            style = "-fx-font-family: 'Segoe UI', sans-serif;"
        }

        // Header
        val headerLabel = Label("Google Photos Metadata Fixer").apply {
            style = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;"
        }

        // Input Section
        val inputSection = createPathSection(stage, "Source Directory:", vm.inputPath)
        
        // Output Section
        val outputSection = createPathSection(stage, "Destination Directory:", vm.outputPath)

        // Stats Section
        val statsGrid = GridPane().apply {
            hgap = 20.0
            vgap = 10.0
            padding = Insets(10.0, 0.0, 10.0, 0.0)
            
            add(Label("Processed Files:").apply { style = "-fx-font-weight: bold;" }, 0, 0)
            add(Label().apply { 
                textProperty().bind(Bindings.concat(vm.processedFilesCount, " / ", vm.totalFilesCount)) 
            }, 1, 0)
            
            add(Label("Data Size:").apply { style = "-fx-font-weight: bold;" }, 0, 1)
            add(Label().apply { 
                textProperty().bind(Bindings.createStringBinding({
                    "${formatSize(vm.processedSize.get())} / ${formatSize(vm.totalSize.get())}"
                }, vm.processedSize, vm.totalSize))
            }, 1, 1)
            
            add(Label("Estimated Time:").apply { style = "-fx-font-weight: bold;" }, 0, 2)
            add(Label().apply { textProperty().bind(vm.estimatedTimeRemaining) }, 1, 2)
        }

        // Progress Section
        val progressBar = ProgressBar().apply {
            progressProperty().bind(vm.progress)
            prefWidth = 450.0
            prefHeight = 20.0
        }
        
        val progressLabel = Label().apply {
            textProperty().bind(Bindings.format("%.0f%%", vm.progress.multiply(100)))
            style = "-fx-font-size: 12px;"
        }
        
        val progressBox = VBox(5.0, progressBar, progressLabel).apply {
            alignment = Pos.CENTER
        }

        // Action Button
        val btnStart = Button("Start Processing").apply {
            style = "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;"
            prefWidth = 200.0
            
            disableProperty().bind(
                vm.isProcessing
                    .or(vm.inputPath.isEmpty)
                    .or(vm.outputPath.isEmpty)
            )
            setOnAction { vm.startProcessing() }
        }
        
        val btnBox = HBox(btnStart).apply {
            alignment = Pos.CENTER
            padding = Insets(10.0, 0.0, 0.0, 0.0)
        }

        root.children.addAll(
            headerLabel, 
            Separator(),
            inputSection, 
            outputSection, 
            Separator(), 
            statsGrid,
            progressBox,
            btnBox
        )

        stage.scene = Scene(root)
        stage.title = "Gallon Emilien - Google Photos Metadata Fixer"
        stage.isResizable = false
        stage.show()
    }

    private fun createPathSection(stage: Stage, title: String, property: javafx.beans.property.StringProperty): VBox {
        val label = Label(title).apply { style = "-fx-font-weight: bold;" }
        
        val pathField = TextField().apply {
            textProperty().bindBidirectional(property)
            isEditable = false
            prefWidth = 350.0
        }
        
        val btnBrowse = Button("Browse...").apply {
            setOnAction {
                val dir = DirectoryChooser().showDialog(stage)
                dir?.let { property.set(it.absolutePath) }
            }
        }
        
        val hBox = HBox(10.0, pathField, btnBrowse).apply {
            alignment = Pos.CENTER_LEFT
        }
        
        return VBox(5.0, label, hBox)
    }
    
    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(bytes / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }
}