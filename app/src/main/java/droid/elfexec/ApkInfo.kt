package droid.elfexec

import java.io.File

class ApkInfo(val apk: File, val hashesEnabled: Boolean, val signEnabled: Boolean) {
    val fileName: String = apk.name
    val fileSize = apk.length()
    val hashes = if (hashesEnabled) ApkInfoHashes(apk) else null
    val sign = if (signEnabled) ApkInfoSign(apk) else null

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
        if (sign != null) {
            sb.append(" L")
                .append(sign.signsMagicsLowercaseCount)
                .append(" U")
                .append(sign.signsMagicsUppercaseCount)
        }
        if (hashes != null) {
            sb.appendLine().append("APK: ").append(hashes.sha1)
                .appendLine().append("APK: ").append(hashes.entriesSha1)
        }
        return sb.toString()
    }
}