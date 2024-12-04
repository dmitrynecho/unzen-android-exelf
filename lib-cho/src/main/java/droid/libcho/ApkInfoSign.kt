package droid.libcho

import droid.libcho.ApkUtil.stringMatchesOffsetsStream
import java.io.File
import java.io.RandomAccessFile
import java.text.NumberFormat
import java.util.Locale

class ApkInfoSign(val apk: File) {

    val nf = NumberFormat.getInstance(Locale.US)
    val signsMagicsUppercaseCount = FileUtils.countStringMatches(apk, ApkUtil.magicUppercase)
    val signsMagicsLowercaseCount = FileUtils.countStringMatches(apk, ApkUtil.magicLowercase)

    init {
        require(signsMagicsLowercaseCount == 0)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("S").append(signsMagicsUppercaseCount)
        RandomAccessFile(apk, "r").use {
            val signMagicsStream = stringMatchesOffsetsStream(apk, ApkUtil.magicUppercase)
            val signBlock = ApkUtil.findApkSigningBlock(it.channel)
            sb.appendLine().append(nf.format(signBlock.second))
            signMagicsStream.forEach { offset ->
                sb.append(" [").append(nf.format(offset))
                    .append(" ").append(nf.format(signBlock.second!! - offset))
                    .append("]")
            }
        }
        return sb.toString()
    }
}