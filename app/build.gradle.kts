import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Real AdMob ids live only in local.properties (gitignored); they are never printed or committed.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}
val admobAppId: String = localProperties.getProperty("ADMOB_APP_ID").orEmpty()
val admobBannerId: String = localProperties.getProperty("ADMOB_BANNER_ID").orEmpty()
// Optional: hashed id of a physical test phone, printed by the UMP SDK in logcat (see docs/ads-testing.md).
val umpTestDeviceId: String = localProperties.getProperty("UMP_TEST_DEVICE_ID").orEmpty()
// Optional: ads test-device id of a physical phone (printed by the ads SDK in logcat). Makes that phone
// get test ads on real ad units in any build type, so a real ad is never shown or clicked on it.
val adsTestDeviceId: String = localProperties.getProperty("ADS_TEST_DEVICE_ID").orEmpty()

gradle.taskGraph.whenReady {
    val buildsRelease = allTasks.any { it.project == project && it.name.contains("Release") }
    if (buildsRelease && (admobAppId.isBlank() || admobBannerId.isBlank())) {
        throw GradleException(
            "Release builds need ADMOB_APP_ID and ADMOB_BANNER_ID in local.properties " +
                "(see the AdMob section of CLAUDE.md).",
        )
    }
}

android {
    namespace = "com.rustanovych.kidsoutfit"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.rustanovych.kidsoutfit"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Google's public sample ids: a debug build can never serve (or click) a real ad.
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "UMP_TEST_DEVICE_ID", "\"$umpTestDeviceId\"")
            buildConfigField("String", "ADS_TEST_DEVICE_ID", "\"$adsTestDeviceId\"")
        }
        release {
            manifestPlaceholders["admobAppId"] = admobAppId
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerId\"")
            buildConfigField("String", "UMP_TEST_DEVICE_ID", "\"\"")
            buildConfigField("String", "ADS_TEST_DEVICE_ID", "\"$adsTestDeviceId\"")
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // DI
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // JSON (licenses screen, navigation keys)
    implementation(libs.kotlinx.serialization.json)

    // Images
    implementation(libs.coil.compose)

    // Background work
    implementation(libs.androidx.work.runtime.ktx)

    // Ads & consent
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.work.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
