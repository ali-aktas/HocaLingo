// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0" apply false
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
    id("com.google.dagger.hilt.android") version "2.53.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false

    // ✅ Firebase/Google Services plugins - BUNLARI EKLE!
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.firebase.crashlytics") version "3.0.2" apply false
}

buildscript {
    dependencies {
        // Google Services classpath (alternatif yöntem)
        classpath("com.google.gms:google-services:4.4.2")
    }
}