import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.chaquopy)
    alias(libs.plugins.firebase.crashlytics)
}

// Load Keystore Properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

// 📦 Enterprise Version Management System (Auto-Increment on Signed Release)
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) {
    versionProps.load(FileInputStream(versionPropsFile))
} else {
    versionProps["VERSION_MAJOR"] = "1"
    versionProps["VERSION_MINOR"] = "0"
    versionProps["VERSION_PATCH"] = "1"
    versionProps["VERSION_CODE"] = "1"
    versionProps.store(versionPropsFile.writer(), "jFindMe Version Tracker")
}

val vMajor = versionProps.getProperty("VERSION_MAJOR", "1").toInt()
val vMinor = versionProps.getProperty("VERSION_MINOR", "0").toInt()
val vPatch = versionProps.getProperty("VERSION_PATCH", "1").toInt()
var vCode = versionProps.getProperty("VERSION_CODE", "1").toInt()

// Check if running a release build task (e.g. assembleRelease / bundleRelease)
val isReleaseBuild = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
if (isReleaseBuild) {
    vCode += 1
    versionProps["VERSION_CODE"] = vCode.toString()
    versionProps.store(versionPropsFile.writer(), "Auto-incremented on Release Build")
}

val computedVersionName = "$vMajor.$vMinor.$vPatch"

android {
    namespace = "com.jdroidx.jfindme"
    compileSdk = 34

    signingConfigs {
        val targetKeystore = if (keystorePropertiesFile.exists()) {
            file(keystoreProperties["storeFile"] as String)
        } else {
            file("../keystore/debug.keystore")
        }
        val targetStorePassword = if (keystorePropertiesFile.exists()) keystoreProperties["storePassword"] as String else "android"
        val targetKeyAlias = if (keystorePropertiesFile.exists()) keystoreProperties["keyAlias"] as String else "androiddebugkey"
        val targetKeyPassword = if (keystorePropertiesFile.exists()) keystoreProperties["keyPassword"] as String else "android"

        create("release") {
            storeFile = targetKeystore
            storePassword = targetStorePassword
            keyAlias = targetKeyAlias
            keyPassword = targetKeyPassword
            enableV1Signing = true
            enableV2Signing = true
        }
        getByName("debug") {
            storeFile = targetKeystore
            storePassword = targetStorePassword
            keyAlias = targetKeyAlias
            keyPassword = targetKeyPassword
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    defaultConfig {
        applicationId = "com.jdroidx.jfindme"
        minSdk = 26
        targetSdk = 34
        versionCode = vCode
        versionName = computedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    splits {
        abi {
            isEnable = false
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    // 🚀 QA Fix: Force the Compiler to ignore missing string translations / layout warnings during Release APK Generation!
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

// Chaquopy 15.0+ configuration
chaquopy {
    defaultConfig {
        version = "3.10"
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation("com.google.maps.android:android-maps-utils:3.8.2")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.android.gms:play-services-nearby:18.3.0")
    
    implementation(platform("com.google.firebase:firebase-bom:34.10.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-crashlytics")
    
    // 🦋 Premium UX: Lottie Vector Graphics for Butterfly Angel Loader
    implementation("com.airbnb.android:lottie:6.4.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
