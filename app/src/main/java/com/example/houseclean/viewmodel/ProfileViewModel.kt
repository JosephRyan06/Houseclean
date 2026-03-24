package com.example.houseclean.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.houseclean.model.User
import com.example.houseclean.model.UserRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class ProfileViewModel(private val repository: UserRepository = UserRepository()) : ViewModel() {

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    private var userListener: ValueEventListener? = null

    fun fetchUserData() {
        val uid = repository.getCurrentUserUid() ?: return
        
        userListener = repository.getUserData(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                _userData.value = user
            }

            override fun onCancelled(error: DatabaseError) {
                _userData.value = null
            }
        })
    }

    fun updateProfile(firstName: String, lastName: String, barangay: String, street: String, landmark: String) {
        val uid = repository.getCurrentUserUid() ?: return
        val updates = mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "barangay" to barangay,
            "street" to street,
            "landmark" to landmark
        )

        repository.updateUserData(uid, updates)
            .addOnSuccessListener {
                _updateResult.value = Result.success(Unit)
            }
            .addOnFailureListener {
                _updateResult.value = Result.failure(it)
            }
    }

    fun updateAccountInfo(email: String, phone: String) {
        val uid = repository.getCurrentUserUid() ?: return
        val updates = mapOf(
            "email" to email,
            "phone" to phone
        )

        repository.updateUserData(uid, updates)
            .addOnSuccessListener {
                _updateResult.value = Result.success(Unit)
            }
            .addOnFailureListener {
                _updateResult.value = Result.failure(it)
            }
    }

    fun logout() {
        repository.logout()
    }

    override fun onCleared() {
        super.onCleared()
        val uid = repository.getCurrentUserUid()
        if (uid != null && userListener != null) {
            repository.getUserData(uid).removeEventListener(userListener!!)
        }
    }
}