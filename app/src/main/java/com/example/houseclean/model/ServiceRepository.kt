package com.example.houseclean.model

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ServiceRepository {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun getAllHousekeepers(): Query {
        return database.child("Users").orderByChild("role").equalTo("Housekeeper")
    }

    fun submitServiceRequest(request: ServiceRequest): Task<Void> {
        val requestId = database.child("ServiceRequests").push().key ?: ""
        val finalRequest = request.copy(requestId = requestId)
        return database.child("ServiceRequests").child(requestId).setValue(finalRequest)
    }

    fun getHouseholderRequests(householderId: String): Query {
        return database.child("ServiceRequests").orderByChild("householderId").equalTo(householderId)
    }

    fun getHousekeeperRequests(housekeeperId: String): Query {
        return database.child("ServiceRequests").orderByChild("housekeeperId").equalTo(housekeeperId)
    }

    fun updateRequestStatus(requestId: String, newStatus: String): Task<Void> {
        val updates = hashMapOf<String, Any>(
            "status" to newStatus,
            "updatedAt" to System.currentTimeMillis()
        )
        
        if (newStatus == "CONFIRMED") {
            updates["paymentStatus"] = "PENDING"
        }
        
        return database.child("ServiceRequests").child(requestId).updateChildren(updates)
    }
    
    fun markStaffArrived(requestId: String): Task<Void> {
        val updates = hashMapOf<String, Any>(
            "staffWaitingConfirmation" to true,
            "updatedAt" to System.currentTimeMillis()
        )
        return database.child("ServiceRequests").child(requestId).updateChildren(updates)
    }

    fun confirmStaffArrival(requestId: String, staffId: String, staffName: String): Task<Void> {
        val now = System.currentTimeMillis()
        val updates = hashMapOf<String, Any>(
            "customerArrivalConfirmed" to true,
            "customerArrivalConfirmedAt" to now,
            "staffArrived" to true,
            "staffArrivedAt" to now,
            "staffArrivedById" to staffId,
            "staffArrivedByName" to staffName,
            "staffWaitingConfirmation" to false,
            "updatedAt" to now
        )
        return database.child("ServiceRequests").child(requestId).updateChildren(updates)
    }

    fun confirmPayment(requestId: String): Task<Void> {
        val updates = hashMapOf<String, Any>(
            "paymentStatus" to "PAID",
            "updatedAt" to System.currentTimeMillis()
        )
        return database.child("ServiceRequests").child(requestId).updateChildren(updates)
    }

    fun submitPaymentInfo(requestId: String, transactionId: String, method: String): Task<Void> {
        val now = System.currentTimeMillis()
        val updates = hashMapOf<String, Any>(
            "paymentStatus" to "PAID",
            "paidAt" to now,
            "paidVia" to method,
            "paymentTransactionId" to transactionId,
            "photosUploadStatus" to "NONE",
            "updatedAt" to now
        )
        return database.child("ServiceRequests").child(requestId).updateChildren(updates)
    }

    fun sendNotification(notification: Notification): Task<Void> {
        val userId = notification.userId
        val id = database.child("UserNotifications").child(userId).push().key ?: ""
        return database.child("UserNotifications").child(userId).child(id).setValue(notification.copy(id = id))
    }

    fun getUserNotifications(userId: String): Query {
        return database.child("UserNotifications").child(userId).orderByChild("createdAt")
    }

    fun deleteNotification(userId: String, notificationId: String): Task<Void> {
        return database.child("UserNotifications").child(userId).child(notificationId).removeValue()
    }

    fun markNotificationAsRead(userId: String, notificationId: String): Task<Void> {
        return database.child("UserNotifications").child(userId).child(notificationId).child("read").setValue(true)
    }

    fun clearAllNotifications(userId: String): Task<Void> {
        return database.child("UserNotifications").child(userId).removeValue()
    }

    fun getContactMessages(): Query {
        return database.child("ContactMessages").orderByChild("createdAt")
    }

    fun markContactMessageAsRead(id: String): Task<Void> {
        return database.child("ContactMessages").child(id).child("status").setValue("read")
    }

    fun deleteContactMessage(id: String): Task<Void> {
        return database.child("ContactMessages").child(id).removeValue()
    }
    
    fun updateUserRating(housekeeperId: String, rating: Float, feedback: String): Task<Void> {
        val userRef = database.child("Users").child(housekeeperId)
        val tcs = com.google.android.gms.tasks.TaskCompletionSource<Void>()
        
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java) ?: return
                val newCount = user.ratingCount + 1
                val newSum = user.ratingSum + rating
                val newAverage = newSum / newCount
                
                val updates = hashMapOf<String, Any>(
                    "ratingCount" to newCount,
                    "ratingSum" to newSum,
                    "ratingAverage" to newAverage,
                    "latestReview" to feedback,
                    "latestReviewAt" to System.currentTimeMillis()
                )
                
                userRef.updateChildren(updates).addOnCompleteListener { task ->
                    if (task.isSuccessful) tcs.setResult(null)
                    else tcs.setException(task.exception!!)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                tcs.setException(error.toException())
            }
        })
        return tcs.task
    }
    
    fun getCurrentUserUid(): String? {
        return auth.currentUser?.uid
    }

    fun getUserData(uid: String): DatabaseReference {
        return database.child("Users").child(uid)
    }
}
