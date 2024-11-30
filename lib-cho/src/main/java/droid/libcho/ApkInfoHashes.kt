package droid.libcho

import java.io.File

class ApkInfoHashes(val apk: File) {
    val sha1 = HashUtils.sha1(apk)
    val entriesSha1sMap = ZipUtils.entriesSha1s(apk)
    val entriesSha1 = HashUtils.sha1(entriesSha1sMap.toString())
}