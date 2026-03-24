package com.example.houseclean.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class PriceRange(
    val min: Double = 0.0,
    val max: Double = 0.0
)

@IgnoreExtraProperties
data class ServiceRequest(
    val requestId: String = "",
    val householderId: String = "",
    val householderName: String = "",
    val customerEmail: String = "",
    val housekeeperId: String = "",
    val housekeeperName: String = "",
    val serviceType: String = "",
    val durationHours: Int = 0,
    val totalPrice: Double = 0.0,
    val priceRange: PriceRange? = null,
    val startDate: String = "", // Combined format: YYYY-MM-DDTHH:mm
    val status: String = "PENDING", 
    var paymentStatus: String = "NONE",
    val paymentMethod: String = "", 
    val cashOnHandConfirmed: Boolean = false,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // Attendance fields
    val customerArrivalConfirmed: Boolean = false,
    val customerArrivalConfirmedAt: Long = 0L,
    val staffArrived: Boolean = false,
    val staffArrivedAt: Long = 0L,
    val staffArrivedById: String = "",
    val staffArrivedByName: String = "",
    val staffWaitingConfirmation: Boolean = false,

    // Payment details
    val paidAt: Long = 0L,
    val paidVia: String = "",
    val paymentTransactionId: String = "",
    val photosUploadStatus: String = "NONE"
)
