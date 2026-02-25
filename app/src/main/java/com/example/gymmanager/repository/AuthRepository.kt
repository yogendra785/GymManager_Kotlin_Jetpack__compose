package com.example.gymmanager.repository

import com.google.firebase.auth.FirebaseAuth

class AuthRepository {
    // Get the Firebase Auth instance
    private val auth = FirebaseAuth.getInstance()

    // Login function using Callbacks (onSuccess and onError)
    fun loginUser(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error -> onError(error.message ?: "Login Failed") }
    }

    // Register function for new gym owners
    fun registerUser(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error -> onError(error.message ?: "Registration Failed") }
    }

    // Check if user is already logged in so they don't have to login every time
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}