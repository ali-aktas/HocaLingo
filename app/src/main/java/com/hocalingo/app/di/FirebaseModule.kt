package com.hocalingo.app.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Firebase dependencies
 *
 * Package: app/src/main/java/com/hocalingo/app/di/
 *
 * ✅ FIXED: Default Firestore database kullanıyoruz (ÜCRETSİZ!)
 * ❌ ÖNCE: Custom database "hocalingodatabase" (PAHALIYDI!)
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        // ✅ FIXED: Default database kullan (Spark plan'da çalışır!)
        return FirebaseFirestore.getInstance()

        // ❌ ÖNCE BÖYLEYDI (Custom database - Blaze plan gerekiyor!)
        // return Firebase.firestore(FirebaseApp.getInstance(), "hocalingodatabase")
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }
}