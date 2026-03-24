package com.example.houseclean.view

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.houseclean.R
import com.example.houseclean.model.ServiceRepository
import com.example.houseclean.model.ServiceRequest
import com.example.houseclean.model.UserRepository
import com.example.houseclean.model.User
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private val repository = ServiceRepository()
    private val userRepository = UserRepository()
    private lateinit var layoutReminders: LinearLayout
    private lateinit var layoutAttendance: LinearLayout
    private lateinit var tvWelcomeName: TextView
    private lateinit var tvNoReminders: TextView
    private lateinit var tvNoAttendance: TextView
    private lateinit var hsvReminders: View
    private lateinit var hsvAttendance: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layoutReminders = view.findViewById(R.id.layoutReminders)
        layoutAttendance = view.findViewById(R.id.layoutAttendance)
        tvWelcomeName = view.findViewById(R.id.tvWelcomeName)
        tvNoReminders = view.findViewById(R.id.tvNoReminders)
        tvNoAttendance = view.findViewById(R.id.tvNoAttendance)
        hsvReminders = view.findViewById(R.id.hsvReminders)
        hsvAttendance = view.findViewById(R.id.hsvAttendance)

        val btnStartService = view.findViewById<MaterialButton>(R.id.btnStartService)
        btnStartService.setOnClickListener {
            val intent = Intent(activity, StartServiceActivity::class.java)
            startActivity(intent)
        }

        setupWelcomeMessage()
        fetchData()
    }

    private fun setupWelcomeMessage() {
        val currentUid = userRepository.getCurrentUserUid() ?: return
        userRepository.getUserData(currentUid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
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

        repository.getHouseholderRequests(currentUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                layoutReminders.removeAllViews()
                layoutAttendance.removeAllViews()
                
                val acceptedRequests = mutableListOf<ServiceRequest>()
                
                for (child in snapshot.children) {
                    val request = child.getValue(ServiceRequest::class.java) ?: continue
                    val requestId = child.key ?: ""
                    val finalRequest = request.copy(requestId = requestId)
                    
                    if (finalRequest.status == "ACCEPTED") {
                        acceptedRequests.add(finalRequest)
                    }
                }

                acceptedRequests.sortBy { it.startDate }

                for (request in acceptedRequests) {
                    addReminderCard(request)
                    addAttendanceCard(request)
                }

                tvNoReminders.visibility = if (acceptedRequests.isEmpty()) View.VISIBLE else View.GONE
                hsvReminders.visibility = if (acceptedRequests.isEmpty()) View.GONE else View.VISIBLE
                
                tvNoAttendance.visibility = if (acceptedRequests.isEmpty()) View.VISIBLE else View.GONE
                hsvAttendance.visibility = if (acceptedRequests.isEmpty()) View.GONE else View.VISIBLE
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun addReminderCard(request: ServiceRequest) {
        val view = layoutInflater.inflate(R.layout.item_reminder_card, layoutReminders, false)
        view.findViewById<TextView>(R.id.tvDateTime).text = formatSchedule(request.startDate)
        view.findViewById<TextView>(R.id.tvDate).text = formatDate(request.startDate)
        view.findViewById<TextView>(R.id.tvService).text = request.serviceType
        view.findViewById<TextView>(R.id.tvHousekeeper).text = request.housekeeperName
        view.findViewById<TextView>(R.id.tvPayment).text = "PHP ${request.totalPrice.toInt()}"

        view.findViewById<View>(R.id.btnViewDetails).setOnClickListener {
            openDetails(request.requestId)
        }
        layoutReminders.addView(view)
    }

    private fun addAttendanceCard(request: ServiceRequest) {
        val view = layoutInflater.inflate(R.layout.item_attendance_card, layoutAttendance, false)
        
        val tvHKName = view.findViewById<TextView>(R.id.tvHKName)
        val tvStatusValue = view.findViewById<TextView>(R.id.tvStatusValue)
        val tvJoinedValue = view.findViewById<TextView>(R.id.tvJoinedValue)
        val tvAvailableValue = view.findViewById<TextView>(R.id.tvAvailableValue)
        val tvInitials = view.findViewById<TextView>(R.id.tvProfileInitials)
        val profileBg = view.findViewById<View>(R.id.profileBg)
        val statusStroke = view.findViewById<View>(R.id.viewStatusStroke)
        
        val tvArrivalMessage = view.findViewById<TextView>(R.id.tvArrivalMessage)
        val layoutHHButtons = view.findViewById<View>(R.id.layoutHHButtons)
        val btnYesArrived = view.findViewById<MaterialButton>(R.id.btnYesArrived)

        tvHKName.text = request.housekeeperName

        if (request.staffWaitingConfirmation) {
            tvArrivalMessage.visibility = View.VISIBLE
            layoutHHButtons.visibility = View.VISIBLE
            tvStatusValue.text = "Staff Waiting"
            
            // Enabled state (original blue gradient)
            btnYesArrived.isEnabled = true
            btnYesArrived.setBackgroundResource(R.drawable.btn_gradient_blue)
            btnYesArrived.setTextColor(Color.WHITE)
            
            btnYesArrived.setOnClickListener {
                repository.confirmStaffArrival(request.requestId, request.housekeeperId, request.housekeeperName).addOnSuccessListener {
                    Toast.makeText(context, "Arrival Confirmed", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (request.staffArrived) {
            tvStatusValue.text = "Present"
            tvArrivalMessage.visibility = View.GONE
            layoutHHButtons.visibility = View.GONE
        } else {
            tvStatusValue.text = "On-Going"
            tvArrivalMessage.visibility = View.VISIBLE // Keep visible but button disabled
            layoutHHButtons.visibility = View.VISIBLE
            
            // Disabled state (#888888 background, #424242 text)
            btnYesArrived.isEnabled = false
            btnYesArrived.setBackgroundColor(Color.parseColor("#888888"))
            btnYesArrived.setTextColor(Color.parseColor("#424242"))
        }

        userRepository.getUserData(request.housekeeperId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    tvHKName.text = "${it.lastName}, ${it.firstName}"
                    tvJoinedValue.text = it.joined
                    
                    tvAvailableValue.text = when (it.availability) {
                        is String -> it.availability
                        is List<*> -> it.availability.joinToString(", ")
                        else -> "Not set"
                    }

                    val initials = "${it.firstName.take(1)}${it.lastName.take(1)}".uppercase()
                    tvInitials.text = initials
                    
                    val colors = listOf("#E57373", "#F06292", "#BA68C8", "#9575CD", "#7986CB", "#64B5F6", "#4FC3F7", "#4DD0E1", "#4DB6AC", "#81C784", "#AED581", "#DCE775", "#FFF176", "#FFD54F", "#FFB74D", "#FF8A65")
                    val colorIndex = Math.abs(it.uid.hashCode()) % colors.size
                    val randomColor = Color.parseColor(colors[colorIndex])
                    (profileBg.background as? GradientDrawable)?.setColor(randomColor)
                    
                    val strokeColor = if (it.isOnline) Color.parseColor("#0DFF00") else Color.parseColor("#888888")
                    (statusStroke.background as? GradientDrawable)?.setStroke(6, strokeColor)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        layoutAttendance.addView(view)
    }

    private fun openDetails(requestId: String) {
        val intent = Intent(activity, ServiceDetailsActivity::class.java)
        intent.putExtra("requestId", requestId)
        startActivity(intent)
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
            
            val finalTimeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(inputDate)

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
            
            "$dayName, $period, $finalTimeStr"
        } catch (e: Exception) {
            if (dateStr.contains("T")) {
                dateStr.replace("T", ", ")
            } else {
                dateStr
            }
        }
    }
}
