package unzen.exelf

import android.content.Context
import unzen.exelf.MainActivity.Report
import unzen.exelf.Utils.apkUnpackDir
import java.io.File

class ApksInfo(c: Context, val jniReport: Report, val exeReport: Report?) {

    val apksInfos = mutableListOf<ApkInfo>()
    val isSplitInstall = !File(apkUnpackDir(c), "lib").exists()
    val installType = if (isSplitInstall) "split" else "standalone"

    init {
        File(c.packageResourcePath).parentFile!!.walk().forEach {
            if (it.extension == "apk") {
                apksInfos.add(ApkInfo(it))
            }
        }
        if (isSplitInstall && apksInfos.size == 1) throw IllegalStateException()
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Install type ").append(installType).append(".").appendLine()
        apksInfos.forEachIndexed { index, apkInfo ->
            if (index != 0) sb.append("\n")
            sb.append(apkInfo.fileName)
                .append(" ")
                .append(apkInfo.fileSize).append(" B")
                .append(" L")
                .append(apkInfo.signsMagicsLowercaseCount)
                .append(" U")
                .append(apkInfo.signsMagicsUppercaseCount)
        }
        return sb.toString()
    }
}