package droid.libcho

import java.io.File

class ApkInfoSign(val apk: File) {

    val signsMagicsUppercaseCount = FileUtils.countStringMatches(apk, ApkUtil.magicUppercase)
    val signsMagicsLowercaseCount = FileUtils.countStringMatches(apk, ApkUtil.magicLowercase)

    init {
        require(signsMagicsLowercaseCount == 0)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("S").append(signsMagicsUppercaseCount)
        /*
        RandomAccessFile(apk, "r").use {
            val signBlock = ApkUtil.findApkSigningBlock(it.channel)
            println("A ${signBlock.second}")
        }
        */
        return sb.toString()
    }
}