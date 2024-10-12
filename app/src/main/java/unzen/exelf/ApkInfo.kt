package unzen.exelf

import java.io.File

class ApkInfo(val file: File) {
    val fileName: String = file.name
    val fileSize = file.length()
    val signsMagicsCount = signsMagicsCount()
    val sha1 = FileUtils.sha1(file)

    private fun signsMagicsCount(): Int {
        // TODO. Too paranoid, remove later.
        if (FileUtils.countStringMatches(file, "APK Sig block 42") != 0) {
            throw IllegalStateException(file.absolutePath)
        }
        // Search for string "APK Sig Block 42" in APK file.
        return FileUtils.countStringMatches(file, "APK Sig Block 42")
    }
}