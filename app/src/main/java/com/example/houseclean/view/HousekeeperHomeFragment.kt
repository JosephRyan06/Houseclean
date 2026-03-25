package com.example.houseclean.view

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.houseclean.R
import com.example.houseclean.model.Notification
import com.example.houseclean.model.ServiceRepository
import com.example.houseclean.model.ServiceRequest
import com.example.houseclean.model.User
import com.example.houseclean.model.UserRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class HousekeeperHomeFragment : Fragment() {

    private val repository = ServiceRepository()
    private val userRepository = UserRepository()
    private lateinit var layoutRequests: LinearLayout
    private lateinit var layoutPayments: LinearLayout
    private lateinit var tvNoRequests: TextView
    private lateinit var tvNoPayments: TextView
    private lateinit var hsvRequests: View
    private lateinit var hsvPayments: View
    private lateinit var tvWelcomeName: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_housekeeper_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layoutRequests = view.findViewById(R.id.layoutRequestsContainer)
        layoutPayments = view.findViewById(R.id.layoutPaymentsContainer)
        tvNoRequests = view.findViewById(R.id.tvNoRequests)
        tvNoPayments = view.findViewById(R.id.tvNoPayments)
        hsvRequests = view.findViewById(R.id.hsvRequests)
        hsvPayments = view.findViewById(R.id.hsvPayments)
        tvWelcomeName = view.findViewById(R.id.tvWelcomeName)

        setupWelcomeText()
        fetchData()
    }

    private fun setupWelcomeText() {
        val currentUid = repository.getCurrentUserUid() ?: return
        userRepository.getUserData(currentUid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    tvWelcomeName.text = "Welcome ${it.lastName}, ${it.firstName}!"
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchData() {
        val currentUid = repository.getCurrentUserUid() ?: return
        
        repository.getHousekeeperRequests(currentUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                layoutRequests.removeAllViews()
                layoutPayments.removeAllViews()

                var hasRequests = false
                var hasPayments = false
                
                for (child in snapshot.children) {
                    val request = child.getValue(ServiceRequest::class.java) ?: continue
                    val requestId = child.key ?: ""
                    val finalRequest = request.copy(requestId = requestId)
                    
                    val status = finalRequest.status
                    // Requests Section: PENDING, PENDING_PAYMENT, or RESERVED (but not yet accepted)
                    if (status == "PENDING" || status == "PENDING_PAYMENT" || status == "RESERVED") {
                        addRequestCard(finalRequest)
                        hasRequests = true
                    } 
                    
                    // Payments Section: status ACCEPTED and paymentStatus RESERVED or PENDING
                    if (status == "ACCEPTED" && (finalRequest.paymentStatus == "RESERVED" || finalRequest.paymentStatus == "PENDING")) {
                        addPaymentCard(finalRequest)
                        hasPayments = true
                    }
                }
                
                tvNoRequests.visibility = if (hasRequests) View.GONE else View.VISIBLE
                hsvRequests.visibility = if (hasRequests) View.VISIBLE else View.GONE
                
                tvNoPayments.visibility = if (hasPayments) View.GONE else View.VISIBLE
                hsvPayments.visibility = if (hasPayments) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HKHome", "Database error: ${error.message}")
            }
        })
    }

    private fun addRequestCard(request: ServiceRequest) {
        val view = layoutInflater.inflate(R.layout.item_service_request, layoutRequests, false)
        
        view.findViewById<TextView>(R.id.tvReqServiceType).text = request.serviceType
        view.findViewById<TextView>(R.id.tvReqDateTime).text = formatSchedule(request.startDate)
        view.findViewById<TextView>(R.id.tvReqHouseholderName).text = request.householderName
        view.findViewById<TextView>(R.id.tvReqSalary).text = "PHP ${request.totalPrice.toInt()}"
        
        val tvStatus = view.findViewById<TextView>(R.id.tvReqStatus)
        tvStatus.text = if (request.status == "RESERVED") "RESERVED" else "PENDING"

        val tvInitials = view.findViewById<TextView>(R.id.tvProfileInitials)
        userRepository.getUserData(request.householderId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    tvInitials.text = "${it.firstName.take(1)}${it.lastName.take(1)}".uppercase()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        view.findViewById<View>(R.id.btnConfirm).setOnClickListener {
            updateStatus(request.requestId, "ACCEPTED", request)
        }
        view.findViewById<View>(R.id.btnDecline).setOnClickListener {
            updateStatus(request.requestId, "DECLINED", request)
        }

        layoutRequests.addView(view)
    }

    private fun addPaymentCard(request: ServiceRequest) {
        val view = layoutInflater.inflate(R.layout.item_housekeeper_payment, layoutPayments, false)
        
        view.findViewById<TextView>(R.id.tvPaymentHouseholderName).text = request.householderName
        view.findViewById<TextView>(R.id.tvPaymentService).text = request.serviceType
        view.findViewById<TextView>(R.id.tvPaymentSalary).text = "PHP ${request.totalPrice.toInt()}"
        
        val tvSchedule = view.findViewById<TextView>(R.id.tvPaymentSchedule)
        if (tvSchedule != null) {
            tvSchedule.text = formatSchedule(request.startDate)
        }
        
        val statusValue = view.findViewById<TextView>(R.id.tvPaymentStatusValue)
        statusValue.text = request.paymentStatus
        statusValue.setTextColor(Color.parseColor("#FFB74D")) // Orange for Reserved/Pending

        val tvInitials = view.findViewById<TextView>(R.id.tvProfileInitials)
        userRepository.getUserData(request.householderId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    tvInitials.text = "${it.firstName.take(1)}${it.lastName.take(1)}".uppercase()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        val btnMarkPayment = view.findViewById<MaterialButton>(R.id.btnMarkPayment)
        
        if (request.staffWaitingConfirmation) {
            // Unclickable if waiting for householder arrival confirmation
            btnMarkPayment.isEnabled = false
            btnMarkPayment.text = "Awaiting Arrival Conf."
            btnMarkPayment.setBackgroundColor(Color.parseColor("#888888"))
            btnMarkPayment.setTextColor(Color.parseColor("#424242"))
        } else {
            // Clickable Mark Payment
            btnMarkPayment.isEnabled = true
            btnMarkPayment.text = "Mark Payment"
            btnMarkPayment.setBackgroundResource(R.drawable.btn_gradient_blue)
            btnMarkPayment.setTextColor(Color.WHITE)
            btnMarkPayment.setOnClickListener {
                showPaymentConfirmation(request)
            }
        }

        layoutPayments.addView(view)
    }

    private fun showPaymentConfirmation(request: ServiceRequest) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Payment")
            .setMessage("Use this only after collecting the cash on-site. This will confirm the booking as paid.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Yes, Confirm Payment") { _, _ ->
                repository.confirmPayment(request.requestId).addOnSuccessListener {
                    Toast.makeText(context, "Payment Confirmed", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun updateStatus(requestId: String, status: String, request: ServiceRequest) {
        val currentUid = repository.getCurrentUserUid() ?: return
        repository.updateRequestStatus(requestId, status).addOnSuccessListener {
            if (status == "ACCEPTED") {
                repository.getUserData(currentUid).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val housekeeper = snapshot.getValue(User::class.java)
                        val housekeeperName = housekeeper?.getDisplayName() ?: "Housekeeper"
                        
                        val notificationMessage = "Hello, I've just accepted your request. I'm pleased to be working with you!"
                        val notification = Notification(
                            userId = request.householderId,
                            senderName = housekeeperName,
                            title = "Request Accepted",
                            body = notificationMessage,
                            createdAt = System.currentTimeMillis(),
                            requestId = requestId,
                            type = "ACCEPTANCE"
                        )
                        repository.sendNotification(notification)
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
            Toast.makeText(context, "Request $status", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to update status", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatSchedule(dateStr: String): String {
        return try {
            val date = if (dateStr.contains("T")) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(dateStr)
            } else {
                SimpleDateFormat("d/M/yyyy", Locale.getDefault()).parse(dateStr)
            }
            SimpleDateFormat("MMMM d, yyyy, h:mm a", Locale.getDefault()).format(date!!)
        } catch (e: Exception) {
            dateStr
        }
    }
}
