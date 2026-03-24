package com.example.houseclean.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.houseclean.R
import com.example.houseclean.model.User
import com.example.houseclean.viewmodel.RegisterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EmailVerificationActivity : AppCompatActivity() {

    private val viewModel: RegisterViewModel by viewModels()
    private lateinit var statusLabel: TextView
    private lateinit var checkStatusButton: Button
    private lateinit var resendEmailButton: Button
    private var isAlreadyRegistered: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_email_verification)

        isAlreadyRegistered = intent.getBooleanExtra("isAlreadyRegistered", false)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showCancelDialog()
            }
        })

        statusLabel = findViewById(R.id.tvStatusLabel)
        checkStatusButton = findViewById(R.id.buttonCheckStatus)
        resendEmailButton = findViewById(R.id.buttonResendEmail)

        // Only register if not already registered (i.e. coming from registration flow)
        if (!isAlreadyRegistered) {
            completeRegistration()
        } else {
            // If already registered, we just need to send/resend verification if it hasn't been sent
            // or just let the user check status.
            Toast.makeText(this, "Please verify your email to continue.", Toast.LENGTH_SHORT).show()
        }
        
        observeViewModel()

        checkStatusButton.setOnClickListener {
            if (viewModel.checkEmailVerificationStatus()) {
                Toast.makeText(this, "Email Verified Successfully!", Toast.LENGTH_SHORT).show()
                if (isAlreadyRegistered) {
                    navigateToMain()
                } else {
                    navigateToLogin()
                }
            } else {
                Toast.makeText(this, "Email not yet verified. Please check your inbox.", Toast.LENGTH_SHORT).show()
            }
        }

        resendEmailButton.setOnClickListener {
            viewModel.resendVerificationEmail()
        }
    }

    private fun completeRegistration() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        val user = User(
            firstName = intent.getStringExtra("firstName") ?: "",
            lastName = intent.getStringExtra("lastName") ?: "",
            email = intent.getStringExtra("email") ?: "",
            phone = intent.getStringExtra("phone") ?: "",
            barangay = intent.getStringExtra("barangay") ?: "",
            street = intent.getStringExtra("street") ?: "",
            landmark = intent.getStringExtra("landmark") ?: "",
            role = intent.getStringExtra("role") ?: "Householder",
            status = "active",
            joined = currentDate
        )
        val password = intent.getStringExtra("password") ?: ""
        viewModel.register(user, password)
    }

    private fun observeViewModel() {
        viewModel.saveUserResult.observe(this) { result ->
            result.onFailure {
                Toast.makeText(this, "Error saving user data: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.verificationEmailSent.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Verification email sent!", Toast.LENGTH_SHORT).show()
            }
            result.onFailure {
                Toast.makeText(this, "Failed to send email: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    private fun showCancelDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cancel Verification")
            .setMessage(if (isAlreadyRegistered) "You will be logged out if you don't verify." else "Are you sure you want to cancel? You will need to start the registration process again.")
            .setPositiveButton("Yes") { _, _ ->
                if (isAlreadyRegistered) {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                }
                finishAffinity()
                startActivity(Intent(this, LoginActivity::class.java))
            }
            .setNegativeButton("No", null)
            .show()
    }
}
