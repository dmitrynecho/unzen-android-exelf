package unzen.exelf

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.util.TreeMap
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name
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
    val resDir = Path.of("src/test/res")
    @Test
    fun addition_isCorrect() {
        assertEquals(4, (2 + 2).toLong())
    }
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
        assertEquals(FileUtils.sha1(zipFileMap.toString()),
            FileUtils.sha1(zipDirMap.toString()))
    }
    @Test
    fun apks() {
        val projectDir = Path.of("src").toAbsolutePath().parent.parent
        val tempDir = projectDir.resolve("temp")
        Files.walk(tempDir).forEach { path: Path ->
            if (path == tempDir) return@forEach
            if (path.extension ==  "apk") {
                val info = ApkInfo(path.toFile())
                println(info.toStringVerbose())
            }
        }
    }
}