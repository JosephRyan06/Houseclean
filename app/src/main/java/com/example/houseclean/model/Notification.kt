package com.example.houseclean.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Notification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val read: Boolean = false,
    val requestId: String = "",
    val source: String = "app",
    val type: String = "GENERAL",
    
    // Legacy/Internal tracking
    val userId: String = "", 
    val senderId: String = "",
    val senderName: String = "",
    val email: String = ""
)
