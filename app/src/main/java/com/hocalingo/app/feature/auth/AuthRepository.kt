package com.hocalingo.app.feature.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.hocalingo.app.core.base.AppError
import com.hocalingo.app.core.base.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for authentication operations
 * Handles Google Sign-In, Anonymous auth, and user state
 */
@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {

    /**
     * Current user as Flow
     */
    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    /**
     * Get current user synchronously
     */
    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    /**
     * Check if user is logged in
     */
    fun isUserAuthenticated(): Boolean = firebaseAuth.currentUser != null

    /**
     * Sign in with Google
     */
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user

            if (user != null) {
                Result.Success(user)
            } else {
                Result.Error(AppError.Unknown(Exception("User is null after sign in")))
            }
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Sign in anonymously (guest mode)
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInAnonymously().await()
            val user = authResult.user

            if (user != null) {
                Result.Success(user)
            } else {
                Result.Error(AppError.Unknown(Exception("Anonymous user is null")))
            }
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Link anonymous account with Google
     */
    suspend fun linkWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                return Result.Error(AppError.Unauthorized)
            }

            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = currentUser.linkWithCredential(credential).await()
            val user = authResult.user

            if (user != null) {
                Result.Success(user)
            } else {
                Result.Error(AppError.Unknown(Exception("User is null after linking")))
            }
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Sign out current user
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Delete user account
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
            if (user != null) {
                user.delete().await()
                Result.Success(Unit)
            } else {
                Result.Error(AppError.Unauthorized)
            }
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Get user display name
     */
    fun getUserDisplayName(): String? {
        return firebaseAuth.currentUser?.displayName
    }

    /**
     * Get user email
     */
    fun getUserEmail(): String? {
        return firebaseAuth.currentUser?.email
    }

    /**
     * Get user photo URL
     */
    fun getUserPhotoUrl(): String? {
        return firebaseAuth.currentUser?.photoUrl?.toString()
    }

    /**
     * Check if current user is anonymous
     */
    fun isAnonymousUser(): Boolean {
        return firebaseAuth.currentUser?.isAnonymous == true
    }
}