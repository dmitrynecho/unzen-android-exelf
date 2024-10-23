package droid.utils

import android.util.Log
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.SortedMap

/**
 * Using Apache Commons Compress instead of system ZIP because system ZIP
 * fails to unpack APK on old Android versions.
 */
object ZipUtils {
    private const val DEBUG = false
    private const val TAG = "ZipUtils"

    @Throws(IOException::class)
    private fun extractEntry(src: ZipArchiveInputStream, dst: File, e: ArchiveEntry) {
        val f = File(dst, e.name)
        if (e.isDirectory) {
            if (!f.mkdirs()) {
                throw IOException("mkdirs() fail " + e.name)
            }
            return
        }
        val parent = f.parentFile ?: throw IOException("getParentFile() fail " + e.name)
        if (!parent.exists()) {
            if (!parent.mkdirs()) {
                throw IOException("parent.mkdirs() fail " + e.name)
            }
        }
        if (DEBUG) {
            Log.i(TAG, "extractEntry: " + f.absolutePath + ", e: " + e.name)
        }
        BufferedOutputStream(FileOutputStream(f)).use { bos ->
            val buf = ByteArray(4096)
            var read: Int
            while ((src.read(buf).also { read = it }) != -1) {
                bos.write(buf, 0, read)
            }
        }
    }

    @Throws(IOException::class)
    private fun nextEntry(zis: ZipArchiveInputStream): ArchiveEntry? {
        val e = zis.nextEntry
        if (e != null && "" == e.name) {
            // Skip special stuff in Android APK.
            if (DEBUG) {
                Log.i(TAG, "Skip special stuff in Android APK")
            }
            return nextEntry(zis)
        }
        return e
    }

    @Throws(IOException::class)
    fun extract(src: File, dst: File) {
        ZipArchiveInputStream(FileInputStream(src)).use { zis ->
            var entry = nextEntry(zis)
            while (entry != null) {
                extractEntry(zis, dst, entry)
                entry = nextEntry(zis)
            }
        }
    }

    fun entriesSha1s(f: File): SortedMap<String, String> {
        val map = sortedMapOf<String, String>()
        ZipArchiveInputStream(FileInputStream(f)).use { zis ->
            var entry = nextEntry(zis)
            while (entry != null) {
                // println("ZIP entry: ${entry.name}")
                if (entry.isDirectory) {
                    map[entry.name] = FileUtils.ZERO_SHA1
                } else {
                    map[entry.name] = FileUtils.sha1(zis)
                }
                entry = nextEntry(zis)
            }
        }
        return map
    }
}