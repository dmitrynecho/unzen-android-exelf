package unzen.exelf

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Path

/**
 * If you see "NoClassDefFoundError" at attempts to run tests watch there is
 * two "Run/Debug Configuration", one default, other from Gradle. Default
 * configuration is failing with "NoClassDefFoundError", use Gradle
 * configuration and tests runs fine.
 * Example local unit test, which will execute on the development machine (host).
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {
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
    }
    @Test
    fun sha1() {
        val file1 = Path.of("src/test/res/sha1-normal.txt").toFile()
        assertEquals("5a87b8667aa3809252a74f6ba7cc698ed2e87ddf", FileUtils.sha1(file1))
        val file2 = Path.of("src/test/res/sha1-empty.txt").toFile()
        assertEquals(FileUtils.ZERO_SHA1, FileUtils.sha1(file2))
    }
}