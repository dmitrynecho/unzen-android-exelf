package unzen.exelf

import android.os.Build
import android.system.Os
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.Objects

/**
 * Some methods copied from Apache Commons IO.
 */
object FileUtils {

    fun countStringMatches(f: File, s: String): Int {
        var count = 0
        FileInputStream(f).use { stream ->
            val reader = BufferedReader(InputStreamReader(stream))
            val searchTerm = s.toCharArray()
            val firstChar = searchTerm[0]
            val searchTermBody = searchTerm.copyOfRange(1, searchTerm.size)
            //println("Body: ${String(searchTermBody)}")
            val bufSize = searchTermBody.size
            val buf = CharArray(bufSize)
            var c: Int
            while ((reader.read().also { c = it }) != -1) {
                if (c == firstChar.code) {
                    reader.mark(bufSize)
                    val readed = reader.read(buf)
                    if (readed == -1) {
                        break
                    } else if (readed == bufSize) {
                        if (searchTermBody.contentEquals(buf)) {
                            //println("Found ${c.toChar()}${String(buf)}")
                            count += 1
                        }
                    }
                    reader.reset()
                }
            }
        }
        return count
    }

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

    @Throws(Exception::class)
    fun symlink(target: String, link: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Files.createSymbolicLink(Paths.get(link), Paths.get(target))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Os.symlink(target, link)
        } else {
            Runtime.getRuntime().exec(arrayOf("ln", "-s", target, link)).waitFor()
        }
    }

    @Throws(IOException::class)
    fun readSymlink(symlink: File): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val target = Files.readSymbolicLink(symlink.toPath()).toString()
            if (symlink.exists()) {
                // Since getCanonicalPath() and getCanonicalFile() works only for non broken
                // links, this check has sense only if link is not broken. Broken symlinks
                // returns false from exists().
                val compatTarget = symlink.canonicalPath
                if (target != compatTarget) {
                    throw IOException(
                        String.format(
                            "!target.equals(compatTarget) [%s] -> [%s][%s]",
                            symlink, target, compatTarget
                        )
                    )
                }
            }
            return target
        }
        val target = symlink.canonicalPath
        if (target == symlink.absolutePath) {
            throw IOException(String.format("Not a symlink: %s", symlink))
        }
        return target
    }

    fun existsFollowLinks(file: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Files.exists(file.toPath())
        }
        return file.exists()
    }

    fun existsNoFollowLinks(file: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS)
        }
        val res = file.exists()
        if (res) {
            return true
        }
        return fileListedInDir(Objects.requireNonNull(file.parentFile), file)
    }

    fun fileListedInDir(dir: File, file: File): Boolean {
        for (fileInDir in Objects.requireNonNull(dir.listFiles())) {
            if (fileInDir.absolutePath == file.absolutePath) {
                return true
            }
        }
        return false
    }

    /**
     * Determines whether the specified file is a Symbolic Link rather than an actual file.
     *
     *
     * Will not return true if there is a Symbolic Link anywhere in the path,
     * only if the specific file is.
     * @param file the file to check
     * @return true if the file is a Symbolic Link
     * @throws IOException if an IO error occurs while checking the file
     */
    @Throws(IOException::class)
    fun isSymlink(file: File?): Boolean {
        if (file == null) {
            throw NullPointerException("File must not be null")
        }
        // We dont use here simple getCanonicalPath() equals getAbsolutePath() because they
        // might be not equals when there is symlink in upper path, but the file that
        // we currently checking is not a symlink.
        val fileInCanonicalDir: File
        if (file.parent == null) {
            fileInCanonicalDir = file
        } else {
            val canonicalDir = Objects.requireNonNull(file.parentFile).canonicalFile
            fileInCanonicalDir = File(canonicalDir, file.name)
        }
        val res: Boolean
        if (fileInCanonicalDir.canonicalFile == fileInCanonicalDir.absoluteFile) {
            // If file exists then if it is a symlink it's not broken.
            if (file.exists()) {
                res = false
            } else {
                // Broken symlink will show up in the list of files of its parent directory.
                val canon = file.canonicalFile
                val parentDir = canon.parentFile
                if (parentDir == null || !parentDir.exists()) {
                    res = false
                } else {
                    val fileInDir = parentDir.listFiles { aFile: File -> aFile == canon }
                    res = fileInDir != null && fileInDir.isNotEmpty()
                }
            }
        } else {
            res = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val resNormal = Files.isSymbolicLink(file.toPath())
            if (res != resNormal) {
                throw IOException(String.format("%b, %b, %s", resNormal, res, file))
            }
        }
        return res
    }

    /**
     * Lists files in a directory, asserting that the supplied directory satisfies exists and is a directory
     * @param directory The directory to list
     * @return The files in the directory, never null.
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class)
    private fun verifiedListFiles(directory: File): Array<File> {
        if (!directory.exists()) {
            val message = "$directory does not exist"
            throw IllegalArgumentException(message)
        }
        if (!directory.isDirectory) {
            val message = "$directory is not a directory"
            throw IllegalArgumentException(message)
        }
        val files = directory.listFiles()
            ?: // null if security restricted
            throw IOException("Failed to list contents of $directory")
        return files
    }

    /**
     * Deletes a file. If file is a directory, delete it and all sub-directories.
     *
     *
     * The difference between File.delete() and this method are:
     *
     *  * A directory to be deleted does not have to be empty.
     *  * You get exceptions when a file or directory cannot be deleted.
     * (java.io.File methods returns a boolean)
     *
     *
     * @param file file or directory to delete, must not be `null`
     * @throws NullPointerException  if the directory is `null`
     * @throws FileNotFoundException if the file was not found
     * @throws IOException           in case deletion is unsuccessful
     */
    @Throws(IOException::class)
    fun forceDelete(file: File) {
        if (file.isDirectory) {
            deleteDirectory(file)
        } else {
            val filePresent = file.exists()
            if (!file.delete()) {
                if (!filePresent) {
                    throw FileNotFoundException("File does not exist: $file")
                }
                val message = "Unable to delete file: $file"
                throw IOException(message)
            }
        }
    }

    /**
     * Cleans a directory without deleting it.
     *
     * @param directory directory to clean
     * @throws IOException              in case cleaning is unsuccessful
     * @throws IllegalArgumentException if `directory` does not exist or is not a directory
     */
    @Throws(IOException::class)
    fun cleanDirectory(directory: File) {
        val files = verifiedListFiles(directory)
        var exception: IOException? = null
        for (file in files) {
            try {
                forceDelete(file)
            } catch (ioe: IOException) {
                exception = ioe
            }
        }
        if (null != exception) {
            throw exception
        }
    }

    /**
     * Deletes a directory recursively.
     *
     * @param directory directory to delete
     * @throws IOException              in case deletion is unsuccessful
     * @throws IllegalArgumentException if `directory` does not exist or is not a directory
     */
    @Throws(IOException::class)
    fun deleteDirectory(directory: File) {
        if (!directory.exists()) {
            return
        }
        if (!isSymlink(directory)) {
            cleanDirectory(directory)
        }
        if (!directory.delete()) {
            val message = "Unable to delete directory $directory."
            throw IOException(message)
        }
    }

}
