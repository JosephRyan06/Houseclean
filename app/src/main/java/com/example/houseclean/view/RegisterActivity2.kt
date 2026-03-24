package com.example.houseclean.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.houseclean.R
import com.example.houseclean.viewmodel.RegisterViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity2 : AppCompatActivity() {

    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_step_2)

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

        val layoutEmail = findViewById<TextInputLayout>(R.id.layoutEmail)
        val layoutPhone = findViewById<TextInputLayout>(R.id.layoutPhone)
        val layoutPassword = findViewById<TextInputLayout>(R.id.layoutPassword)
        val layoutConfirmPassword = findViewById<TextInputLayout>(R.id.layoutConfirmPassword)

        val inputEmail = findViewById<TextInputEditText>(R.id.inputEmail)
        val inputPhone = findViewById<TextInputEditText>(R.id.inputPhone)
        val inputPassword = findViewById<TextInputEditText>(R.id.inputPassword)
        val inputConfirmPassword = findViewById<TextInputEditText>(R.id.inputConfirmPassword)

        observeEmailCheck(layoutEmail)

        findViewById<Button>(R.id.buttonNext).setOnClickListener {
            val emailPrefix = inputEmail.text.toString().trim()
            val phone = inputPhone.text.toString().trim()
            val password = inputPassword.text.toString()
            val confirmPassword = inputConfirmPassword.text.toString()

            var isValid = true

            if (emailPrefix.isEmpty()) {
                layoutEmail.error = "Email required"
                isValid = false
            } else if (emailPrefix.contains("@")) {
                layoutEmail.error = "Do not include @gmail.com"
                isValid = false
            } else layoutEmail.error = null

            // Phone validation
            if (phone.isEmpty()) {
                layoutPhone.error = "Phone required"
                isValid = false
            } else if (!(phone.startsWith("09") && phone.length == 11) && !phone.startsWith("63+")) {
                layoutPhone.error = "Invalid phone format (09... or 63+)"
                isValid = false
            } else layoutPhone.error = null

            // Password validation: 8-18 characters, no spaces
            if (password.isEmpty()) {
                layoutPassword.error = "Password required"
                isValid = false
            } else if (password.contains(" ")) {
                layoutPassword.error = "Password cannot contain spaces"
                isValid = false
            } else if (password.length < 8 || password.length > 18) {
                layoutPassword.error = "Must be 8-18 characters"
                isValid = false
            } else {
                layoutPassword.error = null
            }

            if (password != confirmPassword) {
                layoutConfirmPassword.error = "Passwords do not match"
                isValid = false
            } else layoutConfirmPassword.error = null

            if (isValid) {
                val fullEmail = "$emailPrefix@gmail.com"
                viewModel.checkEmailExists(fullEmail)
            }
        }
    }

    private fun observeEmailCheck(layoutEmail: TextInputLayout) {
        viewModel.emailExists.observe(this) { exists ->
            if (exists) {
                layoutEmail.error = "Email already registered"
            } else {
                val emailPrefix = findViewById<TextInputEditText>(R.id.inputEmail).text.toString().trim()
                val phone = findViewById<TextInputEditText>(R.id.inputPhone).text.toString().trim()
                val password = findViewById<TextInputEditText>(R.id.inputPassword).text.toString()
                
                val intent = Intent(this, RegisterActivity3::class.java).apply {
                    putExtras(this@RegisterActivity2.intent)
                    putExtra("email", "$emailPrefix@gmail.com")
                    putExtra("phone", phone)
                    putExtra("password", password)
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
                finishAffinity() // Closes all activities in the registration stack
                startActivity(Intent(this, LoginActivity::class.java))
            }
            .setNegativeButton("No", null)
            .show()
    }
}
