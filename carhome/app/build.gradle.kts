plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.carhome.launcher"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }

    defaultConfig {
        applicationId = "com.carhome.launcher"
        minSdk        = 26
        targetSdk     = 36
        versionCode   = 14
        versionName   = "1.2.11"
    }

    // Releases are signed outside Gradle: the build job makes an unsigned APK and a
    // separate job, which runs no code from this repository, signs it with the key
    // from the repository's encrypted secrets (see .github/workflows/build-carhome.yml).
    // Only for signing on your own computer, set CARHOME_KEYSTORE (path to the .p12)
    // and CARHOME_KEYSTORE_PASSWORD. Debug builds use the standard debug key.
    val releaseKey = System.getenv("CARHOME_KEYSTORE")?.let { file(it) }?.takeIf { it.exists() }
    signingConfigs {
        if (releaseKey != null) {
            create("release") {
                storeFile     = releaseKey
                storeType     = "pkcs12"
                storePassword = System.getenv("CARHOME_KEYSTORE_PASSWORD")
                keyAlias      = "carhome"
                keyPassword   = System.getenv("CARHOME_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseKey != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // Plain JVM unit tests for the pure logic (speed maths, alerts, prayer
    // times, settings). Android calls that aren't mocked return defaults.
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.palette:palette:1.0.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    testImplementation("junit:junit:4.13.2")
    // Real org.json for tests (the Android stub only throws)
    testImplementation("org.json:json:20240303")
}
