package com.example.houseclean.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.houseclean.model.User
import com.example.houseclean.model.UserRepository
import com.google.firebase.auth.AuthResult
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class RegisterViewModel(private val repository: UserRepository = UserRepository()) : ViewModel() {

    private val _registrationResult = MutableLiveData<Result<AuthResult>>()
    val registrationResult: LiveData<Result<AuthResult>> = _registrationResult

    private val _saveUserResult = MutableLiveData<Result<Unit>>()
    val saveUserResult: LiveData<Result<Unit>> = _saveUserResult

    private val _emailExists = MutableLiveData<Boolean>()
    val emailExists: LiveData<Boolean> = _emailExists
    
    private val _verificationEmailSent = MutableLiveData<Result<Unit>>()
    val verificationEmailSent: LiveData<Result<Unit>> = _verificationEmailSent

    fun checkEmailExists(email: String) {
        repository.checkEmailExists(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _emailExists.value = snapshot.exists()
            }

            override fun onCancelled(error: DatabaseError) {
                _emailExists.value = false
            }
        })
    }

    fun register(user: User, password: String) {
        repository.register(user.email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _registrationResult.value = Result.success(task.result)
                    val uid = task.result.user?.uid ?: ""
                    // Set needsVerification to true for new users
                    saveUser(user.copy(uid = uid, needsVerification = true))
                    sendVerificationEmail()
                } else {
                    _registrationResult.value = Result.failure(task.exception ?: Exception("Registration failed"))
                }
            }
    }

    private fun sendVerificationEmail() {
        repository.sendEmailVerification()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                _verificationEmailSent.value = Result.success(Unit)
            } else {
                _verificationEmailSent.value = Result.failure(task.exception ?: Exception("Failed to send verification email"))
            }
        }
    }
    
    fun resendVerificationEmail() {
        sendVerificationEmail()
    }

    fun checkEmailVerificationStatus() : Boolean {
        return repository.isEmailVerified()
    }

    private fun saveUser(user: User) {
        repository.saveUser(user)
            .addOnSuccessListener {
                _saveUserResult.value = Result.success(Unit)
            }
            .addOnFailureListener {
                _saveUserResult.value = Result.failure(it)
            }
    }
}
