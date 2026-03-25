package com.example.houseclean.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.houseclean.R
import com.example.houseclean.model.Notification
import com.example.houseclean.model.ServiceRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class NotificationFragment : Fragment() {

    private val repository = ServiceRepository()
    private lateinit var layoutNotificationsContainer: LinearLayout
    private lateinit var tvNoNotifications: TextView
    private lateinit var btnClearAll: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layoutNotificationsContainer = view.findViewById(R.id.layoutNotificationsContainer)
        tvNoNotifications = view.findViewById(R.id.tvNoNotifications)
        btnClearAll = view.findViewById(R.id.btnClearAll)

        val currentUid = repository.getCurrentUserUid() ?: return
        
        btnClearAll.setOnClickListener {
            repository.clearAllNotifications(currentUid).addOnSuccessListener {
                Toast.makeText(context, "All notifications cleared", Toast.LENGTH_SHORT).show()
            }
        }

        fetchData()
    }

    private fun fetchData() {
        val currentUid = repository.getCurrentUserUid() ?: return
        
        repository.getUserNotifications(currentUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(userNotifSnapshot: DataSnapshot) {
                if (!isAdded) return
                val notificationsList = mutableListOf<Notification>()
                
                for (child in userNotifSnapshot.children) {
                    val n = child.getValue(Notification::class.java)
                    if (n != null) {
                        notificationsList.add(n.copy(id = child.key!!))
                    }
                }
                
                updateUI(notificationsList)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("NotificationFragment", "Error: ${error.message}")
            }
        })
    }

    private fun updateUI(notifications: List<Notification>) {
        if (!isAdded) return
        layoutNotificationsContainer.removeAllViews()
        
        val sortedList = notifications.sortedByDescending { it.createdAt }
        
        if (sortedList.isEmpty()) {
            tvNoNotifications.visibility = View.VISIBLE
            btnClearAll.visibility = View.GONE
        } else {
            tvNoNotifications.visibility = View.GONE
            btnClearAll.visibility = View.VISIBLE
            for (notification in sortedList) {
                addNotificationCard(notification)
            }
        }
    }

    private fun addNotificationCard(notification: Notification) {
        val view = layoutInflater.inflate(R.layout.item_notification, layoutNotificationsContainer, false)
        
        val tvTitle = view.findViewById<TextView>(R.id.tvNotificationTitle)
        val tvBody = view.findViewById<TextView>(R.id.tvNotificationBody)
        val tvDateTime = view.findViewById<TextView>(R.id.tvNotificationDateTime)
        val tvEmail = view.findViewById<TextView>(R.id.tvNotificationEmail)
        val tvSenderName = view.findViewById<TextView>(R.id.tvNotificationSenderName)
        val btnRemove = view.findViewById<MaterialButton>(R.id.btnRemoveNotification)
        
        val layoutRating = view.findViewById<LinearLayout>(R.id.layoutRatingSection)
        val ratingBar = view.findViewById<RatingBar>(R.id.ratingBar)
        val etFeedback = view.findViewById<EditText>(R.id.etFeedback)
        val btnSubmitRating = view.findViewById<MaterialButton>(R.id.btnSubmitRating)

        val currentUid = repository.getCurrentUserUid() ?: return

        tvTitle.text = notification.title.uppercase()
        tvBody.text = notification.body
        
        val sdf = SimpleDateFormat("EEE, MMM d, yyyy • h:mm a", Locale.getDefault())
        tvDateTime.text = sdf.format(Date(notification.createdAt))

        // Logic for email and sender name visibility
        if (notification.email.isEmpty() && notification.source.isEmpty()) {
            tvEmail.visibility = View.GONE
            tvSenderName.visibility = View.GONE
        } else {
            tvEmail.visibility = View.VISIBLE
            tvSenderName.visibility = View.VISIBLE
            tvEmail.text = notification.email
            tvSenderName.text = "- ${notification.senderName}"
        }

        if (notification.type == "RATING_REQUEST") {
            layoutRating.visibility = View.VISIBLE
            btnSubmitRating.setOnClickListener {
                val rating = ratingBar.rating
                val feedback = etFeedback.text.toString()
                
                if (rating == 0f) {
                    Toast.makeText(context, "Please select a rating", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                repository.updateUserRating(notification.senderId, rating, feedback).addOnSuccessListener {
                    Toast.makeText(context, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
                    repository.deleteNotification(currentUid, notification.id)
                }.addOnFailureListener { e ->
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            layoutRating.visibility = View.GONE
        }

        btnRemove.setOnClickListener {
            repository.deleteNotification(currentUid, notification.id)
        }
        
        if (!notification.read) {
            repository.markNotificationAsRead(currentUid, notification.id)
        }

        layoutNotificationsContainer.addView(view)
    }
}
