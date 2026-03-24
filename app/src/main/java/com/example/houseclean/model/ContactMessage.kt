package com.example.houseclean.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ContactMessage(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val message: String = "",
    val createdAt: Long = 0L,
    val status: String = "unread"
)
