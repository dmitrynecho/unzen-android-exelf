package unzen.exelf

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
            // Search for string UNZEN-VERSION-XXXX in ELF file
            val buf = CharArray(17)
            var c: Int
            while ((reader.read().also { c = it }) != -1) {
                if (c == 'U'.code) {
                    reader.mark(17)
                    val readed = reader.read(buf)
                    if (readed == -1) {
                        break
                    } else if (readed == 17) {
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
                if (sb.length > 0) {
                    sb.append("\n")
                }
                sb.append(line)
            }
        }
        return sb.toString()
    }
}
