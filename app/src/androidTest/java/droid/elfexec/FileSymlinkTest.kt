package droid.elfexec

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import droid.libcho.FileUtils.existsFollowLinks
import droid.libcho.FileUtils.existsNoFollowLinks
import droid.libcho.FileUtils.fileListedInDir
import droid.libcho.FileUtils.isSymlink
import droid.libcho.FileUtils.readSymlink
import droid.libcho.FileUtils.symlink
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileSymlinkTest {
    @Throws(Exception::class)
    private fun testSymlink(target: File, linkDir: File, link: File) {
        val targetPath = target.absolutePath
        val linkPath = link.absolutePath
        Assert.assertTrue(target.exists())
        Assert.assertEquals(linkDir.absolutePath, link.parent)
        if (link.exists()) {
            Assert.assertTrue(link.delete())
        }
        Assert.assertFalse(link.exists())
        val linkMessage = String.format(
            "%nLink: %b, %b, %b, %b, %b, %b%n[%s]%n[%s]%n[%s]",
            link.exists(), link.canRead(), link.canExecute(), link.canWrite(),
            link.isDirectory, link.isFile,
            link.absolutePath, link.canonicalPath, linkDir
        )
        val targetMessage = String.format(
            "%nTarget: %b, %b, %b, %b, %b, %b%n[%s]%n[%s]",
            target.exists(), target.canRead(), target.canExecute(), target.canWrite(),
            target.isDirectory, target.isFile,
            target.absolutePath, target.canonicalPath
        )
        if (existsNoFollowLinks(link)) {
            Assert.assertFalse(existsFollowLinks(link))
            Assert.assertTrue(isSymlink(link))
            Assert.assertTrue(fileListedInDir(linkDir, link))
            val deadTarget = File(readSymlink(link))
            Assert.assertFalse(deadTarget.exists())
            Assert.assertNotEquals(target, deadTarget)
            Assert.assertTrue(link.delete())
            Assert.assertFalse(existsNoFollowLinks(link))
        }
        Assert.assertFalse(linkMessage + targetMessage, fileListedInDir(linkDir, link))
        symlink(targetPath, linkPath)
        Assert.assertEquals(linkDir.absolutePath, link.parent)
        Assert.assertTrue(fileListedInDir(linkDir, link))
        Assert.assertTrue(link.exists())
    }

    @Test
    @Throws(Exception::class)
    fun test() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val normalDir = context.filesDir
        val link = File(normalDir, "link")
        val normalFile = File(normalDir, "temp")
        Assert.assertTrue(!normalFile.exists() || normalFile.delete())
        Assert.assertTrue(normalFile.createNewFile())
        testSymlink(normalFile, normalDir, link)
        val systemDir = File(context.applicationInfo.nativeLibraryDir)
        val soFile = File(systemDir, ExesNames.FOO)
        Assert.assertTrue(soFile.exists())
        testSymlink(soFile, normalDir, link)
    }
}