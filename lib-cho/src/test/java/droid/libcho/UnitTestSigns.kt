package droid.libcho

import droid.libcho.ApkUtil.magicLowercase
import droid.libcho.ApkUtil.magicUppercase
import droid.libcho.ApkUtil.stringMatchesOffsetsStream
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name

/**
 * If you see "NoClassDefFoundError" at attempts to run tests watch there is
 * two "Run/Debug Configuration", one default, other from Gradle. Default
 * configuration is failing with "NoClassDefFoundError", use Gradle
 * configuration and tests runs fine.
 * Example local unit test, which will execute on the development machine (host).
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class UnitTestSigns {
    private val projectDir: Path = Path.of("src").toAbsolutePath().parent.parent
    private val releaseDir = projectDir.resolve("release")

    @Test
    fun printSignsMagicsApk() {
        Files.walk(releaseDir).forEach { path: Path ->
            if (path == releaseDir) return@forEach
            if (path.isDirectory()) {
                val relative = releaseDir.relativize(path)
                println("Processing: $relative")
            }
            if (path.extension == "apk") {
                val info = ApkInfo(
                    path.toFile(),
                    hashesEnabled = false, signEnabled = true
                )
                if (info.sign!!.signsMagicsUppercaseCount > 1) {
                    println(info.toStringVerbose())
                }
                assertEquals(0, info.sign!!.signsMagicsLowercaseCount)
                assertEquals(true, info.sign!!.signsMagicsUppercaseCount > 0)
            }
        }
    }

    @Test
    fun printSignsMagicsDexJar() {
        Files.walk(releaseDir).forEach { path: Path ->
            if (path == releaseDir) return@forEach
            if (path.isDirectory()) {
                val relative = releaseDir.relativize(path)
                println("Processing: $relative")
            }
            val process: String = if (path.extension == "dex") "DEX"
            else if (path.extension == "class") "CLASS"
            else if (path.absolutePathString().contains("META-INF")) "META"
            else ""
            if (process.isNotEmpty() && !path.isDirectory()) {
                assertEquals(0, stringMatchesOffsetsStream(path.toFile(), magicLowercase).size)
                val signMagicsStream = stringMatchesOffsetsStream(path.toFile(), magicUppercase)
                if (signMagicsStream.isNotEmpty()) {
                    assertEquals(1, signMagicsStream.size)
                    val data = "${path.parent.name}/${path.name} $signMagicsStream"
                    println("\"APK Sig Block 42\" in $process $data")
                }
            }
        }
    }
}