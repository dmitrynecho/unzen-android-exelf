package droid.elfexec

import android.content.Context
import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale

object Utils {
    fun format(format: String?, vararg args: Any?): String {
        return String.format(Locale.US, format!!, *args)
    }

    fun fullSoName(name: String): String {
        return "lib$name.so"
    }

    fun shortenAbisNames(s: String): String {
        return s.replace("x86_64", "x64").replace("x86", "x32")
            .replace("armeabi-v7a", "a32").replace("arm64-v8a", "a64")
    }

    fun apkUnpackDir(c: Context): File {
        return File(c.cacheDir, "unzen-apk")
    }

    fun findApkLibsDir(c: Context): File {
        val apkDir = apkUnpackDir(c)
        @Suppress("ConstantConditionIf") if (true) {
            apkDir.walk().forEach {
                if (it.isDirectory) {
                    println("findApkLibsDir 1: $it")
                }
            }
            File(c.packageResourcePath).parentFile!!.walk().forEach {
                println("findApkLibsDir 2: $it")
            }
            File(c.applicationInfo.nativeLibraryDir).walk().forEach {
                println("findApkLibsDir 3: $it")
            }
        }
        val apkLibsDirNormal = File(apkDir, "lib")
        return if (apkLibsDirNormal.exists()) {
            println("findApkLibsDir standalone case: $apkLibsDirNormal")
            apkLibsDirNormal
        } else {
            val res = File(c.applicationInfo.nativeLibraryDir).parentFile
            println("findApkLibsDir split case: $res")
            res!!
        }
    }

    @Throws(IOException::class)
    fun unpackApk(c: Context): File {
        val apkDir = apkUnpackDir(c)
        FileUtils.deleteDirectory(apkDir)
        Assert.assertTrue(!apkDir.exists() && apkDir.mkdirs())
        ZipUtils.extract(File(c.packageResourcePath), apkDir)
        val assetsDir = File(apkDir, "assets")
        val dummy = File(assetsDir, "dummy.txt")
        Assert.assertTrue(dummy.exists() && dummy.length() > 0)
        val dummyLib = File(assetsDir, "dummy-lib.txt")
        Assert.assertTrue(dummyLib.exists() && dummyLib.length() > 0)
        return apkDir
    }

    @get:Suppress("deprecation")
    val supportedAbis: Array<String>
        // Suppress warnings for Gradle build output
        get() = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            Build.SUPPORTED_ABIS
        } else {
            arrayOf(Build.CPU_ABI, Build.CPU_ABI2)
        }

    fun parseVerFromOutput(output: String): Int {
        return output.substring(output.lastIndexOf("-") + 1).toInt()
    }

    @Throws(IOException::class)
    fun parseVerFromFile(file: File?): Int {
        FileInputStream(file).use { stream ->
            val reader = BufferedReader(InputStreamReader(stream))
            // Search for string "UNZEN-VERSION-XXXX" in ELF file.
            val bufSize = "NZEN-VERSION-XXXX".length
            val buf = CharArray(bufSize)
            var c: Int
            while ((reader.read().also { c = it }) != -1) {
                if (c == 'U'.code) {
                    reader.mark(bufSize)
                    val readed = reader.read(buf)
                    if (readed == -1) {
                        break
                    } else if (readed == bufSize) {
                        val ver = String(buf)
                        if (ver.startsWith("NZEN-VERSION-")) {
                            return ver.substring(ver.lastIndexOf("-") + 1).toInt()
                        }
                    }
                    reader.reset()
                }
            }
        }
        return 0
    }

    fun executeFromAppFiles(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    }

    @Throws(IOException::class)
    fun getExeOutput(exe: File): String {
        check(exe.exists()) { "Exe not exists: " + exe.absolutePath }
        check(exe.canExecute()) { "Exe not executable: " + exe.absolutePath }
        val builder = ProcessBuilder(exe.absolutePath)
        val process = builder.start()
        val sb = StringBuilder()
        process.inputStream.use { stream ->
            val reader = BufferedReader(InputStreamReader(stream))
            var line: String?
            while ((reader.readLine().also { line = it }) != null) {
                if (sb.isNotEmpty()) {
                    sb.append("\n")
                }
                sb.append(line)
            }
        }
        return sb.toString()
    }

    private val bytesToHexArray: CharArray = "0123456789abcdef".toCharArray()

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = bytesToHexArray[v ushr 4]
            hexChars[j * 2 + 1] = bytesToHexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}
