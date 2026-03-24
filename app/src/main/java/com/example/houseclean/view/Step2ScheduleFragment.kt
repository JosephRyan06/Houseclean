package com.example.houseclean.view

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.houseclean.R
import com.example.houseclean.model.ServiceRepository
import com.example.houseclean.model.User
import com.example.houseclean.viewmodel.StartServiceViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class Step2ScheduleFragment : Fragment() {

    private val repository = ServiceRepository()
    private val viewModel: StartServiceViewModel by activityViewModels()
    private var selectedHousekeeper: User? = null
    private var isQuickBook = false

    private var tempDate = "" 
    private var tempTime = "" 
    
    private var allHousekeepers = listOf<User>()
    private var currentFilter = "Available" // Default filter

    private lateinit var rvHousekeepers: RecyclerView
    private lateinit var etStartDate: EditText
    private lateinit var etStartTime: EditText
    private lateinit var layoutTimeSlots: LinearLayout
    
    private lateinit var tvFilterAvailable: TextView
    private lateinit var tvFilterUnavailable: TextView
    private lateinit var tvFilterTotal: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_step2_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etStartDate = view.findViewById(R.id.etStartDate)
        etStartTime = view.findViewById(R.id.etStartTime)
        rvHousekeepers = view.findViewById(R.id.rvHousekeepers)
        layoutTimeSlots = view.findViewById(R.id.layoutTimeSlots)
        
        tvFilterAvailable = view.findViewById(R.id.tvFilterAvailable)
        tvFilterUnavailable = view.findViewById(R.id.tvFilterUnavailable)
        tvFilterTotal = view.findViewById(R.id.tvFilterTotal)
        
        val btnQuickBook = view.findViewById<Button>(R.id.btnQuickBook)
        val btnBack = view.findViewById<Button>(R.id.btnBackStep2)
        val btnNext = view.findViewById<Button>(R.id.btnNextStep2)

        // Horizontal Scroll with Pager effect
        rvHousekeepers.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        PagerSnapHelper().attachToRecyclerView(rvHousekeepers)

        setupTimeSlots()
        setupFilters()

        etStartDate.setOnClickListener { showDatePicker() }
        
        btnQuickBook.setOnClickListener {
            isQuickBook = true
            quickBookStaff()
        }

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        btnNext.setOnClickListener {
            if (tempDate.isEmpty() || tempTime.isEmpty()) {
                Toast.makeText(context, "Please select date and time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedHousekeeper == null && !isQuickBook) {
                Toast.makeText(context, "Please select a housekeeper", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val combinedDateTime = "${tempDate}T${tempTime}"
            viewModel.updateSchedule(combinedDateTime)
            
            if (isQuickBook) {
                quickBookStaff()
            } else {
                proceedToReview()
            }
        }

        loadAllStaff()
    }

    private fun setupFilters() {
        tvFilterAvailable.setOnClickListener {
            currentFilter = "Available"
            updateListByFilter()
        }
        tvFilterUnavailable.setOnClickListener {
            currentFilter = "Unavailable"
            updateListByFilter()
        }
        tvFilterTotal.setOnClickListener {
            currentFilter = "Total"
            updateListByFilter()
        }
    }

    private fun updateListByFilter() {
        tvFilterAvailable.alpha = if (currentFilter == "Available") 1.0f else 0.5f
        tvFilterUnavailable.alpha = if (currentFilter == "Unavailable") 1.0f else 0.5f
        tvFilterTotal.alpha = if (currentFilter == "Total") 1.0f else 0.5f

        val filteredList = when (currentFilter) {
            "Available" -> allHousekeepers.filter { isHousekeeperActuallyAvailable(it) }
            "Unavailable" -> allHousekeepers.filter { !isHousekeeperActuallyAvailable(it) }
            else -> allHousekeepers
        }

        (rvHousekeepers.adapter as? HousekeeperSelectableAdapter)?.updateData(filteredList)
    }

    private fun isHousekeeperActuallyAvailable(hk: User): Boolean {
        if (tempDate.isEmpty() || tempTime.isEmpty()) return hk.status == "Available"
        
        val dayOfWeek = getDayOfWeek(tempDate) // e.g., "Monday"
        val dayShort = dayOfWeek.take(3) // e.g., "Mon"
        val selectedTimeStr = getDisplayTime(tempTime) // e.g., "9:00 AM"
        
        val availabilityStr = hk.availability as? String ?: return hk.status == "Available"
        
        // Expected format: "Mon, Tue, Wed, Thu, Fri 9:00 AM-5:00 PM"
        try {
            // Updated Regex to handle "Day, Day Day Time-Time"
            val regex = Regex("([a-zA-Z,\\s]+)\\s+(\\d{1,2}:\\d{2}\\s+[APM]{2})-(\\d{1,2}:\\d{2}\\s+[APM]{2})")
            val matchResult = regex.find(availabilityStr) ?: return hk.status == "Available"
            
            val daysPart = matchResult.groups[1]?.value ?: ""
            val startTimeStr = matchResult.groups[2]?.value ?: ""
            val endTimeStr = matchResult.groups[3]?.value ?: ""
            
            val isDayMatch = daysPart.contains(dayShort, ignoreCase = true)
            if (!isDayMatch) return false
            
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            val startTime = sdf.parse(startTimeStr)
            val endTime = sdf.parse(endTimeStr)
            val selectedTime = sdf.parse(selectedTimeStr)
            
            if (selectedTime != null && startTime != null && endTime != null) {
                return !selectedTime.before(startTime) && !selectedTime.after(endTime)
            }
        } catch (e: Exception) {
            return hk.status == "Available"
        }
        
        return hk.status == "Available"
    }

    private fun getDayOfWeek(dateStr: String): String {
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
            SimpleDateFormat("EEEE", Locale.getDefault()).format(date!!)
        } catch (e: Exception) { "" }
    }

    private fun getDisplayTime(time24h: String): String {
        return try {
            val date = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(time24h)
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(date!!)
        } catch (e: Exception) { "" }
    }

    private fun setupTimeSlots() {
        val times = listOf("9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM", "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM")
        layoutTimeSlots.removeAllViews()
        
        for (time in times) {
            val btn = LayoutInflater.from(requireContext()).inflate(R.layout.item_time_slot_button, layoutTimeSlots, false) as Button
            btn.text = time
            btn.setOnClickListener {
                for (i in 0 until layoutTimeSlots.childCount) {
                    layoutTimeSlots.getChildAt(i).isSelected = false
                }
                btn.isSelected = true
                etStartTime.setText(time)
                
                val sdf12 = SimpleDateFormat("h:mm a", Locale.getDefault())
                val sdf24 = SimpleDateFormat("HH:mm", Locale.getDefault())
                tempTime = sdf24.format(sdf12.parse(time)!!)
                
                refreshStaffAvailabilityCounts()
            }
            layoutTimeSlots.addView(btn)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val dialog = DatePickerDialog(requireContext(), { _, year, month, day ->
            val cal = Calendar.getInstance()
            cal.set(year, month, day)
            tempDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
            etStartDate.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time))
            refreshStaffAvailabilityCounts()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        dialog.datePicker.minDate = System.currentTimeMillis() - 1000
        dialog.show()
    }

    private fun loadAllStaff() {
        repository.getAllHousekeepers().addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<User>()
                for (child in snapshot.children) {
                    val user = child.getValue(User::class.java)?.copy(uid = child.key ?: "") ?: continue
                    list.add(user)
                }
                allHousekeepers = list
                
                if (rvHousekeepers.adapter == null) {
                    rvHousekeepers.adapter = HousekeeperSelectableAdapter(
                        housekeepers = emptyList(),
                        checkAvailability = { isHousekeeperActuallyAvailable(it) },
                        onHousekeeperSelected = { hk ->
                            isQuickBook = false
                            selectedHousekeeper = hk
                            viewModel.selectHousekeeper(hk)
                        },
                        onProfileDetails = { hk ->
                            val intent = Intent(requireContext(), ProfileViewActivity::class.java)
                            intent.putExtra("isReadOnly", true)
                            intent.putExtra("uid", hk.uid)
                            startActivity(intent)
                        }
                    )
                }
                refreshStaffAvailabilityCounts()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun refreshStaffAvailabilityCounts() {
        val availableCount = allHousekeepers.count { isHousekeeperActuallyAvailable(it) }
        val unavailableCount = allHousekeepers.size - availableCount
        
        tvFilterAvailable.text = "Available $availableCount"
        tvFilterUnavailable.text = "Unavailable $unavailableCount"
        tvFilterTotal.text = "Total ${allHousekeepers.size}"
        
        updateListByFilter()
    }

    private fun quickBookStaff() {
        if (tempDate.isEmpty() || tempTime.isEmpty()) {
            Toast.makeText(context, "Please select schedule first", Toast.LENGTH_SHORT).show()
            return
        }
        
        val availableStaff = allHousekeepers.filter { isHousekeeperActuallyAvailable(it) }
        if (availableStaff.isNotEmpty()) {
            selectedHousekeeper = availableStaff.random()
            viewModel.selectHousekeeper(selectedHousekeeper!!)
            proceedToReview()
        } else {
            Toast.makeText(context, "No staff available for this slot", Toast.LENGTH_SHORT).show()
        }
    }

    private fun proceedToReview() {
        (activity as? StartServiceActivity)?.navigateToStep(Step3ReviewFragment())
    }
}
