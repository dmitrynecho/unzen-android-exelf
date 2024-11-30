package droid.libcho

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

object HashUtils {
    const val ZERO_SHA1: String = "da39a3ee5e6b4b0d3255bfef95601890afd80709"

    private val sDigestSha1 = MessageDigest.getInstance("SHA-1")

    fun sha1(f: File): String {
        FileInputStream(f).use { stream ->
            return sha1(stream)
        }
    }

    fun sha1(`in`: InputStream): String {
        val buf = ByteArray(65536)
        var len: Int
        while (true) {
            len = `in`.read(buf)
            if (len <= 0) {
                break
            }
            sDigestSha1.update(buf, 0, len)
        }
        return Utils.bytesToHex(sDigestSha1.digest())
    }

    fun sha1(text: String): String {
        val buf = text.toByteArray(charset("UTF-8"))
        sDigestSha1.update(buf, 0, buf.size)
        return Utils.bytesToHex(sDigestSha1.digest())
    }
}