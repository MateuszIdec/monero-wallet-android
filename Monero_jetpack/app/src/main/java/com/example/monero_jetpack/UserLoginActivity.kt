package com.example.monero_jetpack

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class UserLoginActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager // Use androidx CredentialManager
    private var user by mutableStateOf<FirebaseUser?>(null)

    companion object {
        private const val TAG = "UserLoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        credentialManager = CredentialManager.create(this) // Correct usage for androidx

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                LoginScreen()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        user = auth.currentUser
        updateUI(user)
    }

    private fun launchCredentialManager() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId("73381648475-kb5r09lsted49th7kg0m3vbbgstkbs8m.apps.googleusercontent.com") // Replace with your web client ID
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(true)
            .setNonce("YOUR_NONCE") // Replace with your nonce
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = baseContext
                )
                handleSignIn(result.credential)
            } catch (e: GetCredentialException) {
                handleFailure(e)
            }
        }
    }

    private fun handleSignIn(credential: androidx.credentials.Credential) {
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
        } else {
            Log.w(TAG, "Credential is not of type Google ID!")
        }
    }

    private fun handleFailure(e: GetCredentialException) {
        Log.e(TAG, "Couldn't retrieve user's credentials: ${e.localizedMessage}")
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    user = auth.currentUser
                    Log.d(TAG, "signInWithCredential:success")
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }
                updateUI(user)
            }
    }

    private fun signOut() {
        auth.signOut()
        lifecycleScope.launch {
            try {
                val clearRequest = ClearCredentialStateRequest()
                credentialManager.clearCredentialState(clearRequest)
                user = null
            } catch (e: ClearCredentialException) {
                Log.e(TAG, "Couldn't clear user credentials: ${e.localizedMessage}")
            }
            updateUI(user)
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        this.user = user
    }

    @Composable
    fun LoginScreen() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = if (user != null) "Welcome, ${user?.displayName ?: "User"}" else "Please Sign In")

            Spacer(modifier = Modifier.height(20.dp))

            if (user == null) {
                Button(onClick = { launchCredentialManager() }) {
                    Text("Sign In with Google")
                }
            } else {
                Button(onClick = { signOut() }) {
                    Text("Sign Out")
                }
            }
        }
    }
}