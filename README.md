# Google Photo Takeout Metadata Fixer

This software corrects the metadata (capture dates, EXIF) of your photos and videos exported from Google Takeout. It reconciles media files with their associated `.json` files to restore the original dates.

## User Guide

### Download & Launch

#### Option 1: Portable Version (Windows)
This version does not require Java to be installed on your machine.

1. **Download** the provided ZIP archive (containing the application folder).
2. **Extract** the archive completely to a location of your choice.
3. Open the extracted folder.
4. Launch the **`GooglePhotoMetadataFixer.exe`** file.

> **Important:** Do not move the `.exe` file out of its folder. It needs the `runtime` and `app` folders located next to it to function. If you want a desktop shortcut, right-click on the exe > Send to > Desktop (create shortcut).

---

#### Option 2: JAR Version (Universal - Linux, Mac, Windows)
This method requires **Java 21** (or higher) to be installed.

1. Make sure you have [Java 21 (JDK)](https://adoptium.net/) installed.
2. Download the `GooglePhotoTakeoutMetadataFixer-X.X-all.jar` file.
3. Open a terminal (or command prompt) in the file's folder.
4. Run the following command:
```bash
java -jar GooglePhotoTakeoutMetadataFixer-1.0-all.jar
```

---

### Usage

1. Unzip all your "takeout-xxxxxx.zip" files
2. launch the app
3. select the input folder (where you unziped your files (it's recursive))
4. select an output folder
5. Start the process !

## Developer Guide

### Prerequisites
* JDK 21
* Git

### Run the application (Development Mode)
To test the application directly from the source code without compiling a JAR:
```bash
./gradlew run
```

### Build the project

#### 1. Create a Fat JAR (Executable Java)
Generates a single `.jar` file containing all dependencies.
* **Command:** `./gradlew shadowJar`
* **Result:** `build/libs/GooglePhotoTakeoutMetadataFixer-1.0-all.jar`

#### 2. Create the native executable (Portable)
Generates a folder containing the `.exe` executable and its own Java runtime (via jpackage).
* **Command:** `./gradlew jpackage`
* **Result:** `build/jpackage/GooglePhotoMetadataFixer/`
