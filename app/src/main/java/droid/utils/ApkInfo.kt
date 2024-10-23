package droid.utils

import java.io.File
import java.text.NumberFormat
import java.util.Locale

class ApkInfo(val apk: File, val hashesEnabled: Boolean, val signEnabled: Boolean) {
    val fileName: String = apk.name
    val fileSize = apk.length()
    val hashes = if (hashesEnabled) ApkInfoHashes(apk) else null
    val sign = if (signEnabled) ApkInfoSign(apk) else null
    val nf = NumberFormat.getInstance(Locale.US)

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(fileName)
            .append(" ")
            .append(nf.format(fileSize)).append(" B")
        return sb.toString()
    }

    fun toStringVerbose(): String {
        val sb = StringBuilder()
        sb.append(fileName)
            .append(" ")
            .append(nf.format(fileSize)).append(" B")
        if (sign != null) {
            sb.appendLine().append(sign)
        }
        if (hashes != null) {
            sb.appendLine().append("APK: ").append(hashes.sha1)
                .appendLine().append("CON: ").append(hashes.entriesSha1)
        }
        return sb.toString()
    }
}