package com.example.gymmanager.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.gymmanager.repository.AuthRepository

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    // States that our UI will observe
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    fun login(email: String, password: String, navigateToDashboard: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Email and Password cannot be empty"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null // Clear old errors

        repository.loginUser(email, password,
            onSuccess = {
                _isLoading.value = false
                navigateToDashboard()
            },
            onError = { error ->
                _isLoading.value = false
                _errorMessage.value = error
            }
        )
    }

    fun register(email: String, password: String, navigateToDashboard: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Email and Password cannot be empty"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        repository.registerUser(email, password,
            onSuccess = {
                _isLoading.value = false
                navigateToDashboard()
            },
            onError = { error ->
                _isLoading.value = false
                _errorMessage.value = error
            }
        )
    }
}