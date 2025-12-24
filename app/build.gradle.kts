import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.dagger.hilt)
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

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

// Новый блок для Kotlin-компилятора
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17 // Указываем через тип JvmTarget
    }
}

dependencies {
    // Используем bundles из libs.versions.toml
    implementation(libs.bundles.android.base)
    implementation(libs.bundles.lifecycle.bundle)
    implementation(libs.bundles.navigation.bundle)
    implementation(libs.bundles.camerax.bundle) // Добавьте эту строку

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler) // Теперь будет работать

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler) // Теперь будет работать
    implementation(libs.hilt.navigation.fragment)

    // Coroutines
    implementation(libs.coroutines.android)

    // Glide
    implementation(libs.glide)
    kapt(libs.glide.compiler) // Теперь будет работать

    // Прочие зависимости (упростите)
    implementation(libs.gson)
    implementation(libs.timber)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso)
}

// Для Room compiler нужно создать reference вручную если нет
kapt {
    correctErrorTypes = true
}