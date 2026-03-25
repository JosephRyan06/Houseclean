package com.example.houseclean.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

class AttendanceFragment : Fragment() {

    private val repository = ServiceRepository()
    private lateinit var layoutToday: LinearLayout
    private lateinit var layoutUpcoming: LinearLayout
    private lateinit var tvNoAttendance: TextView
    private lateinit var tvTodayLabel: TextView
    private lateinit var tvUpcomingLabel: TextView
    private var currentStatusFilter = "ACCEPTED"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_attendance, container, false)

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutAttendance)
        layoutToday = view.findViewById(R.id.layoutTodayAttendance)
        layoutUpcoming = view.findViewById(R.id.layoutUpcomingAttendance)
        tvNoAttendance = view.findViewById(R.id.tvNoAttendance)
        tvTodayLabel = view.findViewById(R.id.tvTodayLabel)
        tvUpcomingLabel = view.findViewById(R.id.tvUpcomingLabel)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentStatusFilter = when (tab?.position) {
                    0 -> "ACCEPTED"
                    1 -> "COMPLETED"
                    2 -> "CANCELLED"
                    else -> "ACCEPTED"
                }
                loadAttendanceData()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        loadAttendanceData()

        return view
    }

    private fun loadAttendanceData() {
        val currentUid = repository.getCurrentUserUid() ?: return
        repository.getHousekeeperRequests(currentUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                layoutToday.removeAllViews()
                layoutUpcoming.removeAllViews()

                var hasData = false
                val today = Calendar.getInstance()
                val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
                val todayStr = sdf.format(today.time)

                for (requestSnapshot in snapshot.children) {
                    val request = requestSnapshot.getValue(ServiceRequest::class.java)
                    if (request != null && request.status == currentStatusFilter) {
                        hasData = true
                        if (request.startDate == todayStr) {
                            addAttendanceCard(request, layoutToday)
                        } else {
                            addAttendanceCard(request, layoutUpcoming)
                        }
                    }
                }

                tvNoAttendance.visibility = if (hasData) View.GONE else View.VISIBLE
                tvNoAttendance.text = when(currentStatusFilter) {
                    "ACCEPTED" -> "No ongoing attendance"
                    "COMPLETED" -> "No presence records"
                    "CANCELLED" -> "No late records"
                    else -> "No attendance records found"
                }
                
                val hsvToday = view?.findViewById<View>(R.id.hsvToday)
                val hsvUpcoming = view?.findViewById<View>(R.id.hsvUpcoming)
                
                tvTodayLabel.visibility = if (layoutToday.childCount > 0) View.VISIBLE else View.GONE
                hsvToday?.visibility = if (layoutToday.childCount > 0) View.VISIBLE else View.GONE
                
                tvUpcomingLabel.visibility = if (layoutUpcoming.childCount > 0) View.VISIBLE else View.GONE
                hsvUpcoming?.visibility = if (layoutUpcoming.childCount > 0) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AttendanceFragment", "Error loading data", error.toException())
            }
        })
    }

    private fun addAttendanceCard(request: ServiceRequest, container: LinearLayout) {
        val view = if (currentStatusFilter == "ACCEPTED") {
            layoutInflater.inflate(R.layout.item_attendance_card, container, false)
        } else {
            layoutInflater.inflate(R.layout.item_reminder_card, container, false)
        }
        
        if (currentStatusFilter == "ACCEPTED") {
            val tvHKName = view.findViewById<TextView>(R.id.tvHKName)
            val tvStatusValue = view.findViewById<TextView>(R.id.tvStatusValue)
            val tvJoinedLabel = view.findViewById<TextView>(R.id.tvJoinedLabel)
            val tvJoinedValue = view.findViewById<TextView>(R.id.tvJoinedValue)
            val tvAvailableLabel = view.findViewById<TextView>(R.id.tvAvailableLabel)
            val tvAvailableValue = view.findViewById<TextView>(R.id.tvAvailableValue)
            val btnProfile = view.findViewById<Button>(R.id.btnProfileDetails)
            
            tvHKName.text = request.householderName
            tvJoinedLabel.text = "Service: "
            tvJoinedValue.text = request.serviceType
            tvAvailableLabel.text = "Schedule: "
            tvAvailableValue.text = formatSchedule(request.startDate)
            
            btnProfile.setOnClickListener {
                val intent = Intent(requireContext(), ProfileViewActivity::class.java)
                intent.putExtra("isReadOnly", true)
                intent.putExtra("uid", request.householderId)
                startActivity(intent)
            }
            
            if (request.staffArrived) {
                tvStatusValue.text = "Awaiting Confirmation"
                view.findViewById<View>(R.id.layoutHKButtons).visibility = View.GONE
            } else {
                tvStatusValue.text = "Active"
                val layoutButtons = view.findViewById<View>(R.id.layoutHKButtons)
                layoutButtons.visibility = View.VISIBLE
                view.findViewById<View>(R.id.btnMarkArrived).setOnClickListener {
                    repository.markStaffArrived(request.requestId)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Marked as Arrived", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        } else {
            val tvDateTime = view.findViewById<TextView>(R.id.tvDateTime)
            if (tvDateTime != null) {
                tvDateTime.text = formatSchedule(request.startDate)
            }
            view.findViewById<TextView>(R.id.tvDate).text = formatDate(request.startDate)
            view.findViewById<TextView>(R.id.tvService).text = request.serviceType
            view.findViewById<TextView>(R.id.labelHousekeeper).text = "Householder: "
            view.findViewById<TextView>(R.id.tvHousekeeper).text = request.householderName
            view.findViewById<TextView>(R.id.tvPayment).text = "PHP ${request.totalPrice.toInt()}"
        }

        container.addView(view)
    }

    private fun formatSchedule(dateStr: String?): String {
        if (dateStr == null) return "N/A"
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

    private fun formatDate(dateStr: String): String {
        return try {
            val date = if (dateStr.contains("T")) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(dateStr)
            } else {
                SimpleDateFormat("d/M/yyyy", Locale.getDefault()).parse(dateStr)
            }
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date!!)
        } catch (e: Exception) {
            dateStr
        }
    }
}
