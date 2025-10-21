# =========================================
# HocaLingo - ProGuard Rules for Production
# =========================================
# Keep this file SAFE - it protects against crashes after minification

# =========================================
# GENERAL ANDROID RULES
# =========================================
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn javax.annotation.**
-keepattributes *Annotation*
-keep class * extends java.lang.Exception
-keepattributes Signature
-keepattributes Exceptions
-keepattributes SourceFile,LineNumberTable

# =========================================
# KOTLIN
# =========================================
-dontwarn kotlin.**
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.hocalingo.app.**$$serializer { *; }
-keepclassmembers class com.hocalingo.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.hocalingo.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# =========================================
# JETPACK COMPOSE
# =========================================
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Compose Runtime
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }

# =========================================
# HILT / DAGGER
# =========================================
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.Module class * { *; }
-keep @dagger.Provides class * { *; }
-keep @javax.inject.Inject class * { *; }

# Hilt ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class androidx.hilt.** { *; }

# =========================================
# ROOM DATABASE
# =========================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**

# Room entities
-keep class com.hocalingo.app.database.entity.** { *; }
-keep class com.hocalingo.app.database.dao.** { *; }

# =========================================
# RETROFIT & OKHTTP
# =========================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.internal.platform.**

# =========================================
# KOTLINX SERIALIZATION
# =========================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serializers
-keep,includedescriptorclasses class **$$serializer { *; }
-keepclassmembers class * {
    *** Companion;
}

# =========================================
# FIREBASE
# =========================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firebase Crashlytics
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }

# Firestore
-keep class com.google.firebase.firestore.** { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <methods>;
}

# =========================================
# REVENUECAT
# =========================================
-keep class com.revenuecat.purchases.** { *; }
-dontwarn com.revenuecat.purchases.**
-keepattributes *Annotation*

# =========================================
# ADMOB
# =========================================
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
-keep public class com.google.ads.** { *; }

# =========================================
# COIL (Image Loading)
# =========================================
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# =========================================
# WORKMANAGER
# =========================================
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# =========================================
# NAVIGATION COMPONENT
# =========================================
-keep class androidx.navigation.** { *; }
-keep interface androidx.navigation.** { *; }

# =========================================
# YOUR APP MODELS & DATA CLASSES
# =========================================
# Keep all data classes
-keep class com.hocalingo.app.data.** { *; }
-keep class com.hocalingo.app.domain.model.** { *; }

# Keep sealed classes and interfaces
-keep class * extends com.hocalingo.app.** {
    *;
}

# =========================================
# CUSTOM APP RULES
# =========================================
# Keep BroadcastReceivers (for AlarmManager)
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class com.hocalingo.app.core.notification.DailyNotificationReceiver { *; }

# Keep Application class
-keep class com.hocalingo.app.HocaLingoApplication { *; }

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# =========================================
# DEBUG / LOGGING (Remove in production)
# =========================================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# =========================================
# OPTIMIZATION FLAGS
# =========================================
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# =========================================
# WARNING SUPPRESSIONS (Common libraries)
# =========================================
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn java.awt.**
-dontwarn javax.swing.**