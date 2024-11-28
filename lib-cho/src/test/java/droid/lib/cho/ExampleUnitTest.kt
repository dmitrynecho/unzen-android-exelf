package droid.lib.cho

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString
import kotlin.io.path.readText

/**
 * If you see "NoClassDefFoundError" at attempts to run tests watch there is
 * two "Run/Debug Configuration", one default, other from Gradle. Default
 * configuration is failing with "NoClassDefFoundError", use Gradle
 * configuration and tests runs fine.
 * Example local unit test, which will execute on the development machine (host).
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {
    private val resDir: Path = Path.of("src/test/res")
    @Test
    fun searchInFile() {
        val file1 = Path.of("src/test/res/search-in-file.txt").toFile()
        assertEquals(2, FileUtils.countStringMatches(file1, "TEST TEST TEST"))
        val file2 = Path.of("src/test/res/search-in-file.txt").toFile()
        assertEquals(2, FileUtils.countStringMatches(file2, "TEST TEST TEST"))
        assertEquals(0, FileUtils.countStringMatches(file2, "TEST TeST TEST"))
    }
    @Test
    fun sha1() {
        val file1 = Path.of("src/test/res/sha1-normal.txt").toFile()
        assertEquals("5a87b8667aa3809252a74f6ba7cc698ed2e87ddf", FileUtils.sha1(file1))
        val file2 = Path.of("src/test/res/sha1-empty.txt").toFile()
        assertEquals(FileUtils.ZERO_SHA1, FileUtils.sha1(file2))
    }
    @Test
    fun zip() {
        val zipFile = Path.of("src/test/res/zip/zip.zip").toFile()
        val zipFileMap = ZipUtils.entriesSha1s(zipFile)
        val zipDirPath = Path.of("src/test/res/zip/zip")
        val zipDirMap = sortedMapOf<String, String>()
        Files.walk(zipDirPath).forEach { path: Path ->
            val name = zipDirPath.parent.relativize(path).pathString
            if (path.isDirectory()) {
                zipDirMap["$name/"] = FileUtils.ZERO_SHA1
            } else {
                zipDirMap[name] = FileUtils.sha1(path.toFile())
            }
        }
        val expected = resDir.resolve("zip/expected.txt").readText()
        assertEquals(expected, zipFileMap.toString())
        assertEquals(expected, zipDirMap.toString())
        assertEquals(
            FileUtils.sha1(zipFileMap.toString()),
            FileUtils.sha1(zipDirMap.toString()))
    }
    @Test
    fun apks() {
        val projectDir = Path.of("src").toAbsolutePath().parent.parent
        val releaseDir = projectDir.resolve("release")
        val tempDir = projectDir.resolve("temp")
        Files.walk(releaseDir).forEach { path: Path ->
            if (path == releaseDir) return@forEach
            if (path.extension ==  "apks") {
                val dst = tempDir.resolve(path.nameWithoutExtension)
                ZipUtils.extract(path.toFile(), dst.toFile())
            }
        }
        Files.walk(tempDir).forEach { path: Path ->
            if (path == tempDir) return@forEach
            if (path.extension ==  "apk") {
                val info = ApkInfo(
                    path.toFile(),
                    hashesEnabled = false, signEnabled = true
                )
                println(info.toStringVerbose())
                assertEquals(0, info.sign!!.signsMagicsLowercaseCount)
                assertEquals(1, info.sign!!.signsMagicsUppercaseCount)
            }
        }
    }
    @Test
    fun magicEncode() {
        assertEquals("APK Sig Block 42", ApkUtil.magicUppercase)
        assertEquals("APK Sig block 42", ApkUtil.magicLowercase)
        "APK Sig Block 42".toCharArray().forEachIndexed { i, c ->
            if (i != 0) print(", ")
            //print(c.code)
        }
        println()
        "APK Sig block 42".toCharArray().forEachIndexed { i, c ->
            if (i != 0) print(", ")
            //print(c.code)
        }
        println()
    }
}