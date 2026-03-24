package com.example.houseclean.view

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.houseclean.R
import com.example.houseclean.model.ServiceRepository
import com.example.houseclean.model.User
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class ProfileEditActivity : AppCompatActivity() {

    private val repository = ServiceRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private lateinit var etLastName: EditText
    private lateinit var etFirstName: EditText
    private lateinit var autoCompleteBarangay: AutoCompleteTextView
    private lateinit var etStreet: EditText
    private lateinit var etLandmark: EditText
    
    // Account fields
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var tvVerificationStatus: TextView
    private lateinit var btnVerifyEmailNow: Button
    private lateinit var btnVerifyEmailChange: Button
    private lateinit var btnChangePassword: MaterialButton
    
    // Layouts
    private lateinit var layoutPersonalFields: LinearLayout
    private lateinit var layoutAccountFields: LinearLayout
    
    // Housekeeper fields
    private lateinit var layoutHKFields: LinearLayout
    private lateinit var etAvailabilityDays: EditText
    private lateinit var etAvailabilityTime: EditText
    private lateinit var etSkills: EditText
    private lateinit var etExperience: EditText
    
    private var originalEmail: String = ""
    private var isHouseholder: Boolean = false

    private val barangays = listOf("Barangay 1", "Barangay 2", "Barangay 3", "Barangay 4", "Barangay 5")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_edit)

        // Set system bars color
        val navColor = Color.parseColor("#333A5B")
        window.statusBarColor = navColor
        window.navigationBarColor = navColor

        val mainView = findViewById<View>(R.id.profile_edit_main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Views
        etLastName = findViewById(R.id.etEditLastName)
        etFirstName = findViewById(R.id.etEditFirstName)
        autoCompleteBarangay = findViewById(R.id.dropdownEditBarangay)
        etStreet = findViewById(R.id.etEditStreet)
        etLandmark = findViewById(R.id.etEditLandmark)
        
        etEmail = findViewById(R.id.etEditEmail)
        etPhone = findViewById(R.id.etEditPhone)
        tvVerificationStatus = findViewById(R.id.tvVerificationStatus)
        btnVerifyEmailNow = findViewById(R.id.btnVerifyEmailNow)
        btnVerifyEmailChange = findViewById(R.id.btnVerifyEmailChange)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        
        layoutPersonalFields = findViewById(R.id.layoutPersonalFields)
        layoutAccountFields = findViewById(R.id.layoutAccountFields)
        
        layoutHKFields = findViewById(R.id.layoutHousekeeperFields)
        etAvailabilityDays = findViewById(R.id.etEditAvailabilityDays)
        etAvailabilityTime = findViewById(R.id.etEditAvailabilityTime)
        etSkills = findViewById(R.id.etEditSkills)
        etExperience = findViewById(R.id.etEditExperience)

        setupBarangayDropdown()
        loadUserData()
        checkEmailVerification()

        findViewById<MaterialButton>(R.id.btnDoneProfile).setOnClickListener {
            saveProfile()
        }

        btnVerifyEmailNow.setOnClickListener {
            sendVerificationEmail()
        }

        btnVerifyEmailChange.setOnClickListener {
            updateEmailAndVerify()
        }

        btnChangePassword.setOnClickListener {
            sendPasswordResetEmail()
        }

        etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val newEmail = s.toString().trim()
                if (isHouseholder && newEmail != originalEmail && newEmail.isNotEmpty()) {
                    btnVerifyEmailChange.visibility = View.VISIBLE
                } else {
                    btnVerifyEmailChange.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<TabLayout>(R.id.tabLayoutProfile).addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        layoutPersonalFields.visibility = View.VISIBLE
                        layoutAccountFields.visibility = View.GONE
                    }
                    1 -> {
                        layoutPersonalFields.visibility = View.GONE
                        layoutAccountFields.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupBarangayDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, barangays)
        autoCompleteBarangay.setAdapter(adapter)
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        repository.getUserData(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java) ?: return
                
                etLastName.setText(user.lastName)
                etFirstName.setText(user.firstName)
                etStreet.setText(user.street)
                etLandmark.setText(user.landmark)
                etEmail.setText(user.email)
                etPhone.setText(user.phone)
                
                originalEmail = user.email
                isHouseholder = user.role == "Householder"

                if (user.barangay.isNotEmpty()) {
                    autoCompleteBarangay.setText(user.barangay, false)
                }

                findViewById<TextView>(R.id.tvProfileNameEdit).text = "${user.lastName}, ${user.firstName}"
                findViewById<TextView>(R.id.tvProfileRoleEdit).text = user.role
                findViewById<TextView>(R.id.tvProfileInitialsEdit).text = if (user.firstName.isNotEmpty() && user.lastName.isNotEmpty()) {
                    "${user.firstName[0]}${user.lastName[0]}".uppercase()
                } else "HK"
                
                val profileBg = findViewById<View>(R.id.profileBgEdit)
                val colors = listOf("#E57373", "#F06292", "#BA68C8", "#9575CD", "#7986CB", "#64B5F6", "#4FC3F7", "#4DD0E1", "#4DB6AC", "#81C784", "#AED581", "#DCE775", "#FFF176", "#FFD54F", "#FFB74D", "#FF8A65")
                val colorIndex = Math.abs(user.uid.hashCode()) % colors.size
                (profileBg.background as? GradientDrawable)?.setColor(Color.parseColor(colors[colorIndex]))

                // Handle Housekeeper specific fields
                if (user.role == "Housekeeper") {
                    layoutHKFields.visibility = View.VISIBLE
                    
                    // Availability format: "Days, Time"
                    val avail = user.availability as? String ?: ""
                    if (avail.contains(",")) {
                        val parts = avail.split(",")
                        etAvailabilityDays.setText(parts[0].trim())
                        etAvailabilityTime.setText(parts[1].trim())
                    } else {
                        etAvailabilityDays.setText(avail)
                    }
                    
                    etSkills.setText(user.getSkillsDisplay())
                    etExperience.setText(user.experience)
                } else {
                    layoutHKFields.visibility = View.GONE
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun checkEmailVerification() {
        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener {
            if (user.isEmailVerified) {
                tvVerificationStatus.text = "Email Verified"
                tvVerificationStatus.setTextColor(Color.parseColor("#2DCC91"))
                btnVerifyEmailNow.visibility = View.GONE
            } else {
                tvVerificationStatus.text = "Email Not Verified"
                tvVerificationStatus.setTextColor(Color.parseColor("#B00020"))
                btnVerifyEmailNow.visibility = View.VISIBLE
            }
        }
    }

    private fun sendVerificationEmail() {
        auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Verification email sent to ${auth.currentUser?.email}", Toast.LENGTH_LONG).show()
                btnVerifyEmailNow.isEnabled = false
                btnVerifyEmailNow.text = "Sent"
            } else {
                Toast.makeText(this, "Failed to send verification email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendPasswordResetEmail() {
        val email = auth.currentUser?.email ?: return
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Password reset email sent to $email", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Failed to send reset email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateEmailAndVerify() {
        val newEmail = etEmail.text.toString().trim()
        val user = auth.currentUser ?: return

        user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Verification link sent to $newEmail. Please verify to update your email.", Toast.LENGTH_LONG).show()
                btnVerifyEmailChange.visibility = View.GONE
            } else {
                Toast.makeText(this, "Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveProfile() {
        val uid = auth.currentUser?.uid ?: return
        val lastName = etLastName.text.toString().trim()
        val firstName = etFirstName.text.toString().trim()
        val barangay = autoCompleteBarangay.text.toString().trim()
        val street = etStreet.text.toString().trim()
        val landmark = etLandmark.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val currentEmail = etEmail.text.toString().trim()

        if (lastName.isEmpty() || firstName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = mutableMapOf<String, Any>(
            "lastName" to lastName,
            "firstName" to firstName,
            "barangay" to barangay,
            "street" to street,
            "landmark" to landmark,
            "fullName" to "$firstName $lastName",
            "phone" to phone
        )

        // Only update email in Realtime Database if it's actually verified/changed in Auth
        // For householder, we force verification first via updateEmailAndVerify()
        if (currentEmail != originalEmail) {
            if (!isHouseholder) {
                // For Housekeeper or others, we might allow direct update or follow same flow
                // But as per request, Householder specifically needs the button.
                // Let's update DB only if Auth matches.
                if (auth.currentUser?.email == currentEmail) {
                    updates["email"] = currentEmail
                } else {
                    // Revert UI to original if not verified/applied
                    etEmail.setText(originalEmail)
                }
            } else {
                // For Householder, if they didn't verify, we keep original in DB
                if (auth.currentUser?.email != currentEmail) {
                    etEmail.setText(originalEmail)
                } else {
                    updates["email"] = currentEmail
                }
            }
        }

        // Add housekeeper fields if visible
        if (layoutHKFields.visibility == View.VISIBLE) {
            val days = etAvailabilityDays.text.toString().trim()
            val time = etAvailabilityTime.text.toString().trim()
            updates["availability"] = "$days, $time"
            updates["skills"] = etSkills.text.toString().trim()
            updates["experience"] = etExperience.text.toString().trim()
        }

        repository.getUserData(uid).updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
