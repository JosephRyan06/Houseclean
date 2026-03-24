package com.example.houseclean.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.houseclean.model.User
import com.example.houseclean.model.UserRepository
import com.google.firebase.auth.AuthResult

class LoginViewModel(private val repository: UserRepository = UserRepository()) : ViewModel() {

    private val _loginResult = MutableLiveData<Result<AuthResult>>()
    val loginResult: LiveData<Result<AuthResult>> = _loginResult

    private val _userRole = MutableLiveData<String?>()
    val userRole: LiveData<String?> = _userRole
    
    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    fun login(email: String, password: String, rememberMe: Boolean, sharedPreferences: SharedPreferences) {
        repository.login(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    handleRememberMe(email, password, rememberMe, sharedPreferences)
                    _loginResult.value = Result.success(task.result)
                    val uid = task.result.user?.uid ?: ""
                    getUserData(uid)
                } else {
                    _loginResult.value = Result.failure(task.exception ?: Exception("Login failed"))
                }
            }
    }

    private fun handleRememberMe(email: String, password: String, rememberMe: Boolean, sharedPreferences: SharedPreferences) {
        val editor = sharedPreferences.edit()
        if (rememberMe) {
            editor.putString("email", email)
            editor.putString("password", password)
            editor.putBoolean("rememberMe", true)
        } else {
            editor.clear()
        }
        editor.apply()
    }

    private fun getUserData(uid: String) {
        repository.getUserData(uid).get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(User::class.java)
                _userData.value = user
                _userRole.value = user?.role
            }
            .addOnFailureListener {
                _userData.value = null
                _userRole.value = null
            }
    }

    fun isEmailVerified(): Boolean {
        return repository.isEmailVerified()
    }
}
