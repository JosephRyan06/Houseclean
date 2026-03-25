package com.example.houseclean.view

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.houseclean.R
import com.example.houseclean.model.Notification
import com.example.houseclean.model.ServiceRepository
import com.example.houseclean.model.ServiceRequest
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class PaymentFragment : Fragment() {

    private val repository = ServiceRepository()
    private lateinit var layoutToday: LinearLayout
    private lateinit var layoutUpcoming: LinearLayout
    private lateinit var tvNoPayments: TextView
    private lateinit var tvTodayLabel: TextView
    private lateinit var tvUpcomingLabel: TextView
    private lateinit var hsvToday: View
    private lateinit var hsvUpcoming: View
    private var currentTabFilter = "WAITING" // WAITING, PAID, OVERDUE, REJECTED

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_payment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layoutToday = view.findViewById(R.id.layoutTodayPayments)
        layoutUpcoming = view.findViewById(R.id.layoutUpcomingPayments)
        tvNoPayments = view.findViewById(R.id.tvNoPayments)
        tvTodayLabel = view.findViewById(R.id.tvTodayPaymentLabel)
        tvUpcomingLabel = view.findViewById(R.id.tvUpcomingPaymentLabel)
        hsvToday = view.findViewById(R.id.hsvTodayPayments)
        hsvUpcoming = view.findViewById(R.id.hsvUpcomingPayments)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutPayments)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTabFilter = tab?.text.toString().uppercase()
                fetchPayments()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        fetchPayments()
    }

    private fun fetchPayments() {
        val currentUid = repository.getCurrentUserUid() ?: return
        
        repository.getHouseholderRequests(currentUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                layoutToday.removeAllViews()
                layoutUpcoming.removeAllViews()
                
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val now = Date()
                val todayStr = sdf.format(now)
                var totalCount = 0
                var todayCount = 0
                var upcomingCount = 0

                val requests = mutableListOf<ServiceRequest>()
                for (child in snapshot.children) {
                    val request = child.getValue(ServiceRequest::class.java) ?: continue
                    val requestId = child.key ?: ""
                    val finalRequest = request.copy(requestId = requestId)

                    val isPastSchedule = try {
                        val scheduleDate = if (finalRequest.startDate.contains("T")) {
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(finalRequest.startDate)
                        } else {
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(finalRequest.startDate)
                        }
                        scheduleDate != null && now.after(scheduleDate)
                    } catch (e: Exception) {
                        false
                    }

                    val isOverdue = when {
                        finalRequest.paymentStatus == "OVERDUE" -> true
                        isPastSchedule && (finalRequest.paymentStatus == "PENDING" || finalRequest.paymentStatus == "RESERVED") -> true
                        finalRequest.paymentStatus == "PAID" && finalRequest.paidAt > 0 -> {
                            val scheduleTime = try {
                                if (finalRequest.startDate.contains("T")) {
                                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(finalRequest.startDate)?.time ?: 0L
                                } else {
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(finalRequest.startDate)?.time ?: 0L
                                }
                            } catch (e: Exception) { 0L }
                            finalRequest.paidAt > scheduleTime
                        }
                        else -> false
                    }

                    val matchesFilter = when (currentTabFilter) {
                        "REJECTED" -> finalRequest.status == "DECLINED"
                        "OVERDUE" -> finalRequest.status != "DECLINED" && isOverdue
                        "WAITING" -> finalRequest.status != "DECLINED" && !isOverdue && (finalRequest.status == "PENDING_PAYMENT" || finalRequest.status == "RESERVED" || finalRequest.status == "ACCEPTED") && 
                                    (finalRequest.paymentStatus == "PENDING" || finalRequest.paymentStatus == "RESERVED")
                        "PAID" -> finalRequest.status != "DECLINED" && !isOverdue && finalRequest.paymentStatus == "PAID"
                        else -> false
                    }

                    if (matchesFilter) {
                        requests.add(finalRequest)
                    }
                }

                requests.sortBy { it.startDate }

                for (request in requests) {
                    val requestDate = if (request.startDate.contains("T")) {
                        request.startDate.split("T")[0]
                    } else {
                        request.startDate
                    }
                    
                    if (requestDate == todayStr) {
                        addPaymentCard(request, layoutToday)
                        todayCount++
                    } else {
                        addPaymentCard(request, layoutUpcoming)
                        upcomingCount++
                    }
                    totalCount++
                }
                
                tvTodayLabel.visibility = if (todayCount > 0) View.VISIBLE else View.GONE
                hsvToday.visibility = if (todayCount > 0) View.VISIBLE else View.GONE
                
                tvUpcomingLabel.visibility = if (upcomingCount > 0) View.VISIBLE else View.GONE
                hsvUpcoming.visibility = if (upcomingCount > 0) View.VISIBLE else View.GONE
                
                tvNoPayments.visibility = if (totalCount == 0) View.VISIBLE else View.GONE
                tvNoPayments.text = "No ${currentTabFilter.lowercase()} payments found"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PaymentFragment", "Error: ${error.message}")
            }
        })
    }

    private fun addPaymentCard(request: ServiceRequest, container: LinearLayout) {
        val view = layoutInflater.inflate(R.layout.item_payment_history_card, container, false)
        
        view.findViewById<TextView>(R.id.tvPaymentPrice).text = "PHP ${request.totalPrice.toInt()}"
        
        val statusValue = view.findViewById<TextView>(R.id.tvPaymentStatusValue)
        
        when (currentTabFilter) {
            "REJECTED" -> {
                statusValue.text = "REJECTED"
                statusValue.setTextColor(Color.parseColor("#E57373"))
            }
            "OVERDUE" -> {
                statusValue.text = "OVERDUE"
                statusValue.setTextColor(Color.parseColor("#E57373"))
            }
            else -> {
                statusValue.text = if (request.status == "PENDING_PAYMENT") "Awaiting Staff Confirmation" else request.paymentStatus
                when(request.paymentStatus) {
                    "PAID" -> statusValue.setTextColor(Color.parseColor("#2DCC91"))
                    "PENDING", "RESERVED" -> statusValue.setTextColor(Color.parseColor("#FFB74D"))
                    "OVERDUE" -> statusValue.setTextColor(Color.parseColor("#E57373"))
                }
            }
        }

        if (request.startDate.contains("T")) {
            val parts = request.startDate.split("T")
            try {
                val dateObj = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(parts[0])
                view.findViewById<TextView>(R.id.tvPaymentSchedule).text = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(dateObj!!)
                
                val timeObj = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(parts[1])
                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(timeObj!!)
                
                val hour24 = parts[1].split(":")[0].toInt()
                val period = when (hour24) {
                    in 5..11 -> "Morning"
                    in 12..16 -> "Afternoon"
                    in 17..20 -> "Evening"
                    in 21..23 -> "Night"
                    else -> "Midnight"
                }
                view.findViewById<TextView>(R.id.tvPaymentDayTime).text = "$period, $timeStr"
            } catch (e: Exception) {
                view.findViewById<TextView>(R.id.tvPaymentSchedule).text = parts[0]
                view.findViewById<TextView>(R.id.tvPaymentDayTime).text = parts[1]
            }
        }

        view.findViewById<TextView>(R.id.tvPaymentService).text = request.serviceType
        view.findViewById<TextView>(R.id.tvPaymentHousekeeper).text = request.housekeeperName

        val btnPay = view.findViewById<View>(R.id.btnPayNow)
        if (currentTabFilter == "WAITING" && request.paymentStatus == "PENDING") {
            btnPay.visibility = View.VISIBLE
            btnPay.setOnClickListener {
                showPayDialog(request)
            }
        } else {
            btnPay.visibility = View.GONE
        }

        view.findViewById<View>(R.id.btnViewPaymentDetails).setOnClickListener {
            val intent = Intent(activity, ServiceDetailsActivity::class.java)
            intent.putExtra("requestId", request.requestId)
            startActivity(intent)
        }

        container.addView(view)
    }

    private fun showPayDialog(request: ServiceRequest) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Enter Transaction ID")

        val input = EditText(requireContext())
        input.hint = "Transaction ID"
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        input.layoutParams = lp
        
        val container = LinearLayout(requireContext())
        container.orientation = LinearLayout.VERTICAL
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(50, 0, 50, 0)
        container.addView(input, params)
        
        builder.setView(container)

        builder.setPositiveButton("Confirm") { _, _ ->
            val transactionId = input.text.toString().trim()
            if (transactionId.isNotEmpty()) {
                repository.submitPaymentInfo(request.requestId, transactionId, request.paymentMethod)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Payment Submitted Successfully!", Toast.LENGTH_SHORT).show()
                        
                        // Send notification to housekeeper
                        val notification = Notification(
                            userId = request.housekeeperId,
                            title = "Booking confirmed (Paid)",
                            body = "${request.serviceType} has been paid via QR. Please review the job.",
                            createdAt = System.currentTimeMillis(),
                            requestId = request.requestId,
                            type = "PAYMENT_CONFIRMED"
                        )
                        repository.sendNotification(notification)
                    }
            } else {
                Toast.makeText(context, "Please enter Transaction ID", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val date = if (dateStr.contains("T")) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(dateStr)
            } else {
                SimpleDateFormat("d/M/yyyy", Locale.getDefault()).parse(dateStr)
            }
            SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date!!)
        } catch (e: Exception) {
            dateStr
        }
    }
}
