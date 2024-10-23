package droid.elfexec

import droid.utils.Utils

object ExesNames {
    const val FOO_NAME = "jnifoo"
    val FOO: String = Utils.fullSoName(FOO_NAME)
    const val BAR_NAME = "exebar"
    val BAR: String = Utils.fullSoName(BAR_NAME)
    const val BAZ_NAME = "exebaz"
    val BAZ: String = Utils.fullSoName(BAZ_NAME)
    val APK_EXES : Set<String> = HashSet(listOf(FOO, BAR, BAZ))
}