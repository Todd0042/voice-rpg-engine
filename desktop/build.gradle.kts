plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

sourceSets {
    val main by getting {
        kotlin.srcDir("src/main/kotlin")
        kotlin.srcDir("../app/src/main/java")
        kotlin.exclude(
            "com/voicerpg/engine/MainActivity.kt",
            "com/voicerpg/engine/localization/TranslationManager.kt",
            "com/voicerpg/engine/model/LanguageCatalog.kt",
            "com/voicerpg/engine/ui/creation/AvatarSynthesizer.kt",
            "com/voicerpg/engine/ui/creation/SelfieFilterDialog.kt",
            "com/voicerpg/engine/ui/creation/SelfiePortraitProcessor.kt"
        )
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-common:2.8.4")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("com.google.code.gson:gson:2.11.0")
}

compose.desktop {
    application {
        mainClass = "com.voicerpg.engine.desktop.MainKt"
    }
}
