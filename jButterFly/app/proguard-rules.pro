# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\jiten\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

-keep class com.jdroidx.jbutterfly.** { *; }
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.airbnb.lottie.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn okio.**
