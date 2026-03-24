package com.example.houseclean.view

import android.content.Intent
import android.os.Bundle
import android.widget.AutoCompleteTextView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.houseclean.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity1 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_step_1)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Handle back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showCancelDialog()
            }
        })

        val lastNameLayout = findViewById<TextInputLayout>(R.id.layoutLastName)
        val lastNameInput = findViewById<TextInputEditText>(R.id.inputLastName)
        val firstNameLayout = findViewById<TextInputLayout>(R.id.layoutFirstName)
        val firstNameInput = findViewById<TextInputEditText>(R.id.inputFirstName)
        val barangayLayout = findViewById<TextInputLayout>(R.id.layoutBarangay)
        val barangayDropdown = findViewById<AutoCompleteTextView>(R.id.dropdownBarangay)
        val streetInput = findViewById<TextInputEditText>(R.id.inputStreet)
        val landmarkInput = findViewById<TextInputEditText>(R.id.inputLandmark)

        val barangayList = listOf(
            "Amado", "Balogo", "Binloc", "Bolosan", "Bonuan Boquig",
            "Bonuan Gueset", "Bonuan Blue Beach", "Calmay", "Carael",
            "Caranglaan", "Cebol", "Herrero", "Lasip Chico", "Lasip Grande",
            "Lucao", "Malued", "Mamalingling", "Mangin", "Mayombo", "Pantal",
            "Pogo Chico", "Pogo Grande", "Pugaro Suit", "Salisay", "Salapingao",
            "Tebeng", "Barangay I (T. Bugallon)", "Barangay II (New Quebec)",
            "Barangay III (Commercial District)", "Barangay IV (Commercial District)",
            "Barangay V (Commercial District)"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, barangayList)
        barangayDropdown.setAdapter(adapter)

        findViewById<Button>(R.id.buttonNext).setOnClickListener {
            val lastName = lastNameInput.text.toString().trim()
            val firstName = firstNameInput.text.toString().trim()
            val barangay = barangayDropdown.text.toString().trim()
            val street = streetInput.text.toString().trim()
            val landmark = landmarkInput.text.toString().trim()

            var isValid = true
            val nameRegex = "^[a-zA-Z\\s.',~`]{2,}$".toRegex()

            if (lastName.isEmpty() || !nameRegex.matches(lastName)) {
                lastNameLayout.error = "Invalid Last Name (min 2 letters, accepts .',~`)"
                isValid = false
            } else lastNameLayout.error = null

            if (firstName.isEmpty() || !nameRegex.matches(firstName)) {
                firstNameLayout.error = "Invalid First Name (min 2 letters, accepts .',~`)"
                isValid = false
            } else firstNameLayout.error = null

            if (barangay.isEmpty()) {
                barangayLayout.error = "Select Barangay"
                isValid = false
            } else barangayLayout.error = null

            if (street.isEmpty()) {
                findViewById<TextInputLayout>(R.id.layoutStreet).error = "Street is required"
                isValid = false
            } else findViewById<TextInputLayout>(R.id.layoutStreet).error = null

            if (landmark.isEmpty()) {
                findViewById<TextInputLayout>(R.id.layoutLandmark).error = "Landmark is required"
                isValid = false
            } else findViewById<TextInputLayout>(R.id.layoutLandmark).error = null

            if (isValid) {
                val intent = Intent(this, RegisterActivity2::class.java).apply {
                    putExtra("lastName", lastName)
                    putExtra("firstName", firstName)
                    putExtra("barangay", barangay)
                    putExtra("street", street)
                    putExtra("landmark", landmark)
                }
                startActivity(intent)
            }
        }
    }

    private fun showCancelDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cancel Registration")
            .setMessage("Are you sure you want to cancel your registration progress? All entered data will be lost.")
            .setPositiveButton("Yes") { _, _ ->
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }
}
