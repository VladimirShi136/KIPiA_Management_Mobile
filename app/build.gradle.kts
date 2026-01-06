plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "com.kipia.management.mobile"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.kipia.management.mobile"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        viewBinding = false
        buildConfig = true
        compose = true
    }

    @Suppress("UnstableApiUsage")
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ========== JETPACK COMPOSE ==========
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose.bundle)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)

    // ========== COIL (ПОЛНАЯ ЗАМЕНА GLIDE) ==========
    implementation(libs.bundles.coil.bundle)

    // Accompanist
    implementation(libs.bundles.accompanist.bundle)

    // ========== БАЗОВЫЕ ЗАВИСИМОСТИ ==========
    implementation(libs.bundles.android.base)
    implementation(libs.bundles.lifecycle.bundle)

    // Navigation Compose
    implementation(libs.navigation.compose)

    // ========== БИБЛИОТЕКИ С KSP ==========

    // Room с KSP
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt с KSP
    implementation(libs.hilt.android)
    ksp(libs.hilt.ksp)

    // Hilt Navigation Compose
    implementation(libs.hilt.navigation.compose)

    // Coroutines
    implementation(libs.coroutines.android)

    // CameraX
    implementation(libs.bundles.camerax.bundle)

    // Прочие зависимости
    implementation(libs.gson)
    implementation(libs.timber)

    // MPAndroidChart
    implementation(libs.mp.android.chart)

    // ========== ТЕСТИРОВАНИЕ ==========
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso)
}