import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ID AdMob: mặc định là ID TEST của Google. Khi phát hành thật, truyền qua gradle.properties
// hoặc biến môi trường (xem README / docs): ADMOB_APP_ID, ADMOB_BANNER, ADMOB_APP_OPEN.
// App chỉ dùng 2 loại quảng cáo: banner và quảng cáo mở app.
fun cfg(name: String, fallback: String): String =
    (findProperty(name) as String?)?.trim()?.takeIf { it.isNotBlank() } ?: System.getenv(name)?.trim()?.takeIf { it.isNotBlank() } ?: fallback

/** ID quảng cáo thử nghiệm của Google đều thuộc tài khoản này. */
val ADMOB_TEST_PUBLISHER = "ca-app-pub-3940256099942544"

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
        buildConfigField("String", "ADMOB_APP_OPEN", "\"${cfg("ADMOB_APP_OPEN", "ca-app-pub-3940256099942544/9257395921")}\"")
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
            // Đưa biểu tượng gỡ lỗi của thư viện native (WebRTC, AdMob...) vào chính file .aab để Play Console hết cảnh báo
            ndk { debugSymbolLevel = "FULL" }
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
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        // libkatago.so là tệp CHẠY ĐƯỢC (KataGo cho cờ vây), phải được giải nén ra thư mục thư viện để app gọi bằng ProcessBuilder
        jniLibs.useLegacyPackaging = true
    }
    bundle { language { enableSplit = false } }   // giữ đủ ngôn ngữ để đổi ngôn ngữ trong app
    lint { abortOnError = false }

    testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Bản release: cảnh báo rõ ràng nếu còn dùng ID quảng cáo thử nghiệm (app sẽ hiện "Test Ad").
// Đặt REQUIRE_REAL_ADS=true (CI làm sẵn) để biến cảnh báo thành lỗi build.
gradle.taskGraph.whenReady {
    if (allTasks.any { it.path.startsWith(":app:") && it.name.contains("Release") }) {
        val test = listOf("ADMOB_APP_ID", "ADMOB_BANNER", "ADMOB_APP_OPEN").filter { cfg(it, ADMOB_TEST_PUBLISHER).startsWith(ADMOB_TEST_PUBLISHER) }
        if (test.isNotEmpty()) {
            val msg = "Bản release đang dùng ID quảng cáo THỬ NGHIỆM cho: ${test.joinToString()} -> app sẽ hiện quảng cáo \"Test Ad\". " +
                "Đặt các biến này (GitHub: Settings > Secrets and variables > Actions > Variables hoặc Secrets)."
            if ((System.getenv("REQUIRE_REAL_ADS") ?: findProperty("REQUIRE_REAL_ADS") as String?) == "true") throw GradleException(msg)
            logger.warn("WARNING: $msg")
        }
    }
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
    implementation(libs.play.billing)
    implementation(libs.play.services.blockstore)
}
