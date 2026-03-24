package com.example.houseclean.model

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    fun login(email: String, password: String): Task<AuthResult> {
        return auth.signInWithEmailAndPassword(email, password)
    }

    fun register(email: String, password: String): Task<AuthResult> {
        return auth.createUserWithEmailAndPassword(email, password)
    }

    fun sendEmailVerification(): Task<Void>? {
        return auth.currentUser?.sendEmailVerification()
    }

    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified ?: false
    }

    fun saveUser(user: User): Task<Void> {
        return database.child("Users").child(user.uid).setValue(user)
    }

    fun getUserRole(uid: String): Task<DataSnapshot> {
        return database.child("Users").child(uid).child("role").get()
    }

    fun getUserData(uid: String): DatabaseReference {
        return database.child("Users").child(uid)
    }

    fun getAllUsers(): DatabaseReference {
        return database.child("Users")
    }

    fun updateUserData(uid: String, updates: Map<String, Any>): Task<Void> {
        return database.child("Users").child(uid).updateChildren(updates)
    }

    fun checkEmailExists(email: String): Query {
        return database.child("Users").orderByChild("email").equalTo(email)
    }

    fun logout() {
        auth.signOut()
    }

    fun getCurrentUserUid(): String? {
        return auth.currentUser?.uid
    }
}