# Enterprise Proguard Matrix for jFindMe
# 🚀 High-Fidelity Protection against R8/Proguard Class Stripping

# Preserve Core Package Logic
-keep class com.jdroidx.jfindme.** { *; }
-keep interface com.jdroidx.jfindme.** { *; }
-keep enum com.jdroidx.jfindme.** { *; }

# Firebase Integrity Handshake
-keep class com.google.firebase.** { *; }
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.database.** { *; }
-dontwarn com.google.firebase.**

# Google Play Services Matrix
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Butterfly Angel Loader (Lottie)
-keep class com.airbnb.lottie.** { *; }

# Chaquopy Python Bridge (if utilized)
-keep class com.chaquo.python.** { *; }

# AndroidX Security & Identity
-keep class androidx.security.crypto.** { *; }

# Native Serialization Attributes
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-keepattributes SourceFile, LineNumberTable

# Prevent obfuscation of field names used in Firebase Database mapping
-keepclassmembers class * {
  @com.google.firebase.database.IgnoreExtraProperties *;
  @com.google.firebase.database.PropertyName *;
}

# General Reflection Preservation
-keepclassmembernames class * {
    fun *(...);
    var *;
}

# Hardened Firebase & Telemetry Model Keep Rules
-keep class com.jdroidx.jfindme.data.model.** { *; }
-keepclassmembers class com.jdroidx.jfindme.data.model.** {
    <fields>;
    <methods>;
}

# Keep SystemLogger completely intact for dynamic logging & reflection
-keep class com.jdroidx.jfindme.utils.SystemLogger { *; }

# ZXing Barcode & QR Scanner Engine
-keep class com.journeyapps.barcodescanner.** { *; }
-keep class com.google.zxing.** { *; }
-dontwarn com.journeyapps.barcodescanner.**
-dontwarn com.google.zxing.**

# AndroidX WorkManager & Coroutines
-keep class androidx.work.** { *; }
-keep class kotlinx.coroutines.** { *; }