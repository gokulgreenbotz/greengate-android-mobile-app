import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Resident sign-in API (app-web-ui-client-onboarding). Debug builds can point elsewhere with
// residentApi.baseUrl in local.properties, e.g. http://10.0.2.2:5176 for a local backend on the emulator.
val residentApiBaseUrl = "https://onboarding.gbotz.site"
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val debugResidentApiBaseUrl = localProperties.getProperty("residentApi.baseUrl") ?: residentApiBaseUrl

android {
    namespace = "com.example.greengate"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.greengate"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["usesCleartextTraffic"] = false
    }

    buildTypes {
        debug {
            buildConfigField("String", "RESIDENT_API_BASE_URL", "\"$debugResidentApiBaseUrl\"")
            manifestPlaceholders["usesCleartextTraffic"] = debugResidentApiBaseUrl.startsWith("http://")
        }
        release {
            buildConfigField("String", "RESIDENT_API_BASE_URL", "\"$residentApiBaseUrl\"")
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
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Multi-Provider Video Calling dependencies
    implementation("com.twilio:video-android:7.10.4")
    implementation("com.vonage:client-sdk-video:2.30.1")

    // Real-time Call Signaling dependencies
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    // Booking QR codes
    implementation("com.google.zxing:core:3.5.3")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}