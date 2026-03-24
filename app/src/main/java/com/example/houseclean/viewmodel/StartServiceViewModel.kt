package com.example.houseclean.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.houseclean.model.PriceRange
import com.example.houseclean.model.ServiceRequest
import com.example.houseclean.model.User
import com.example.houseclean.model.UserRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class StartServiceViewModel : ViewModel() {
    private val repository = UserRepository()
    private val _request = MutableLiveData<ServiceRequest>(ServiceRequest())
    val request: LiveData<ServiceRequest> = _request

    private val _selectedHousekeeper = MutableLiveData<User?>()
    val selectedHousekeeper: LiveData<User?> = _selectedHousekeeper

    private val _availableHousekeepers = MutableLiveData<List<User>>()
    val availableHousekeepers: LiveData<List<User>> = _availableHousekeepers

    fun updateServiceType(type: String, priceRange: PriceRange) {
        _request.value = _request.value?.copy(
            serviceType = type,
            priceRange = priceRange
        )
    }

    fun updateDuration(hours: Int, price: Double) {
        _request.value = _request.value?.copy(durationHours = hours, totalPrice = price)
    }

    fun updatePaymentAndNotes(method: String, notes: String) {
        _request.value = _request.value?.copy(paymentMethod = method, notes = notes)
    }

    fun updateSchedule(dateTime: String) {
        _request.value = _request.value?.copy(startDate = dateTime)
    }

    fun selectHousekeeper(housekeeper: User) {
        _selectedHousekeeper.value = housekeeper
        _request.value = _request.value?.copy(
            housekeeperId = housekeeper.uid,
            housekeeperName = "${housekeeper.lastName}, ${housekeeper.firstName}"
        )
    }

    fun fetchAvailableHousekeepers() {
        repository.getAllUsers().addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val housekeepers = mutableListOf<User>()
                for (child in snapshot.children) {
                    val user = child.getValue(User::class.java)
                    if (user?.role == "Housekeeper") {
                        housekeepers.add(user)
                    }
                }
                _availableHousekeepers.value = housekeepers
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
