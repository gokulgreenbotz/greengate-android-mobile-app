plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.greenbotz.greengate"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.greenbotz.greengate"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // The GreenGate Backend (api-gateway) the app signs in against. Set
        // greengate.apiBaseUrl in ~/.gradle/gradle.properties or pass -Pgreengate.apiBaseUrl=…;
        // the default is the emulator's alias for a Backend on this machine.
        val apiBaseUrl = (project.findProperty("greengate.apiBaseUrl") as String? ?: "http://10.0.2.2:8080").trimEnd('/')
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
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
    // LiveKit Cloud (third provider; own prefixed WebRTC, no clash with Twilio/Vonage)
    implementation("io.livekit:livekit-android:2.28.2")

    // Real-time Call Signaling dependencies
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    // Push notifications (FCM)
    implementation(platform("com.google.firebase:firebase-bom:35.0.0"))
    implementation("com.google.firebase:firebase-messaging")

    // Booking QR codes
    implementation("com.google.zxing:core:3.5.3")

    testImplementation(libs.junit)
    // Runs the resident API client against a local server; org.json replaces Android's stub on the JVM.
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}