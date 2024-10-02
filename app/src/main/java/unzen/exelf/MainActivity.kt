package unzen.exelf

import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
import android.widget.TextView
import unzen.exelf.cuscuta.Cuscuta
import java.io.File
import java.io.IOException
import java.util.Arrays
import java.util.Objects

class MainActivity : Activity() {
    private class Report(
        val name: String, val abisToVers: Map<String?, Int>,
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
                if (result.length > 0) {
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

    @Throws(IOException::class)
    private fun getJniReport(apkDir: File): Report {
        val apkLibsDir = File(apkDir, "lib")
        val abisToVers: MutableMap<String?, Int> = HashMap()
        var totalSize: Long = 0
        for (abiDir in Objects.requireNonNull<Array<File>>(apkLibsDir.listFiles())) {
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
        Assert.assertTrue(!setExec || barExe.setExecutable(true))
        val barOut = Utils.getExeOutput(barExe)
        checkOutput(BAR_NAME, barOut)
        val barVer = Utils.parseVerFromOutput(barOut)
        val bazExe = File(exesDir, if (fullName) BAZ else BAZ_NAME)
        Assert.assertTrue(!setExec || bazExe.setExecutable(true))
        val bazOut = Utils.getExeOutput(bazExe)
        checkOutput(BAZ_NAME, bazOut)
        val bazVer = Utils.parseVerFromOutput(barOut)
        Assert.assertTrue(barVer == bazVer, Utils.format("VerFromOutput %d != %d", barVer, bazVer))
        return barVer
    }

    @Throws(Exception::class)
    private fun exesVerFromOutputSymlinks(exesDir: File): Int {
        val linksDir = File(cacheDir, "exe-links")
        Assert.assertTrue(linksDir.exists() || linksDir.mkdirs())
        for (exe in arrayOf<String>(BAR_NAME, BAZ_NAME)) {
            val target = File(exesDir, Utils.fullSoName(exe))
            Assert.assertTrue(target.exists())
            val symlink = File(linksDir, exe)
            if (symlink.exists()) {
                Assert.assertTrue(symlink.delete())
            }
            Assert.assertFalse(symlink.exists())
            if (FileUtils.existsNoFollowLinks(symlink)) {
                Assert.assertFalse(FileUtils.existsFollowLinks(symlink))
                Assert.assertTrue(FileUtils.isSymlink(symlink))
                Assert.assertTrue(FileUtils.fileListedInDir(linksDir, symlink))
                val deadTarget = File(FileUtils.readSymlink(symlink))
                Assert.assertFalse(deadTarget.exists())
                Assert.assertFalse(target == deadTarget)
                Assert.assertTrue(symlink.delete())
                Assert.assertFalse(FileUtils.existsNoFollowLinks(symlink))
            }
            Assert.assertFalse(FileUtils.fileListedInDir(linksDir, symlink))
            FileUtils.symlink(target.absolutePath, symlink.absolutePath)
            Assert.assertTrue(symlink.exists())
        }
        return exesVerFromOutput(linksDir, false, false)
    }

    @Throws(Exception::class)
    private fun getExeReport(apkDir: File): Report? {
        val apkLibsDir = File(apkDir, "lib")
        val abisToVers: MutableMap<String?, Int> = HashMap()
        var totalSize: Long = 0
        for (abiDir in Objects.requireNonNull<Array<File>>(apkLibsDir.listFiles())) {
            val filesNames = Objects.requireNonNull(abiDir.list())
            val names: Set<String> = HashSet(Arrays.asList(*filesNames))
            if (!names.contains(BAR) && !names.contains(BAZ)) {
                error("APK missing ELFs.%n%s", names)
                return null
            }
            Assert.assertTrue(APK_EXES == names, names.toString())
            val bar = File(abiDir, BAR)
            val baz = File(abiDir, BAZ)
            val barVer = Utils.parseVerFromFile(bar)
            val bazVer = Utils.parseVerFromFile(baz)
            Assert.assertTrue(
                barVer == bazVer,
                Utils.format("VerFromFile %d != %d", barVer, bazVer)
            )
            abisToVers[abiDir.name] = barVer
            totalSize += bar.length() + baz.length()
        }
        val exeDir = File(applicationInfo.nativeLibraryDir)
        val verFromOutputDirect = exesVerFromOutput(exeDir, true, false)
        val verFromOutputLinks = exesVerFromOutputSymlinks(exeDir)
        Assert.assertTrue(verFromOutputDirect == verFromOutputLinks)
        val verFromOutput = verFromOutputLinks
        Assert.assertFalse(verFromOutput == -1)
        if (Utils.executeFromAppFiles()) {
            for (abi in Utils.supportedAbis) {
                if (abisToVers.containsKey(abi)) {
                    val execDir = File(apkLibsDir, abi)
                    val ver = exesVerFromOutput(execDir, true, true)
                    Assert.assertTrue(ver == verFromOutput)
                    break
                }
            }
        }
        return Report("exebar, exebaz", abisToVers, totalSize, verFromOutput)
    }

    private val messages = ArrayList<String?>()
    private val warns = ArrayList<String?>()
    private val errors = ArrayList<String?>()

    private fun message(m: String) {
        messages.add(m)
    }

    /** @noinspection unused
     */
    private fun message(format: String, vararg args: Any) {
        message(Utils.format(format, *args))
    }

    private fun warn(m: String) {
        warns.add(m)
    }

    private fun warn(format: String, vararg args: Any) {
        warn(Utils.format(format, *args))
    }

    private fun error(m: String) {
        errors.add(m)
    }

    /** @noinspection SameParameterValue
     */
    private fun error(format: String, vararg args: Any) {
        error(Utils.format(format, *args))
    }

    private fun checkJniExeReports(jniReport: Report, exeReport: Report?) {
        if (exeReport == null) {
            return
        }
        Assert.assertTrue(jniReport.abisToVers == exeReport.abisToVers)
        Assert.assertTrue(jniReport.verFromOutput == BuildConfig.BASE_VERSION_CODE)
        Assert.assertTrue(exeReport.verFromOutput == BuildConfig.BASE_VERSION_CODE)
        if (BuildConfig.FLAVOR == "fat") {
            Assert.assertTrue(BuildConfig.VERSION_CODE == BuildConfig.BASE_VERSION_CODE)
            Assert.assertTrue(mutableListOf(1, 2, 3, 4).contains(jniReport.abisToVers.size))
        } else {
            Assert.assertTrue(jniReport.abisToVers.size == 1)
            if (BuildConfig.FLAVOR == "a32") {
                Assert.assertTrue(BuildConfig.VERSION_CODE == BuildConfig.BASE_VERSION_CODE + 1)
            } else if (BuildConfig.FLAVOR == "a64") {
                Assert.assertTrue(BuildConfig.VERSION_CODE == BuildConfig.BASE_VERSION_CODE + 2)
            } else if (BuildConfig.FLAVOR == "x32") {
                Assert.assertTrue(BuildConfig.VERSION_CODE == BuildConfig.BASE_VERSION_CODE + 3)
            } else if (BuildConfig.FLAVOR == "x64") {
                Assert.assertTrue(BuildConfig.VERSION_CODE == BuildConfig.BASE_VERSION_CODE + 4)
            }
        }
        val runBuildTypeWarn = ("That's may be due to build performed by"
                + " Android Studio's \"Run\" action, that makes new build"
                + " only for ABI of the \"Run\" target's device. May be"
                + " resolved by \"Build\" -> \"Make Project\".")
        if (!jniReport.versInSync(BuildConfig.BASE_VERSION_CODE)) {
            warn("Versions between ABIs doesn't match. $runBuildTypeWarn")
        }
        if (BuildConfig.FLAVOR == "fat" && jniReport.abisToVers.size != 4) {
            warn(
                "Flavor \"fat\" has only %d ABIs, expected 4 ABIs. "
                        + runBuildTypeWarn, jniReport.abisToVers.size
            )
        }
    }

    private fun displayReport(jniReport: Report, exeReport: Report?, textView: TextView) {
        val header = ArrayList<String?>()
        header.add(
            Utils.format(
                "Java v%s, Cpp v%d",
                BuildConfig.VERSION_NAME, BuildConfig.BASE_VERSION_CODE
            )
        )
        header.add(
            """
    
    $jniReport
    """.trimIndent()
        )
        if (exeReport != null) {
            header.add(
                """
    
    $exeReport
    """.trimIndent()
            )
        }
        if (!messages.isEmpty()) {
            header.add("\n")
            header.addAll(messages)
        }
        var text = TextUtils.join("\n", header)
        if (!warns.isEmpty()) {
            text = Utils.format("%s%n%n%nWARNINGS%n%n%s", text, TextUtils.join("\n\n", warns))
        }
        if (!errors.isEmpty()) {
            text = Utils.format("%s%n%n%nERRORS%n%n%s", text, TextUtils.join("\n\n", errors))
        }

        textView.text = text
        if (!errors.isEmpty()) {
            textView.setTextColor(-0x10000)
        } else if (!warns.isEmpty()) {
            textView.setTextColor(-0x36bf6)
        } else {
            textView.setTextColor(-0xff00ab)
        }
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
        val elfs: Set<String> = HashSet(Arrays.asList(*fileNames))
        if (elfs == APK_EXES) {
            if (isSymlinkInPath(dir)) {
                message(nativeLibraryDirMessage(dir, elfs))
            }
        } else {
            warn(nativeLibraryDirMessage(dir, elfs))
        }
    }

    @Throws(IOException::class)
    private fun unpackApk(): File {
        val apkDir = File(cacheDir, "unzen-apk")
        FileUtils.deleteDirectory(apkDir)
        Assert.assertTrue(!apkDir.exists() && apkDir.mkdirs())
        ZipUtils.extract(File(packageResourcePath), apkDir)
        val assetsDir = File(apkDir, "assets")
        val dummy = File(assetsDir, "dummy.txt")
        Assert.assertTrue(dummy.exists() && dummy.length() > 0)
        val dummyLib = File(assetsDir, "dummy-lib.txt")
        Assert.assertTrue(dummyLib.exists() && dummyLib.length() > 0)
        return apkDir
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        try {
            nativeLibraryDirReport()
            val apkDir = unpackApk()
            val jniReport = getJniReport(apkDir)
            val exeReport = getExeReport(apkDir)
            checkJniExeReports(jniReport, exeReport)
            displayReport(jniReport, exeReport, findViewById(R.id.main_text))
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    companion object {
        private const val FOO_NAME = "jnifoo"
        public val FOO: String = Utils.fullSoName(FOO_NAME)
        private const val BAR_NAME = "exebar"
        val BAR: String = Utils.fullSoName(BAR_NAME)
        private const val BAZ_NAME = "exebaz"
        val BAZ: String = Utils.fullSoName(BAZ_NAME)
        private val APK_EXES
                : Set<String> = HashSet(Arrays.asList(FOO, BAR, BAZ))

        private fun checkOutput(elfName: String, actualOut: String) {
            val expected = Utils.format("I'm %s! UNZEN-VERSION-", elfName)
            val message = Utils.format("Expected: %s, actual: %s", expected, actualOut)
            Assert.assertTrue(actualOut.startsWith(expected), message)
        }
    }
}
