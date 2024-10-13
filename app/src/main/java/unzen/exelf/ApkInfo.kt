package unzen.exelf

import java.io.File

class ApkInfo(val file: File) {
    val fileName: String = file.name
    val fileSize = file.length()
    // Search for string "APK Sig Block 42" in APK file.
    val signsMagicsUppercaseCount = FileUtils.countStringMatches(file, "APK Sig Block 42")
    val signsMagicsLowercaseCount = FileUtils.countStringMatches(file, "APK Sig block 42")
    val sha1 = FileUtils.sha1(file)
}