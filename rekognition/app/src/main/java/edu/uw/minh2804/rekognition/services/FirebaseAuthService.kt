/**
 * Shane Fretwell: I figured out how and when firebase auth is necessary, and tom refactored my
 * implementation into this Singleton
 */

package edu.uw.minh2804.rekognition.services

import android.util.Log
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// This service is responsible for handling all of the authentication with Firebase, required when
// using Firebase functions
object FirebaseAuthService {
    private val AUTH = Firebase.auth

    suspend fun signIn(): AuthResult {
        Log.v("FirebaseAuthService", "Logging from signIn()")
        return suspendCoroutine { continuation ->
            AUTH.signInAnonymously()
                .addOnSuccessListener {
                    Log.v("FirebaseAuthService", "onSuccess: $it")
                    continuation.resume(it)
                }
                .addOnFailureListener { exception ->
                    Log.v("FirebaseAuthService", exception.toString())
                    continuation.resumeWithException(exception)
                }
        }
    }

    fun isAuthenticated(): Boolean {
        Log.v("FirebaseAuthService", "Logging from isAuthenticated()")
        return AUTH.currentUser != null
    }
}