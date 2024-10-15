package droid.elfexec

import java.io.File

class ApkInfoSign(val apk: File) {
    val signsMagicsUppercaseCount = FileUtils.countStringMatches(apk, "APK Sig Block 42")
    val signsMagicsLowercaseCount = FileUtils.countStringMatches(apk, "APK Sig block 42")
}