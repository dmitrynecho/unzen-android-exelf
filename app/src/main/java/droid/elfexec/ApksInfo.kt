package droid.elfexec

import android.content.Context
import droid.elfexec.MainActivity.Report
import droid.libcho.Utils.apkUnpackDir
import droid.libcho.ApkInfo
import java.io.File

class ApksInfo(c: Context, val jniReport: Report, val exeReport: Report?) {

    val apksInfos = mutableListOf<ApkInfo>()
    val isSplitInstall = !File(apkUnpackDir(c), "lib").exists()
    val installType = if (isSplitInstall) "split" else "standalone"

    init {
        File(c.packageResourcePath).parentFile!!.walk().forEach {
            if (it.extension == "apk") {
                apksInfos.add(ApkInfo(it, hashesEnabled = false, signEnabled = false))
            }
        }
        if (isSplitInstall && apksInfos.size == 1) throw IllegalStateException()
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Install type ").append(installType).append(".").appendLine()
        apksInfos.forEachIndexed { index, apkInfo ->
            if (index != 0) sb.appendLine()
            sb.append(apkInfo.toString())
        }
        return sb.toString()
    }

    fun toStringVerbose(): String {
        val sb = StringBuilder()
        sb.append("Install type ").append(installType).append(".").appendLine()
        apksInfos.forEachIndexed { index, apkInfo ->
            if (index != 0) sb.appendLine()
            sb.append(apkInfo.toStringVerbose())
        }
        return sb.toString()
    }
}