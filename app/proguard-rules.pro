# Enterprise Proguard Rules for jFindMe

# Firebase Rules
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Google Play Services & Maps
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# AndroidX Security
-keep class androidx.security.crypto.** { *; }

# Data Models (Keep for Firebase Serialization)
-keep class com.jdroidx.jfindme.data.model.** { *; }

# Serialization
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepclassmembers class * {
  @com.google.firebase.database.IgnoreExtraProperties *;
}

# Source preservation for crash logs
-keepattributes SourceFile,LineNumberTable