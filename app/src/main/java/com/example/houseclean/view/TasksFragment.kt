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
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class TasksFragment : Fragment() {

    private val repository = ServiceRepository()
    private lateinit var layoutToday: LinearLayout
    private lateinit var layoutUpcoming: LinearLayout
    private lateinit var tvNoTasks: TextView
    private lateinit var tvTodayLabel: TextView
    private lateinit var tvUpcomingLabel: TextView
    private lateinit var hsvToday: View
    private lateinit var hsvUpcoming: View
    private var currentFilter = "PENDING" // PENDING, ON-GOING, COMPLETED

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layoutToday = view.findViewById(R.id.layoutTodayTasks)
        layoutUpcoming = view.findViewById(R.id.layoutUpcomingTasks)
        tvNoTasks = view.findViewById(R.id.tvNoTasks)
        tvTodayLabel = view.findViewById(R.id.tvTodayLabel)
        tvUpcomingLabel = view.findViewById(R.id.tvUpcomingLabel)
        hsvToday = view.findViewById(R.id.hsvToday)
        hsvUpcoming = view.findViewById(R.id.hsvUpcoming)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutTasks)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentFilter = when (tab?.position) {
                    0 -> "PENDING"
                    1 -> "ON-GOING"
                    2 -> "COMPLETED"
                    else -> "PENDING"
                }
                fetchTasks()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        fetchTasks()
    }

    private fun fetchTasks() {
        val currentUid = repository.getCurrentUserUid() ?: return
        
        repository.getHousekeeperRequests(currentUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                layoutToday.removeAllViews()
                layoutUpcoming.removeAllViews()
                
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = sdf.format(Date())
                var totalCount = 0
                var todayCount = 0
                var upcomingCount = 0

                val requests = mutableListOf<ServiceRequest>()
                for (child in snapshot.children) {
                    val request = child.getValue(ServiceRequest::class.java) ?: continue
                    val requestId = child.key ?: ""
                    val finalRequest = request.copy(requestId = requestId)

                    // PENDING: Accepted but not yet started or not yet paid
                    // ON-GOING: Accepted, staff arrived AND paid
                    // COMPLETED: Status is COMPLETED
                    val matchesFilter = when (currentFilter) {
                        "PENDING" -> finalRequest.status == "ACCEPTED" && (!finalRequest.staffArrived || finalRequest.paymentStatus != "PAID")
                        "ON-GOING" -> finalRequest.status == "ACCEPTED" && finalRequest.staffArrived && finalRequest.paymentStatus == "PAID"
                        "COMPLETED" -> finalRequest.status == "COMPLETED"
                        else -> false
                    }

                    if (matchesFilter) {
                        requests.add(finalRequest)
                    }
                }

                // Sort by date
                requests.sortBy { it.startDate }

                for (request in requests) {
                    val requestDate = if (request.startDate.contains("T")) {
                        request.startDate.split("T")[0]
                    } else {
                        request.startDate
                    }
                    
                    if (requestDate == today) {
                        addTaskCard(request, layoutToday)
                        todayCount++
                    } else {
                        addTaskCard(request, layoutUpcoming)
                        upcomingCount++
                    }
                    totalCount++
                }
                
                tvTodayLabel.visibility = if (todayCount > 0) View.VISIBLE else View.GONE
                hsvToday.visibility = if (todayCount > 0) View.VISIBLE else View.GONE
                
                tvUpcomingLabel.visibility = if (upcomingCount > 0) View.VISIBLE else View.GONE
                hsvUpcoming.visibility = if (upcomingCount > 0) View.VISIBLE else View.GONE
                
                tvNoTasks.visibility = if (totalCount == 0) View.VISIBLE else View.GONE
                tvNoTasks.text = when(currentFilter) {
                    "PENDING" -> "No pending tasks found"
                    "ON-GOING" -> "No ongoing tasks found"
                    "COMPLETED" -> "No completed tasks found"
                    else -> "No tasks found"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TasksFragment", "Error: ${error.message}")
            }
        })
    }

    private fun addTaskCard(request: ServiceRequest, container: LinearLayout) {
        val view = if (currentFilter == "PENDING" || currentFilter == "ON-GOING") {
            layoutInflater.inflate(R.layout.item_attendance_card, container, false)
        } else {
            layoutInflater.inflate(R.layout.item_reminder_card, container, false)
        }
        
        if (currentFilter == "PENDING" || currentFilter == "ON-GOING") {
            val tvHKName = view.findViewById<TextView>(R.id.tvHKName)
            val tvStatusValue = view.findViewById<TextView>(R.id.tvStatusValue)
            val tvJoinedLabel = view.findViewById<TextView>(R.id.tvJoinedLabel)
            val tvJoinedValue = view.findViewById<TextView>(R.id.tvJoinedValue)
            val tvAvailableLabel = view.findViewById<TextView>(R.id.tvAvailableLabel)
            val tvAvailableValue = view.findViewById<TextView>(R.id.tvAvailableValue)
            
            tvHKName.text = request.householderName
            tvJoinedLabel.text = "Service: "
            tvJoinedValue.text = request.serviceType
            tvAvailableLabel.text = "Schedule: "
            tvAvailableValue.text = formatSchedule(request.startDate)
            
            val layoutHKButtons = view.findViewById<View>(R.id.layoutHKButtons)
            val btnAction = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnMarkArrived)
            val btnComplete = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnComplete)

            if (currentFilter == "PENDING") {
                layoutHKButtons.visibility = View.VISIBLE
                btnComplete.visibility = View.GONE
                btnAction.visibility = View.VISIBLE

                if (!request.staffArrived) {
                    if (request.staffWaitingConfirmation) {
                        tvStatusValue.text = "Awaiting Confirmation"
                        layoutHKButtons.visibility = View.GONE
                    } else {
                        tvStatusValue.text = "Active"
                        btnAction.text = "Mark Arrived"
                        btnAction.isEnabled = true
                        btnAction.setBackgroundResource(R.drawable.btn_gradient_blue)
                        btnAction.setTextColor(Color.WHITE)
                        btnAction.setOnClickListener {
                            repository.markStaffArrived(request.requestId).addOnSuccessListener {
                                Toast.makeText(context, "Marked as Arrived. Awaiting Householder Confirmation.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else if (request.paymentStatus != "PAID") {
                    tvStatusValue.text = "Awaiting Payment"
                    btnAction.text = "Mark Paid"
                    btnAction.isEnabled = true
                    btnAction.setBackgroundResource(R.drawable.btn_gradient_blue)
                    btnAction.setTextColor(Color.WHITE)
                    btnAction.setOnClickListener {
                        showPaymentConfirmation(request)
                    }
                }
            } else if (currentFilter == "ON-GOING") {
                tvStatusValue.text = "Working"
                layoutHKButtons.visibility = View.VISIBLE
                btnAction.visibility = View.GONE
                btnComplete.visibility = View.VISIBLE
                
                btnComplete.setOnClickListener {
                    showCompleteConfirmation(request)
                }
            }
        } else {
            // Completed tasks
            view.findViewById<TextView>(R.id.tvDateTime).text = formatSchedule(request.startDate)
            view.findViewById<TextView>(R.id.tvDate).text = formatDate(request.startDate)
            view.findViewById<TextView>(R.id.tvService).text = request.serviceType
            view.findViewById<TextView>(R.id.labelHousekeeper).text = "Householder: "
            view.findViewById<TextView>(R.id.tvHousekeeper).text = request.householderName
            view.findViewById<TextView>(R.id.tvPayment).text = "PHP ${request.totalPrice.toInt()}"
            view.findViewById<View>(R.id.btnViewDetails).setOnClickListener {
                val intent = Intent(activity, ServiceDetailsActivity::class.java)
                intent.putExtra("requestId", request.requestId)
                startActivity(intent)
            }
        }

        container.addView(view)
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

    private fun showCompleteConfirmation(request: ServiceRequest) {
        AlertDialog.Builder(requireContext())
            .setMessage("Confirming will complete the request and send a rating and feedback to the customer.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Yes, Complete") { _, _ ->
                completeRequest(request)
            }
            .show()
    }

    private fun completeRequest(request: ServiceRequest) {
        val currentUid = repository.getCurrentUserUid() ?: return
        
        repository.updateRequestStatus(request.requestId, "COMPLETED").addOnSuccessListener {
            // Get housekeeper name for notification
            repository.getUserData(currentUid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val housekeeper = snapshot.getValue(User::class.java)
                    val housekeeperName = housekeeper?.getDisplayName() ?: "Housekeeper"
                    
                    val notification = Notification(
                        userId = request.householderId,
                        senderId = currentUid,
                        senderName = housekeeperName,
                        title = "Customer feedback received",
                        body = "A customer left feedback on a completed request.",
                        createdAt = System.currentTimeMillis(),
                        requestId = request.requestId,
                        type = "RATING_REQUEST"
                    )
                    repository.sendNotification(notification)
                    Toast.makeText(context, "Task Completed", Toast.LENGTH_SHORT).show()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
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

    private fun formatSchedule(dateStr: String): String {
        return try {
            val inputDate: Date? = if (dateStr.contains("T")) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(dateStr)
            } else {
                SimpleDateFormat("d/M/yyyy", Locale.getDefault()).parse(dateStr)
            }
            
            val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(inputDate!!).uppercase()
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
            
            "$dayName, $period, $timeStr"
        } catch (e: Exception) {
            dateStr
        }
    }
}
