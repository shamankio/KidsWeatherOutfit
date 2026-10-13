// Data layer: implements the :domain repository interfaces (Open-Meteo over Retrofit, DataStore,
// fused location, platform geocoder). Everything is `internal`; :app sees only the Koin modules.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.rustanovych.kidsoutfit.data"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        // The OkHttp logging interceptor is gated on this module's BuildConfig.DEBUG.
        buildConfig = true
    }
}

dependencies {
    api(project(":domain"))

    implementation(libs.androidx.core.ktx)

    // DI
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    // Network
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)

    // Storage
    implementation(libs.androidx.datastore.preferences)

    // Location
    implementation(libs.play.services.location)
    // Bridges Play services Task<T> to suspend functions (Task.await).
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
