package com.example.houseclean.model

import com.google.firebase.database.IgnoreExtraProperties
import java.util.Locale

@IgnoreExtraProperties
data class User(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    var fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val contact: String = "", // Added contact field
    val barangay: String = "",
    val street: String = "",
    val landmark: String = "",
    val role: String = "Householder",
    val isOnline: Boolean = false,
    val needsVerification: Boolean = false,
    
    // Housekeeper specific fields
    val joined: String = "",
    val status: String = "Available",
    val availability: Any? = null,
    val skills: Any? = null,
    val otherSkill: String = "",
    val experience: String = "",
    
    // Rating fields
    val rating: Double = 0.0,
    val ratingAverage: Double = 0.0,
    val ratingCount: Int = 0,
    val ratingSum: Double = 0.0,
    val latestReview: String = "",
    val latestReviewAt: Long = 0L,
    
    val trainingCompleted: Any? = null,
    val emergency: Any? = null
) {
    fun getDisplayName(): String {
        return when {
            fullName.isNotBlank() -> fullName
            firstName.isNotBlank() || lastName.isNotBlank() -> "$firstName $lastName".trim()
            else -> "Unknown User"
        }
    }

    fun getSkillsDisplay(): String {
        return when (skills) {
            is List<*> -> skills.joinToString(", ")
            is String -> if (skills.isNotBlank()) skills else "No skills listed"
            else -> "No skills listed"
        }
    }

    fun getRatingsDisplay(): String {
        return if (ratingCount > 0) {
            String.format(Locale.getDefault(), "%.1f (%d reviews)", ratingAverage, ratingCount)
        } else {
            "No ratings yet"
        }
    }
}
