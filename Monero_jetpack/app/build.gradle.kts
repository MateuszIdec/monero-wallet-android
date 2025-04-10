plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.monero_jetpack"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.monero_jetpack"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    // Compose Material 3
    implementation(libs.material3)

    // Compose UI
    implementation(libs.ui)
    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))

    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-auth")

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation (libs.googleid)
    implementation ("com.google.android.gms:play-services-auth:21.3.0")

    implementation ("com.google.zxing:core:3.5.2")

    implementation ("com.google.mlkit:barcode-scanning:17.3.0")
    implementation ("com.google.android.gms:play-services-mlkit-barcode-scanning:18.2.0")
    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation (libs.accompanist.permissions)

    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.1")

    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.0")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.0")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.0")
    implementation ("com.squareup.okhttp3:okhttp:4.10.0")

    implementation ("com.google.code.gson:gson:2.8.8")

    implementation ("io.github.ehsannarmani:compose-charts:0.1.2")

}