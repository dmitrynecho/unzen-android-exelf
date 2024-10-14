package droid.elfexec

import android.os.Build
import android.system.Os
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import droid.elfexec.FileUtils.fileListedInDir
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Objects

/**
 * Looks like creating hard links without root was disabled since Android M.
 * See: https://seandroid-list.tycho.nsa.narkive.com/r5ZNxgkh/selinux-hardlink-brain-damage-in-android-m
 * See: https://code.google.com/archive/p/android-developer-preview/issues/3150
 */
@RunWith(AndroidJUnit4::class)
class FileLinkTest {
    @Throws(IOException::class, InterruptedException::class)
    private fun link(oldPath: String, newPath: String): Boolean {
        val link = File(newPath)
        Runtime.getRuntime().exec(arrayOf("ln", oldPath, newPath)).waitFor()
        if (link.exists()) {
            return true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Os.link(oldPath, newPath)
                Assert.assertTrue(link.exists())
                return true
            } catch (e: Exception) {
                // Exception instead of ErrnoException to awoid crashes on old platforms
                e.printStackTrace()
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Files.createLink(Paths.get(newPath), Paths.get(oldPath))
                Assert.assertTrue(link.exists())
                return true
            } catch (e: Exception) {
                // Exception instead of AccessDeniedException to awoid crashes on old platforms
                e.printStackTrace()
            }
        }
        Assert.assertFalse(fileListedInDir(Objects.requireNonNull(link.parentFile), link))
        return false
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun test() {
        // Context of the app under test.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = context.filesDir
        val link = File(dir, "link")
        Assert.assertEquals(dir.absolutePath, link.parent)
        Assert.assertTrue(!link.exists() || link.delete())
        Assert.assertFalse(link.exists())
        Assert.assertFalse(fileListedInDir(dir, link))
        val normalFile = File(dir, "temp")
        Assert.assertTrue(!normalFile.exists() || normalFile.delete())
        Assert.assertTrue(normalFile.createNewFile())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // This test branch succeed on emulator with API 29 with and without Google Play
            // This test branch succeed on Asus Nexus 7 3G with custom Android 7.1.2
            // This test branch succeed on Xiaomi Redmi Note 8 Pro with MIUI Android 9
            Assert.assertFalse(link(normalFile.absolutePath, link.absolutePath))
            Assert.assertFalse(link.exists())
            Assert.assertFalse(fileListedInDir(dir, link))
            Assert.assertFalse(link.delete())
        } else {
            // This test branch succeed on emulator with API 17 and API 16
            Assert.assertTrue(link(normalFile.absolutePath, link.absolutePath))
            Assert.assertTrue(link.exists())
            Assert.assertTrue(fileListedInDir(dir, link))
            Assert.assertTrue(link.delete())
            Assert.assertFalse(fileListedInDir(dir, link))
            Assert.assertFalse(link.exists())
            Assert.assertEquals(dir.absolutePath, link.parent)
        }
    }
}