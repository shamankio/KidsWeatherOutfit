import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin module: no Android dependencies allowed, so it can move to KMP later.
plugins {
    alias(libs.plugins.kotlin.jvm)
    // For `api`: repository interfaces expose Flow, so consumers need coroutines on their
    // compile classpath.
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    // The only allowed third party dependency: multiplatform-safe, and unavoidable because
    // repository interfaces are Flow-based.
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
