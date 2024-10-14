package unzen.exelf

import java.io.File

class ApkInfo(val file: File) {
    val fileName: String = file.name
    val fileSize = file.length()
    val signsMagicsUppercaseCount = FileUtils.countStringMatches(file, "APK Sig Block 42")
    val signsMagicsLowercaseCount = FileUtils.countStringMatches(file, "APK Sig block 42")
    val sha1 = FileUtils.sha1(file)
    val entriesSha1sMap = ZipUtils.entriesSha1s(file)
    val entriesSha1 = FileUtils.sha1(entriesSha1sMap.toString())

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(fileName)
            .append(" ")
            .append(fileSize).append(" B")
        return sb.toString()
    }

    fun toStringVerbose(): String {
        val sb = StringBuilder()
        sb.append(fileName)
            .append(" ")
            .append(fileSize).append(" B")
            .append(" L")
            .append(signsMagicsLowercaseCount)
            .append(" U")
            .append(signsMagicsUppercaseCount)
            .appendLine().append("APK: ").append(sha1)
            .appendLine().append("APK: ").append(entriesSha1)
        return sb.toString()
    }
}