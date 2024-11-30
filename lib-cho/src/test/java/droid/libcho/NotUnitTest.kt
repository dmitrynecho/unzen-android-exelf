package droid.libcho

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
class NotUnitTest {
    private val projectDir: Path = Path.of("src").toAbsolutePath().parent.parent
    private val releaseDir = projectDir.resolve("release")
    private val tempDir = projectDir.resolve("temp")
    private val resDir: Path = Path.of("src/test/res")
    @Test
    fun apks1() {
        val aabs1Dir = tempDir.resolve("21.09.08+2030+GPS")
        val aabs1 = mutableMapOf<Path, String>()
        Files.walk(aabs1Dir).forEach { path: Path ->
            if (path == aabs1Dir) return@forEach
            if (path.extension ==  "aab") {
                aabs1[path] = HashUtils.sha1(path.toFile())
            }
        }
        val aabs2Dir = tempDir.resolve("bundle")
        val aabs2 = mutableMapOf<Path, String>()
        Files.walk(aabs2Dir).forEach { path: Path ->
            if (path == aabs2Dir) return@forEach
            if (path.extension ==  "aab") {
                aabs2[path] = HashUtils.sha1(path.toFile())
            }
        }
        println("${aabs1.size} AAA ${aabs2.size}")
        assertEquals(true, aabs2.values.containsAll(aabs1.values))
    }
}