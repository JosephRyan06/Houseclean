package com.example.houseclean.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.houseclean.R
import com.example.houseclean.model.*
import com.example.houseclean.viewmodel.StartServiceViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class Step3ReviewFragment : Fragment() {

    private lateinit var viewModel: StartServiceViewModel
    private val repository = ServiceRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_step3_review, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[StartServiceViewModel::class.java]

        val housekeeperLayout = view.findViewById<View>(R.id.layoutHousekeeperSummary)
        val tvHousekeeper = housekeeperLayout.findViewById<TextView>(R.id.tvHKName)
        val tvJoinedValue = housekeeperLayout.findViewById<TextView>(R.id.tvJoinedValue)
        val tvAvailableValue = housekeeperLayout.findViewById<TextView>(R.id.tvAvailableValue)
        val tvPhone = housekeeperLayout.findViewById<TextView>(R.id.tvHKPhone)
        val rbRating = housekeeperLayout.findViewById<RatingBar>(R.id.rbHKRating)
        val tvRatingCount = housekeeperLayout.findViewById<TextView>(R.id.tvHKRatingsCount)
        
        val tvService = view.findViewById<TextView>(R.id.tvSummaryService)
        val tvSchedule = view.findViewById<TextView>(R.id.tvSummaryDate)
        val tvPrice = view.findViewById<TextView>(R.id.tvSummaryPrice)
        val tvPayment = view.findViewById<TextView>(R.id.tvPaymentMethodSummary)
        val tvNotes = view.findViewById<TextView>(R.id.tvNotesSummary)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitRequest)
        val btnBack = view.findViewById<Button>(R.id.btnBackStep3)

        // Hide time as it's included in formatted schedule
        view.findViewById<View>(R.id.tvSummaryTime).visibility = View.GONE

        viewModel.request.observe(viewLifecycleOwner) { request ->
            tvHousekeeper.text = request.housekeeperName
            tvService.text = "Service: ${request.serviceType}"
            tvSchedule.text = "Schedule: ${formatSchedule(request.startDate)}"
            tvPrice.text = "Price: PHP ${request.totalPrice.toInt()}"
            tvPayment.text = "Payment Method: ${formatPaymentMethod(request.paymentMethod)}"
            tvNotes.text = "Notes: ${request.notes.ifEmpty { "None" }}"

            val tvInitials = housekeeperLayout.findViewById<TextView>(R.id.tvProfileInitials)
            tvInitials.text = if (request.housekeeperName.isNotBlank()) {
                request.housekeeperName.split(" ").filter { it.isNotEmpty() }
                    .map { it[0] }.take(2).joinToString("").uppercase()
            } else "?"

            // Fetch extra housekeeper data from database
            if (request.housekeeperId.isNotEmpty()) {
                repository.getUserData(request.housekeeperId).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val housekeeper = snapshot.getValue(User::class.java)
                        if (housekeeper != null) {
                            tvJoinedValue.text = housekeeper.joined
                            tvAvailableValue.text = housekeeper.availability?.toString() ?: "Not specified"
                            tvPhone.text = housekeeper.phone.ifEmpty { housekeeper.contact }
                            
                            rbRating.rating = housekeeper.ratingAverage.toFloat()
                            tvRatingCount.text = "(${housekeeper.ratingCount} reviews)"
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
            
            btnSubmit.setOnClickListener {
                if (request.housekeeperId.isEmpty()) {
                    Toast.makeText(context, "Housekeeper information is missing", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                submitRequest(request)
            }
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun submitRequest(request: ServiceRequest) {
        val currentUid = repository.getCurrentUserUid() ?: return
        
        // Get householder info first
        repository.getUserData(currentUid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val householder = snapshot.getValue(User::class.java)
                val householderName = householder?.getDisplayName() ?: "Unknown User"
                val customerEmail = householder?.email ?: ""
                
                val now = System.currentTimeMillis()
                
                // Business logic for status and paymentStatus
                val (finalStatus, finalPaymentStatus, cohConfirmed) = when (request.paymentMethod) {
                    "CASH_ON_HAND" -> Triple("RESERVED", "RESERVED", true)
                    "STATIC_QR" -> Triple("PENDING_PAYMENT", "PENDING", false)
                    else -> Triple("PENDING", "NONE", false)
                }

                val finalRequest = request.copy(
                    householderId = currentUid,
                    householderName = householderName,
                    customerEmail = customerEmail,
                    status = finalStatus,
                    paymentStatus = finalPaymentStatus,
                    cashOnHandConfirmed = cohConfirmed,
                    timestamp = now,
                    updatedAt = now
                )
                
                repository.submitServiceRequest(finalRequest).addOnSuccessListener {
                    // Send notification to housekeeper
                    val (notifTitle, notifBody) = if (request.paymentMethod == "CASH_ON_HAND") {
                        "Cash on hand reserved" to "${request.serviceType} is reserved for cash payment on arrival."
                    } else {
                        "New booking request" to "${request.serviceType} - ${request.startDate}"
                    }

                    val notification = Notification(
                        userId = request.housekeeperId,
                        senderId = currentUid,
                        senderName = householderName,
                        title = notifTitle,
                        body = notifBody,
                        createdAt = now,
                        requestId = "", // Will be updated by repository push key logic if needed
                        type = "SERVICE_REQUEST"
                    )
                    repository.sendNotification(notification)

                    Toast.makeText(context, "Request Submitted Successfully!", Toast.LENGTH_SHORT).show()
                    activity?.finish()
                }.addOnFailureListener { e ->
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Step3Review", "Error: ${error.message}")
            }
        })
    }

    private fun formatSchedule(dateStr: String): String {
        if (dateStr.isEmpty()) return "Not set"
        return try {
            val inputDate: Date? = if (dateStr.contains("T")) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(dateStr)
            } else {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
            }
            
            if (inputDate == null) return dateStr

            val dayName = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()).format(inputDate).uppercase()
            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(inputDate)

            val cal = Calendar.getInstance()
            cal.time = inputDate
            val hour24 = cal.get(Calendar.HOUR_OF_DAY)
            
            val period = when (hour24) {
                in 5..11 -> "Morning"
                in 12..16 -> "Afternoon"
                in 17..20 -> "Evening"
                in 21..23 -> "Night"
                else -> "Midnight"
            }
            
            "$dayName\n$period ($timeStr)"
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun formatPaymentMethod(method: String): String {
        return when (method) {
            "CASH_ON_HAND" -> "Cash on Hand"
            "STATIC_QR" -> "Static QR"
            else -> method
        }
    }
}
