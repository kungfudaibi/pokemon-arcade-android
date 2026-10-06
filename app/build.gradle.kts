plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.local.pokemonlauncher"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.local.pokemonlauncher"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.2.2-demo"
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging {
        jniLibs { keepDebugSymbols += "**/libmgba_libretro_android.so" }
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("com.github.Swordfish90:libretrodroid:0.9.0")
}
