package com.example.houseclean.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.houseclean.R
import com.example.houseclean.model.UserRepository
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginEmailVerificationActivity : AppCompatActivity() {

    private val repository = UserRepository()
    private lateinit var emailField: TextInputEditText
    private lateinit var statusLabel: TextView
    private lateinit var checkStatusButton: Button
    private lateinit var resendButton: Button
    private lateinit var logoutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_email_verification)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        emailField = findViewById(R.id.etEmail)
        statusLabel = findViewById(R.id.tvStatusLabel)
        checkStatusButton = findViewById(R.id.buttonCheckStatus)
        resendButton = findViewById(R.id.buttonResendEmail)
        logoutButton = findViewById(R.id.buttonLogout)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            navigateToLogin()
            return
        }
        
        emailField.setText(currentUser.email)

        checkStatusButton.setOnClickListener {
            checkVerificationStatus()
        }

        resendButton.setOnClickListener {
            handleResendOrUpdate()
        }

        logoutButton.setOnClickListener {
            repository.logout()
            navigateToLogin()
        }
    }

    private fun checkVerificationStatus() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        checkStatusButton.isEnabled = false
        statusLabel.text = "Status: Checking..."
        
        currentUser.reload().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (currentUser.isEmailVerified) {
                    Toast.makeText(this, "Email Verified Successfully! Please log in again.", Toast.LENGTH_LONG).show()
                    
                    val uid = currentUser.uid
                    // Update flags in database
                    repository.updateUserData(uid, mapOf("needsVerification" to false))
                    
                    // Sign out and navigate to login as requested
                    repository.logout()
                    navigateToLogin()
                } else {
                    checkStatusButton.isEnabled = true
                    statusLabel.text = "Status: Not yet verified"
                    Toast.makeText(this, "Email not yet verified. Please check your inbox.", Toast.LENGTH_SHORT).show()
                }
            } else {
                checkStatusButton.isEnabled = true
                statusLabel.text = "Status: Error checking verification"
                Toast.makeText(this, "Failed to refresh status: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleResendOrUpdate() {
        val newEmail = emailField.text.toString().trim()
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        if (newEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches() || !newEmail.endsWith("@gmail.com")) {
            emailField.error = "Please enter a valid @gmail.com email"
            return
        }

        if (newEmail != currentUser.email) {
            // Update email in Firebase Auth
            currentUser.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Update email in Realtime Database
                    repository.updateUserData(currentUser.uid, mapOf("email" to newEmail))
                    Toast.makeText(this, "Verification email sent to $newEmail. Please verify to update your email.", Toast.LENGTH_LONG).show()
                    statusLabel.text = "Status: Verification sent to $newEmail"
                } else {
                    Toast.makeText(this, "Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            sendVerification()
        }
    }

    private fun sendVerification() {
        repository.sendEmailVerification()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Verification email resent to ${emailField.text}", Toast.LENGTH_SHORT).show()
                statusLabel.text = "Status: Verification email resent"
            } else {
                Toast.makeText(this, "Failed to send email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
