### About

This is a sample Android project that shows how to build ELF executables
alongside with standard JNI shared libraries and package those executables
into APK. Supports build of multiple APKs, splitted by ABI, via build
flavors mechanics. Supports Android 10 with W^X SELinux policy.

### Notes

AGP 4.1.2 and AGP 7.3.1 binary stripping details.

AGP 4.1.2 and AGP 7.3.1 gather solibs from all modules in merged_native_libs dir
of app module and then performs stripping on them that places stripped solibs
in stripped_native_libs. You can see this by search in task execution log
for forementioned dirs and see that stripping task runs after merge task.

In AGP 4.1.2, when project contains modules, they responsible for stripping
their solibs on their own. You can search app build dir for solib names and
you'll see that it contains only already stripped versions of solibs from
modules that app depends on. However, strippend solibs from modules are
copied in app's merged_native_libs dir, so app will redurantly run stripping
on them second time.

In AGP 7.3.1, when project contains modules, stripping performed only once
in app module, modules that app depends on no more perform redurant solib
stripping.

\* \* \*

When Android OS installs debug app builds it process arbitrary named binaries
from APK, but on release app build it ignores everything except files named
like solibs, i.e. "libname.so".

\* \* \*

Android 10 W^X policy related links:
* https://github.com/termux/termux-packages/wiki/Termux-and-Android-10
* https://developer.android.com/about/versions/10/behavior-changes-10#execute-permission
* https://issuetracker.google.com/issues/128554619
* https://developer.android.com/ndk/guides/wrap-script

\* \* \*

Looks like creating hard links without root was disabled since Android 6:
* https://seandroid-list.tycho.nsa.narkive.com/r5ZNxgkh/selinux-hardlink-brain-damage-in-android-m
* https://code.google.com/archive/p/android-developer-preview/issues/3150

### Contacts

dmitryratty@gmail.com