package droid.elfexec

import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
import android.widget.TextView
import droid.elfexec.Assert.assertTrue
import droid.elfexec.BuildConfig.BASE_VERSION_CODE
import droid.elfexec.BuildConfig.VERSION_CODE
import droid.elfexec.BuildConfig.VERSION_NAME
import droid.elfexec.ExesNames.APK_EXES
import droid.elfexec.ExesNames.BAR
import droid.elfexec.ExesNames.BAR_NAME
import droid.elfexec.ExesNames.BAZ
import droid.elfexec.ExesNames.BAZ_NAME
import droid.elfexec.ExesNames.FOO
import droid.elfexec.Utils.apkUnpackDir
import droid.elfexec.cuscuta.Cuscuta
import java.io.File
import java.io.IOException
import java.util.Objects

class MainActivity : Activity() {
    class Report(
        val name: String, val abisToVers: Map<String, Int>,
        val totalSize: Long, val verFromOutput: Int
    ) {
        fun versInSync(version: Int): Boolean {
            for (v in abisToVers.values) {
                if (version != v) {
                    return false
                }
            }
            return true
        }

        fun header(): String {
            return Utils.format("%s v%d, %d B", name, verFromOutput, totalSize)
        }

        fun body(): String {
            val result = StringBuilder()
            for ((key, value) in abisToVers) {
                if (result.isNotEmpty()) {
                    result.append(", ")
                }
                result.append(key)
                result.append(" ").append("v")
                result.append(value)
            }
            return Utils.shortenAbisNames(result.toString())
        }

        override fun toString(): String {
            return """
                ${header()}
                ${body()}
                """.trimIndent()
        }
    }

    private fun checkOutput(elfName: String, actualOut: String) {
        val expected = Utils.format("I'm %s! UNZEN-VERSION-", elfName)
        val message = Utils.format("Expected: %s, actual: %s", expected, actualOut)
        assertTrue(actualOut.startsWith(expected), message)
    }

    @Throws(IOException::class)
    private fun getJniReport(): Report {
        val apkLibsDir = Utils.findApkLibsDir(this)
        val abisToVers: MutableMap<String, Int> = HashMap()
        var totalSize: Long = 0
        val abiDirs = apkLibsDir.listFiles()
            ?: throw IllegalStateException("$apkLibsDir list null")
        for (abiDir in abiDirs) {
            val foo = File(abiDir, FOO)
            if (!foo.exists()) {
                continue
            }
            abisToVers[abiDir.name] = Utils.parseVerFromFile(foo)
            totalSize += foo.length()
        }
        val output: String = Cuscuta.stringFromJni!!
        checkOutput(FOO, output)
        return Report(FOO, abisToVers, totalSize, Utils.parseVerFromOutput(output))
    }

    @Throws(IOException::class)
    private fun exesVerFromOutput(exesDir: File, fullName: Boolean, setExec: Boolean): Int {
        val barExe = File(exesDir, if (fullName) BAR else BAR_NAME)
        assertTrue(!setExec || barExe.setExecutable(true))
        val barOut = Utils.getExeOutput(barExe)
        checkOutput(BAR_NAME, barOut)
        val barVer = Utils.parseVerFromOutput(barOut)
        val bazExe = File(exesDir, if (fullName) BAZ else BAZ_NAME)
        assertTrue(!setExec || bazExe.setExecutable(true))
        val bazOut = Utils.getExeOutput(bazExe)
        checkOutput(BAZ_NAME, bazOut)
        val bazVer = Utils.parseVerFromOutput(barOut)
        assertTrue(barVer == bazVer, Utils.format("VerFromOutput %d != %d", barVer, bazVer))
        return barVer
    }

    @Throws(Exception::class)
    private fun exesVerFromOutputSymlinks(exesDir: File): Int {
        val linksDir = File(cacheDir, "exe-links")
        assertTrue(linksDir.exists() || linksDir.mkdirs())
        for (exe in arrayOf(BAR_NAME, BAZ_NAME)) {
            val target = File(exesDir, Utils.fullSoName(exe))
            assertTrue(target.exists())
            val symlink = File(linksDir, exe)
            if (symlink.exists()) {
                assertTrue(symlink.delete())
            }
            Assert.assertFalse(symlink.exists())
            if (FileUtils.existsNoFollowLinks(symlink)) {
                Assert.assertFalse(FileUtils.existsFollowLinks(symlink))
                assertTrue(FileUtils.isSymlink(symlink))
                assertTrue(FileUtils.fileListedInDir(linksDir, symlink))
                val deadTarget = File(FileUtils.readSymlink(symlink))
                Assert.assertFalse(deadTarget.exists())
                Assert.assertFalse(target == deadTarget)
                assertTrue(symlink.delete())
                Assert.assertFalse(FileUtils.existsNoFollowLinks(symlink))
            }
            Assert.assertFalse(FileUtils.fileListedInDir(linksDir, symlink))
            FileUtils.symlink(target.absolutePath, symlink.absolutePath)
            assertTrue(symlink.exists())
        }
        return exesVerFromOutput(linksDir, false, false)
    }

    @Throws(Exception::class)
    private fun getExeReport(): Report? {
        val apkLibsDir = Utils.findApkLibsDir(this)
        val abisToVers: MutableMap<String, Int> = HashMap()
        var totalSize: Long = 0
        for (abiDir in Objects.requireNonNull<Array<File>>(apkLibsDir.listFiles())) {
            val filesNames = Objects.requireNonNull(abiDir.list())
            val names: Set<String> = HashSet(listOf(*filesNames))
            if (!names.contains(BAR) && !names.contains(BAZ)) {
                error("APK missing ELFs.%n%s", names)
                return null
            }
            assertTrue(APK_EXES == names, names.toString())
            val bar = File(abiDir, BAR)
            val baz = File(abiDir, BAZ)
            val barVer = Utils.parseVerFromFile(bar)
            val bazVer = Utils.parseVerFromFile(baz)
            assertTrue(
                barVer == bazVer,
                Utils.format("VerFromFile %d != %d", barVer, bazVer)
            )
            abisToVers[abiDir.name] = barVer
            totalSize += bar.length() + baz.length()
        }
        val exeDir = File(applicationInfo.nativeLibraryDir)
        val verFromOutputDirect = exesVerFromOutput(exeDir, true, false)
        val verFromOutputLinks = exesVerFromOutputSymlinks(exeDir)
        assertTrue(verFromOutputDirect == verFromOutputLinks)
        val verFromOutput = verFromOutputLinks
        Assert.assertFalse(verFromOutput == -1)
        if (Utils.executeFromAppFiles()) {
            for (abi in Utils.supportedAbis) {
                if (abisToVers.containsKey(abi)) {
                    val execDir = File(apkLibsDir, abi)
                    val ver = exesVerFromOutput(execDir, true, true)
                    assertTrue(ver == verFromOutput)
                    break
                }
            }
        }
        return Report("exebar, exebaz", abisToVers, totalSize, verFromOutput)
    }

    private val messages = ArrayList<String>()
    private val warns = ArrayList<String>()
    private val errors = ArrayList<String>()

    private fun message(m: String) {
        messages.add(m)
    }

    private fun message(format: String, vararg args: Any) {
        message(Utils.format(format, *args))
    }

    private fun warn(m: String) {
        warns.add(m)
    }

    @Suppress("SameParameterValue")
    private fun warn(format: String, vararg args: Any) {
        warn(Utils.format(format, *args))
    }

    private fun error(m: String) {
        errors.add(m)
    }

    @Suppress("SameParameterValue")
    private fun error(format: String, vararg args: Any) {
        error(Utils.format(format, *args))
    }

    private fun checkReports(info: ApksInfo) {
        if (info.exeReport == null) {
            return
        }
        val jniReport = info.jniReport
        val exeReport = info.exeReport
        val abisCount = jniReport.abisToVers.size
        assertTrue(jniReport.abisToVers == exeReport.abisToVers)
        assertTrue(jniReport.verFromOutput == BASE_VERSION_CODE)
        assertTrue(exeReport.verFromOutput == BASE_VERSION_CODE)
        @Suppress("KotlinConstantConditions")
        if (BuildConfig.FLAVOR == "fat") {
            assertTrue(VERSION_CODE == BASE_VERSION_CODE)
            assertTrue(mutableListOf(1, 2, 3, 4).contains(abisCount))
        } else {
            assertTrue(abisCount == 1)
            when (BuildConfig.FLAVOR) {
                "a32" -> {
                    assertTrue(VERSION_CODE == BASE_VERSION_CODE + 1)
                }
                "a64" -> {
                    assertTrue(VERSION_CODE == BASE_VERSION_CODE + 2)
                }
                "x32" -> {
                    assertTrue(VERSION_CODE == BASE_VERSION_CODE + 3)
                }
                "x64" -> {
                    assertTrue(VERSION_CODE == BASE_VERSION_CODE + 4)
                }
            }
        }
        if (!jniReport.versInSync(BASE_VERSION_CODE)) {
            throw IllegalStateException("Versions between ABIs doesn't match.")
        }
        @Suppress("KotlinConstantConditions")
        if (BuildConfig.FLAVOR == "fat" && abisCount != 4) {
            if (abisCount != 1) {
                throw IllegalStateException("Flavor fat, but ABIs count $abisCount")
            }
            if (!info.isSplitInstall) {
                val runBuildTypeWarn = ("That's may be due to build performed by"
                        + " Android Studio's \"Run\" action, that makes new build"
                        + " only for ABI of the \"Run\" target's device.")
                warn("Flavor \"fat\" has only %d ABIs, expected 4 ABIs. "
                        + runBuildTypeWarn, abisCount)
            }
        }
    }

    private fun displayText(info: ApksInfo):String {
        val header = ArrayList<String>()
        header.add(
            Utils.format("Java v%s, Cpp v%d.",
                VERSION_NAME, BASE_VERSION_CODE)
        )
        header.add("\n${info.jniReport}")

        if (info.exeReport != null) {
            header.add("\n${info.exeReport}")
        }

        header.add("\n${info}")

        if (messages.isNotEmpty()) {
            header.add("\n")
            header.addAll(messages)
        }
        var text = TextUtils.join("\n", header)
        if (warns.isNotEmpty()) {
            text = Utils.format("%s%n%n%nWARNINGS%n%n%s",
                text, TextUtils.join("\n\n", warns))
        }
        if (errors.isNotEmpty()) {
            text = Utils.format("%s%n%n%nERRORS%n%n%s",
                text, TextUtils.join("\n\n", errors))
        }
        return text
    }

    @Throws(IOException::class)
    private fun isSymlinkInPath(file: File): Boolean {
        if (FileUtils.isSymlink(file)) {
            return true
        }
        return file.absolutePath != file.canonicalPath
    }

    @Throws(IOException::class)
    private fun nativeLibraryDirMessage(dir: File, elfs: Set<String>): String {
        var message = "getApplicationInfo().nativeLibraryDir"
        message += """
            
            $elfs
            """.trimIndent()
        message += """
            
            ${applicationInfo.nativeLibraryDir}
            """.trimIndent()
        if (isSymlinkInPath(dir)) {
            message += """
                
                ${FileUtils.readSymlink(dir)}
                """.trimIndent()
        }
        return message
    }

    @Throws(IOException::class)
    private fun nativeLibraryDirReport() {
        val dir = File(applicationInfo.nativeLibraryDir)
        val fileNames = Objects.requireNonNull(dir.list())
        val elfs: Set<String> = HashSet(listOf(*fileNames))
        if (elfs == APK_EXES) {
            if (isSymlinkInPath(dir)) {
                message(nativeLibraryDirMessage(dir, elfs))
            }
        } else {
            warn(nativeLibraryDirMessage(dir, elfs))
        }
    }

    @Throws(IOException::class)
    fun unpackApk(): File {
        val apkDir = apkUnpackDir(this)
        FileUtils.deleteDirectory(apkDir)
        assertTrue(!apkDir.exists() && apkDir.mkdirs())
        ZipUtils.extract(File(packageResourcePath), apkDir)
        val assetsDir = File(apkDir, "assets")
        val dummy = File(assetsDir, "dummy.txt")
        assertTrue(dummy.exists() && dummy.length() > 0)
        val dummyLib = File(assetsDir, "dummy-lib.txt")
        assertTrue(dummyLib.exists() && dummyLib.length() > 0)
        return apkDir
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        try {
            nativeLibraryDirReport()
            unpackApk()
            val jniReport = getJniReport()
            val exeReport = getExeReport()
            val info = ApksInfo(this, jniReport, exeReport)
            checkReports(info)
            val text = displayText(info)
            val tv: TextView = findViewById(R.id.main_text)
            tv.text = text
            if (errors.isNotEmpty()) {
                tv.setTextColor(-0x10000)
            } else if (warns.isNotEmpty()) {
                tv.setTextColor(-0x36bf6)
            } else {
                tv.setTextColor(-0xff00ab)
            }
            println(info.toStringVerbose())
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
