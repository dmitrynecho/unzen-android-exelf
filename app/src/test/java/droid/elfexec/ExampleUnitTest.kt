package droid.elfexec

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
    @Test
    fun addition_isCorrect() {
        assertEquals(4, (2 + 2).toLong())
    }
}