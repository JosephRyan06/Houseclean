package com.example.houseclean.view

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.houseclean.R
import com.example.houseclean.model.ServiceRepository
import com.example.houseclean.model.ServiceRequest
import com.example.houseclean.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class ServiceDetailsActivity : AppCompatActivity() {

    private val repository = ServiceRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_service_details)

        // Set system bars color
        val navColor = Color.parseColor("#333A5B")
        window.statusBarColor = navColor
        window.navigationBarColor = navColor

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val requestId = intent.getStringExtra("requestId")
        if (requestId == null) {
            Toast.makeText(this, "Error: Request ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fetchRequestDetails(requestId)
    }

    private fun fetchRequestDetails(requestId: String) {
        com.google.firebase.database.FirebaseDatabase.getInstance().reference
            .child("ServiceRequests").child(requestId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val request = snapshot.getValue(ServiceRequest::class.java)
                    if (request != null) {
                        displayDetails(request)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ServiceDetailsActivity, "Error loading details", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun displayDetails(request: ServiceRequest) {
        // 1. Overview Information
        findViewById<TextView>(R.id.tvDetailServiceType).text = request.serviceType
        findViewById<TextView>(R.id.tvDetailStatus).text = "Status: ${request.status}"
        findViewById<TextView>(R.id.tvDetailTotalPrice).text = "Total Price: ₱ ${String.format("%,.0f", request.totalPrice)}"
        
        var formattedDate = ""
        var formattedTime = ""
        try {
            val inputDate: Date? = if (request.startDate.contains("T")) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(request.startDate)
            } else {
                SimpleDateFormat("d/M/yyyy", Locale.getDefault()).parse(request.startDate)
            }
            formattedDate = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(inputDate!!)
            formattedTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(inputDate)
        } catch (e: Exception) {
            formattedDate = request.startDate
        }
        
        findViewById<TextView>(R.id.tvDetailSchedule).text = formattedDate
        findViewById<TextView>(R.id.tvDetailTime).text = formattedTime

        // 2. Extra Details
        findViewById<TextView>(R.id.tvDetailPaymentMethod).text = "Payment Method: ${request.paymentMethod}"
        findViewById<TextView>(R.id.tvDetailNotes).text = "Notes: ${if (request.notes.isBlank()) "None" else request.notes}"

        // 3. Populate Householder Card
        repository.getUserData(request.householderId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                val uid = snapshot.key ?: ""
                user?.let { 
                    val finalUser = if (it.uid.isEmpty()) it.copy(uid = uid) else it
                    populateUserCard(findViewById(R.id.cardHouseholder), finalUser, true) 
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 4. Populate Housekeeper Card
        repository.getUserData(request.housekeeperId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                val uid = snapshot.key ?: ""
                user?.let { 
                    val finalUser = if (it.uid.isEmpty()) it.copy(uid = uid) else it
                    populateUserCard(findViewById(R.id.cardHousekeeper), finalUser, false) 
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun populateUserCard(cardView: View, user: User, isHouseholder: Boolean) {
        val tvName = cardView.findViewById<TextView>(R.id.tvHKName)
        val tvEmail = cardView.findViewById<TextView>(R.id.tvUserEmail)
        val tvStatusValue = cardView.findViewById<TextView>(R.id.tvStatusValue)
        val tvJoinedValue = cardView.findViewById<TextView>(R.id.tvJoinedValue)
        val tvAvailableLabel = cardView.findViewById<TextView>(R.id.tvAvailableLabel)
        val tvAvailableValue = cardView.findViewById<TextView>(R.id.tvAvailableValue)
        val tvInitials = cardView.findViewById<TextView>(R.id.tvProfileInitials)
        val profileBg = cardView.findViewById<View>(R.id.profileBg)
        val statusStroke = cardView.findViewById<View>(R.id.viewStatusStroke)
        val rbRating = cardView.findViewById<RatingBar>(R.id.rbHKRating)
        val tvRatingCount = cardView.findViewById<TextView>(R.id.tvHKRatingsCount)
        val tvPhone = cardView.findViewById<TextView>(R.id.tvHKPhone)

        tvName.text = user.getDisplayName()
        tvEmail.visibility = View.VISIBLE
        tvEmail.text = user.email
        tvPhone.text = user.phone.ifEmpty { user.contact }
        
        tvStatusValue.text = user.status
        tvJoinedValue.text = user.joined
        
        if (isHouseholder) {
            rbRating.visibility = View.GONE
            tvRatingCount.visibility = View.GONE
            tvAvailableLabel.text = "Created At: "
            tvAvailableValue.text = user.joined
        } else {
            rbRating.visibility = View.VISIBLE
            tvRatingCount.visibility = View.VISIBLE
            tvRatingCount.text = user.getRatingsDisplay()
            rbRating.rating = user.ratingAverage.toFloat()
            
            tvAvailableLabel.text = "Available: "
            tvAvailableValue.text = when (val avail = user.availability) {
                is String -> avail
                is List<*> -> avail.joinToString(", ")
                else -> "Not specified"
            }
        }

        // Initials and Color
        val initials = if (user.firstName.isNotEmpty() && user.lastName.isNotEmpty()) {
            "${user.firstName.take(1)}${user.lastName.take(1)}".uppercase()
        } else user.getDisplayName().take(2).uppercase()
        tvInitials.text = initials
        
        val colors = listOf("#E57373", "#F06292", "#BA68C8", "#9575CD", "#7986CB", "#64B5F6", "#4FC3F7", "#4DD0E1", "#4DB6AC", "#81C784", "#AED581", "#DCE775", "#FFF176", "#FFD54F", "#FFB74D", "#FF8A65")
        val colorIndex = Math.abs(user.uid.hashCode()) % colors.size
        (profileBg.background as? GradientDrawable)?.setColor(Color.parseColor(colors[colorIndex]))
        
        val strokeColor = if (user.isOnline) Color.parseColor("#0DFF00") else Color.parseColor("#888888")
        (statusStroke.background as? GradientDrawable)?.setStroke(6, strokeColor)

        // Set up "Check Profile" button
        cardView.findViewById<View>(R.id.btnProfileDetails).setOnClickListener {
            val intent = Intent(this, ProfileViewActivity::class.java)
            intent.putExtra("uid", user.uid)
            intent.putExtra("isReadOnly", true)
            startActivity(intent)
        }
    }
}
