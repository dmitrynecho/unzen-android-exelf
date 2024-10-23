package droid.utils

import java.io.File
import java.io.RandomAccessFile

class ApkInfoSign(val apk: File) {
    val signsMagicsUppercaseCount = FileUtils.countStringMatches(apk, "APK Sig Block 42")
    val signsMagicsLowercaseCount = FileUtils.countStringMatches(apk, "APK Sig block 42")

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("L")
            .append(signsMagicsLowercaseCount)
            .append(" U")
            .append(signsMagicsUppercaseCount)
        RandomAccessFile(apk, "r").use {
            val signBlock = ApkUtil.findApkSigningBlock(it.channel)
            println("A ${signBlock.second}")
        }
        return sb.toString()
    }
}