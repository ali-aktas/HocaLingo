plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")

    // ✅ Firebase plugins
    id("com.google.gms.google-services")
    // id("com.google.firebase.crashlytics") // Crashlytics'i sonra aktifleştireceğiz

    // ✅ COMPOSE COMPILER PLUGIN
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.hocalingo.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hocalingo.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 8
        versionName = "1.0.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            // applicationIdSuffix = ".debug"
            // versionNameSuffix = "-debug"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// ✅ KSP configuration
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Android Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.3")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(libs.androidx.hilt.common)

    // ✅ SPLASH SCREEN
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Icons
    implementation(libs.compose.material.icons.extended)

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Dependency Injection - Hilt
    implementation("com.google.dagger:hilt-android:2.53.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")
    implementation(libs.androidx.cardview)
    ksp("com.google.dagger:hilt-compiler:2.53.1")

    // Database - Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ✅ WorkManager for daily notifications
    implementation("androidx.work:work-runtime-ktx:2.8.1")

    // ✅ Hilt WorkManager integration
    implementation("androidx.hilt:hilt-work:1.0.0")
    ksp("androidx.hilt:hilt-compiler:1.0.0")

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // ✅ Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-analytics")
    // implementation("com.google.firebase:firebase-crashlytics") // Sonra
    implementation("com.google.firebase:firebase-config")

    // ✅ Google Sign In
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // AdMob
    implementation(libs.play.services.ads)

    // ✅ MONETIZATION - RevenueCat (AKTIF!)
    implementation("com.revenuecat.purchases:purchases:8.10.0")

    // ❌ AdMob şimdilik kapalı - ADIM 3'te eklenecek
    // implementation("com.google.android.gms:play-services-ads:23.6.0")

    // Image Loading - STABLE VERSION
    implementation("io.coil-kt:coil-compose:2.7.0")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ✅ System UI Controller (Status bar/Navigation bar renkleri için)
    // MainActivity.kt'de kullanıyoruz
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.36.0")

    // ✅ WindowInsets (Edge-to-edge için)
    // Compose'da zaten var ama emin olmak için
    implementation("androidx.compose.foundation:foundation:1.7.5")

    // ✅ Activity Compose (enableEdgeToEdge için)
    // Compose'da zaten var ama version check
    implementation("androidx.activity:activity-compose:1.9.3")

    // ✅ Google Play In-App Review
    implementation(libs.review.ktx)


    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    testImplementation("io.mockk:mockk:1.13.14")

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.53.1")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.53.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}