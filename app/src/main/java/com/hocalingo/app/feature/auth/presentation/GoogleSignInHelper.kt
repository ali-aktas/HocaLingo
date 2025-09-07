package com.hocalingo.app.feature.auth.presentation

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.hocalingo.app.R

/**
 * Google Sign-In Helper for Compose
 */
class GoogleSignInHelper(
    private val context: Context,
    private val onSignInSuccess: (String) -> Unit,
    private val onSignInFailed: (Exception) -> Unit
) {

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        GoogleSignIn.getClient(context, gso)
    }

    fun signIn(launcher: ManagedActivityResultLauncher<Intent, ActivityResult>) {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    fun handleSignInResult(result: ActivityResult) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { token ->
                onSignInSuccess(token)
            } ?: onSignInFailed(Exception("ID Token is null"))
        } catch (e: ApiException) {
            onSignInFailed(e)
        }
    }

    fun signOut() {
        googleSignInClient.signOut()
    }
}

/**
 * Composable helper for Google Sign-In
 */
@Composable
fun rememberGoogleSignInHelper(
    onSignInSuccess: (String) -> Unit,
    onSignInFailed: (Exception) -> Unit
): Pair<GoogleSignInHelper, ManagedActivityResultLauncher<Intent, ActivityResult>> {
    val context = LocalContext.current

    val helper = remember {
        GoogleSignInHelper(
            context = context,
            onSignInSuccess = onSignInSuccess,
            onSignInFailed = onSignInFailed
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        helper.handleSignInResult(result)
    }

    return helper to launcher
}