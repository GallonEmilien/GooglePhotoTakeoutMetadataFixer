package fr.gallonemilien.googlephotometadatafixer

import java.nio.file.Path
import kotlin.io.path.extension

val photoExtensions = setOf("jpg", "jpeg", "png", "heic", "heif", "webp", "gif", "bmp", "tiff")
val videoExtensions = setOf("mp4", "mov", "avi", "wmv", "mkv", "3gp", "m4v")

val Path.isPhoto: Boolean
    get() = extension.lowercase() in photoExtensions

val Path.isVideo: Boolean
    get() = extension.lowercase() in videoExtensions