plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

// Kiểm tra tự động luật cờ + AI (perft, chiếu hết, chặn/hoàn thành dãy caro).
// Chạy: gradle :core:selfCheck
tasks.register<JavaExec>("selfCheck") {
    group = "verification"
    description = "Chạy bộ kiểm tra luật cờ và AI"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.fws.playmind.core.SelfCheckKt")
}
tasks.named("check") { dependsOn("selfCheck") }
