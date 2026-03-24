package com.example.houseclean.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.houseclean.R
import com.example.houseclean.model.ServiceRepository
import com.example.houseclean.model.ServiceRequest
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class RequestFragment : Fragment() {

    private val repository = ServiceRepository()
    private lateinit var layoutToday: LinearLayout
    private lateinit var layoutUpcoming: LinearLayout
    private lateinit var tvNoRequests: TextView
    private lateinit var tvTodayLabel: TextView
    private lateinit var tvUpcomingLabel: TextView
    private lateinit var hsvToday: View
    private lateinit var hsvUpcoming: View
    private var currentStatus = "PENDING"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_request, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layoutToday = view.findViewById(R.id.layoutTodayRequests)
        layoutUpcoming = view.findViewById(R.id.layoutUpcomingRequests)
        tvNoRequests = view.findViewById(R.id.tvNoRequests)
        tvTodayLabel = view.findViewById(R.id.tvTodayLabel)
        tvUpcomingLabel = view.findViewById(R.id.tvUpcomingLabel)
        hsvToday = view.findViewById(R.id.hsvToday)
        hsvUpcoming = view.findViewById(R.id.hsvUpcoming)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutRequests)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentStatus = tab?.text.toString().uppercase()
                // "Accepted" tab maps to "ACCEPTED" in db
                // "Pending" tab maps to "PENDING" in db
                // "Declined" tab maps to "DECLINED" in db
                // "Cancelled" tab maps to "CANCELLED" in db
                fetchRequests()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        fetchRequests()
    }

    private fun fetchRequests() {
        val currentUid = repository.getCurrentUserUid() ?: return
        
        repository.getHouseholderRequests(currentUid).addValueEventListener(object : ValueEventListener {
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
                    val request = child.getValue(ServiceRequest::class.java)
                    if (request != null && request.status == currentStatus) {
                        val finalRequest = if (request.requestId.isEmpty()) {
                            request.copy(requestId = child.key ?: "")
                        } else {
                            request
                        }
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
                        addRequestCard(request, layoutToday)
                        todayCount++
                    } else {
                        addRequestCard(request, layoutUpcoming)
                        upcomingCount++
                    }
                    totalCount++
                }
                
                tvTodayLabel.visibility = if (todayCount > 0) View.VISIBLE else View.GONE
                hsvToday.visibility = if (todayCount > 0) View.VISIBLE else View.GONE
                
                tvUpcomingLabel.visibility = if (upcomingCount > 0) View.VISIBLE else View.GONE
                hsvUpcoming.visibility = if (upcomingCount > 0) View.VISIBLE else View.GONE
                
                tvNoRequests.visibility = if (totalCount == 0) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RequestFragment", "Error: ${error.message}")
            }
        })
    }

    private fun addRequestCard(request: ServiceRequest, container: LinearLayout) {
        val view = layoutInflater.inflate(R.layout.item_reminder_card, container, false)
        
        view.findViewById<TextView>(R.id.tvDateTime).text = formatSchedule(request.startDate)
        view.findViewById<TextView>(R.id.tvDate).text = formatDate(request.startDate)
        view.findViewById<TextView>(R.id.tvService).text = request.serviceType
        view.findViewById<TextView>(R.id.tvHousekeeper).text = request.housekeeperName
        view.findViewById<TextView>(R.id.tvPayment).text = "PHP ${request.totalPrice.toInt()}"

        val btnCancel = view.findViewById<View>(R.id.btnCancelRequest)
        if (currentStatus == "PENDING") {
            btnCancel.visibility = View.VISIBLE
            btnCancel.setOnClickListener {
                cancelRequest(request.requestId)
            }
        } else {
            btnCancel.visibility = View.GONE
        }

        view.findViewById<View>(R.id.btnViewDetails).setOnClickListener {
            val intent = Intent(activity, ServiceDetailsActivity::class.java)
            intent.putExtra("requestId", request.requestId)
            startActivity(intent)
        }

        container.addView(view)
    }

    private fun cancelRequest(requestId: String) {
        repository.updateRequestStatus(requestId, "CANCELLED")
            .addOnSuccessListener {
                Toast.makeText(context, "Request Cancelled", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to cancel request", Toast.LENGTH_SHORT).show()
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
