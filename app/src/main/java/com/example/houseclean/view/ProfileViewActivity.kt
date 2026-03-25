package com.example.houseclean.view

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.houseclean.R
import com.example.houseclean.model.ServiceRepository
import com.example.houseclean.model.ServiceRequest
import com.example.houseclean.model.User
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class ProfileViewActivity : AppCompatActivity() {

    private val repository = ServiceRepository()
    private val auth = FirebaseAuth.getInstance()
    private var isReadOnly = false
    private var targetUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_view)

        isReadOnly = intent.getBooleanExtra("isReadOnly", false)
        targetUid = intent.getStringExtra("uid")

        // Set system bars color
        val navColor = Color.parseColor("#333A5B")
        window.statusBarColor = navColor
        window.navigationBarColor = navColor

        val mainView = findViewById<View>(R.id.profile_view_main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnEdit = findViewById<MaterialButton>(R.id.btnEditProfile)
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)

        if (isReadOnly) {
            btnEdit.visibility = View.GONE
            btnLogout.visibility = View.GONE
        }

        btnEdit.setOnClickListener {
            startActivity(Intent(this, ProfileEditActivity::class.java))
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        loadProfileData()
    }

    private fun loadProfileData() {
        val uid = targetUid ?: auth.currentUser?.uid ?: return
        
        repository.getUserData(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing) return
                val user = snapshot.getValue(User::class.java) ?: return
                
                findViewById<TextView>(R.id.tvProfileName).text = "${user.lastName}, ${user.firstName}"
                findViewById<TextView>(R.id.tvProfileRole).text = user.role
                findViewById<TextView>(R.id.tvProfileInitials).text = if (user.firstName.isNotEmpty() && user.lastName.isNotEmpty()) {
                    "${user.firstName[0]}${user.lastName[0]}".uppercase()
                } else "HK"
                
                findViewById<TextView>(R.id.tvProfileEmail).text = "Email: ${user.email}"
                findViewById<TextView>(R.id.tvProfilePhone).text = "Phone: ${user.phone}"
                findViewById<TextView>(R.id.tvProfileJoined).text = "Joined: ${user.joined}"
                findViewById<TextView>(R.id.tvProfileAddress).text = "Address: ${user.street}, ${user.barangay}, ${user.landmark}"
                
                val profileBg = findViewById<View>(R.id.profileBg)
                val colors = listOf("#E57373", "#F06292", "#BA68C8", "#9575CD", "#7986CB", "#64B5F6", "#4FC3F7", "#4DD0E1", "#4DB6AC", "#81C784", "#AED581", "#DCE775", "#FFF176", "#FFD54F", "#FFB74D", "#FF8A65")
                val colorIndex = Math.abs(uid.hashCode()) % colors.size
                (profileBg.background as? GradientDrawable)?.setColor(Color.parseColor(colors[colorIndex]))

                // Handle Housekeeper specific visibility and data
                if (user.role == "Housekeeper") {
                    findViewById<TextView>(R.id.tvProfileRatings).apply {
                        visibility = View.VISIBLE
                        text = "Ratings: ${user.getRatingsDisplay()}"
                    }
                    findViewById<TextView>(R.id.tvProfileAvailability).apply {
                        visibility = View.VISIBLE
                        text = "Availability: ${user.availability ?: "Not specified"}"
                    }
                    findViewById<TextView>(R.id.tvProfileSkills).apply {
                        visibility = View.VISIBLE
                        text = "Skills: ${user.getSkillsDisplay()}"
                    }
                    findViewById<TextView>(R.id.tvProfileExperience).apply {
                        visibility = View.VISIBLE
                        text = "Experience: ${user.experience.ifBlank { "Not specified" }}"
                    }
                    findViewById<TextView>(R.id.tvSectionTitle).text = "TASKS"
                    findViewById<ImageView>(R.id.ivSectionIcon).apply {
                        setImageResource(R.drawable.task_image)
                        colorFilter = null // Ensure no tint is applied
                    }
                } else {
                    findViewById<TextView>(R.id.tvProfileRatings).visibility = View.GONE
                    findViewById<TextView>(R.id.tvProfileAvailability).visibility = View.GONE
                    findViewById<TextView>(R.id.tvProfileSkills).visibility = View.GONE
                    findViewById<TextView>(R.id.tvProfileExperience).visibility = View.GONE
                    findViewById<TextView>(R.id.tvSectionTitle).text = "REQUESTS"
                    findViewById<ImageView>(R.id.ivSectionIcon).apply {
                        setImageResource(R.drawable.request_image)
                        colorFilter = null // Ensure no tint is applied
                    }
                }

                if (!isReadOnly) {
                    fetchStats(user.role, uid)
                } else {
                    findViewById<View>(R.id.tvSectionTitle).visibility = View.GONE
                    findViewById<View>(R.id.ivSectionIcon).visibility = View.GONE
                    findViewById<View>(R.id.cardStats).visibility = View.GONE
                    findViewById<View>(R.id.tvPaymentSectionTitle).visibility = View.GONE
                    findViewById<View>(R.id.cardPayments).visibility = View.GONE
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchStats(role: String, uid: String) {
        val query = if (role == "Housekeeper") {
            repository.getHousekeeperRequests(uid)
        } else {
            repository.getHouseholderRequests(uid)
        }

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalReq = 0
                var confirmed = 0
                var pending = 0
                var declined = 0
                
                var totalPrice = 0.0
                var paidCount = 0
                var waitingCount = 0
                var overdueCount = 0

                for (child in snapshot.children) {
                    val req = child.getValue(ServiceRequest::class.java) ?: continue
                    totalReq++
                    when(req.status) {
                        "ACCEPTED" -> confirmed++
                        "PENDING" -> pending++
                        "DECLINED" -> declined++
                    }

                    totalPrice += req.totalPrice
                    when(req.paymentStatus) {
                        "PAID" -> paidCount++
                        "PENDING" -> waitingCount++
                        "OVERDUE" -> overdueCount++
                    }
                }

                findViewById<TextView>(R.id.tvTotalRequests).text = "Total: $totalReq"
                
                if (role == "Housekeeper") {
                    var hkCompleted = 0
                    var hkFailed = 0
                    var hkOnGoing = 0
                    for (child in snapshot.children) {
                        val req = child.getValue(ServiceRequest::class.java) ?: continue
                        when(req.status) {
                            "ACCEPTED" -> hkOnGoing++
                            "COMPLETED" -> hkCompleted++
                            "DECLINED" -> hkFailed++
                        }
                    }
                    findViewById<TextView>(R.id.tvConfirmedRequests).text = "On-Going: $hkOnGoing"
                    findViewById<TextView>(R.id.tvPendingRequests).text = "Completed: $hkCompleted"
                    findViewById<TextView>(R.id.tvDeclinedRequests).text = "Failed: $hkFailed"
                } else {
                    findViewById<TextView>(R.id.tvConfirmedRequests).text = "Confirmed: $confirmed"
                    findViewById<TextView>(R.id.tvPendingRequests).text = "Pending: $pending"
                    findViewById<TextView>(R.id.tvDeclinedRequests).text = "Declined: $declined"
                }

                findViewById<TextView>(R.id.tvTotalPayments).apply {
                    text = "Total: PHP ${String.format(Locale.getDefault(), "%,.0f", totalPrice)}"
                    visibility = if (role == "Householder") View.VISIBLE else View.GONE
                }
                findViewById<TextView>(R.id.tvPaidPayments).apply {
                    text = "Paid: $paidCount"
                    visibility = if (role == "Householder") View.VISIBLE else View.GONE
                }
                findViewById<TextView>(R.id.tvWaitingPayments).apply {
                    text = "Waiting: $waitingCount"
                    visibility = if (role == "Householder") View.VISIBLE else View.GONE
                }
                findViewById<TextView>(R.id.tvOverduePayments).apply {
                    text = "Overdue: $overdueCount"
                    visibility = if (role == "Householder") View.VISIBLE else View.GONE
                }
                
                findViewById<View>(R.id.tvPaymentSectionTitle).visibility = if (role == "Householder") View.VISIBLE else View.GONE
                findViewById<View>(R.id.cardPayments).visibility = if (role == "Householder") View.VISIBLE else View.GONE
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
