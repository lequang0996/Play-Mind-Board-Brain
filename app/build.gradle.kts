import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ID AdMob: mặc định là ID TEST của Google. Khi phát hành thật, truyền qua gradle.properties
// hoặc biến môi trường (xem README / docs): ADMOB_APP_ID, ADMOB_BANNER, ADMOB_INTERSTITIAL, ADMOB_REWARDED
fun cfg(name: String, fallback: String): String =
    (findProperty(name) as String?) ?: System.getenv(name)?.takeIf { it.isNotBlank() } ?: fallback

android {
    namespace = "com.fws.playmind"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fws.playmind"
        minSdk = 24
        targetSdk = 36
        versionCode = (System.getenv("VERSION_CODE") ?: (findProperty("VERSION_CODE") as String?) ?: "1").toInt()
        versionName = "1.0.0"
        manifestPlaceholders["admobAppId"] = cfg("ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
        buildConfigField("String", "ADMOB_BANNER", "\"${cfg("ADMOB_BANNER", "ca-app-pub-3940256099942544/9214589741")}\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL", "\"${cfg("ADMOB_INTERSTITIAL", "ca-app-pub-3940256099942544/1033173712")}\"")
        buildConfigField("String", "ADMOB_REWARDED", "\"${cfg("ADMOB_REWARDED", "ca-app-pub-3940256099942544/5224354917")}\"")
        buildConfigField("String", "PRIVACY_URL", "\"${cfg("PRIVACY_URL", "https://lequang0996.github.io/fwsandroid/play-mind/")}\"")
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }

    val ksPath = System.getenv("KEYSTORE_PATH")
    signingConfigs {
        if (ksPath != null && File(ksPath).exists()) {
            create("release") {
                storeFile = File(ksPath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true        // R8: thu nhỏ + làm rối mã
            isShrinkResources = true      // loại bỏ tài nguyên không dùng
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    androidResources { localeFilters += listOf("en", "zh-rCN", "es", "pt", "ru", "ja", "ko", "vi") }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    bundle { language { enableSplit = false } }   // giữ đủ ngôn ngữ để đổi ngôn ngữ trong app
    lint { abortOnError = false }

    testOptions { unitTests { isIncludeAndroidResources = true } }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.ads)
    implementation(libs.ump)
    implementation(libs.play.app.update)
    implementation(libs.play.services.nearby)
    implementation(libs.stream.webrtc)
    implementation(libs.play.services.code.scanner)
    implementation(libs.zxing.core)
}
