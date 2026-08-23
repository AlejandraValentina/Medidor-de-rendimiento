plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.medidorderendimiento"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.medidorderendimiento"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        buildConfig = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:domain"))
}
