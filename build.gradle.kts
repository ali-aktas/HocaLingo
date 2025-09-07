// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.9.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0" apply false
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
    id("com.google.dagger.hilt.android") version "2.53.1" apply false

    // ❌ Firebase plugins geçici olarak devre dışı (google-services.json eksik)
    // id("com.google.gms.google-services") version "4.4.2" apply false
    // id("com.google.firebase.crashlytics") version "3.0.2" apply false

    // ✅ COMPOSE COMPILER PLUGIN - Direct application
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
}