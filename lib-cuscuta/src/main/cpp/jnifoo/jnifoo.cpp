#include <jni.h>
#include <cstdlib>
#include "../global_version.h"

extern "C" JNIEXPORT jstring JNICALL
Java_droid_elfexec_cuscuta_Cuscuta_getStringFromJni(JNIEnv* env, jclass)
{
    char* buf;
    asprintf(&buf, "I'm libjnifoo.so! %s", CUSCUTA_GLOBAL_VERSION);
    jstring result = env->NewStringUTF(buf);
    free(buf);
    return result;
}
