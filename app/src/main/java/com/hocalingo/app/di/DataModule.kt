package com.hocalingo.app.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.hocalingo.app.data.FirebaseWordPackageDataSource
import com.hocalingo.app.data.WordPackageRepository
import com.hocalingo.app.database.HocaLingoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DataModule - Hilt Dependency Injection
 *
 * Package: app/src/main/java/com/hocalingo/app/di/
 *
 * Firebase Word Package sisteminin DI modülü
 * - FirebaseWordPackageDataSource
 * - WordPackageRepository
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideFirebaseWordPackageDataSource(
        firestore: FirebaseFirestore,
        storage: FirebaseStorage
    ): FirebaseWordPackageDataSource {
        return FirebaseWordPackageDataSource(
            firestore = firestore,
            storage = storage
        )
    }

    @Provides
    @Singleton
    fun provideWordPackageRepository(
        firebaseDataSource: FirebaseWordPackageDataSource,
        database: HocaLingoDatabase
    ): WordPackageRepository {
        return WordPackageRepository(
            firebaseDataSource = firebaseDataSource,
            database = database
        )
    }
}