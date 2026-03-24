package com.example.houseclean.view

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.houseclean.R
import com.example.houseclean.viewmodel.LoginViewModel
import com.google.android.material.textfield.TextInputEditText
import android.util.Patterns

class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Set system bars color
        val navColor = Color.parseColor("#333A5B")
        window.statusBarColor = navColor
        window.navigationBarColor = navColor

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val emailField = findViewById<TextInputEditText>(R.id.textInputEditText)
        val passwordField = findViewById<TextInputEditText>(R.id.editText)
        val loginButton = findViewById<Button>(R.id.button)
        val rememberMeCheckbox = findViewById<CheckBox>(R.id.checkBox)
        val forgotPasswordText = findViewById<TextView>(R.id.textView6)
        val sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)

        if (sharedPreferences.getBoolean("rememberMe", false)) {
            emailField.setText(sharedPreferences.getString("email", ""))
            passwordField.setText(sharedPreferences.getString("password", ""))
            rememberMeCheckbox.isChecked = true
        }

        observeViewModel()

        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString()

            if (validateInputs(email, password, emailField, passwordField)) {
                viewModel.login(email, password, rememberMeCheckbox.isChecked, sharedPreferences)
            }
        }

        forgotPasswordText.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        findViewById<TextView>(R.id.signupBtn).setOnClickListener {
            startActivity(Intent(this, RegisterActivity1::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.loginResult.observe(this) { result ->
            result.onFailure {
                Toast.makeText(this, "Login Failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.userData.observe(this) { user ->
            if (user != null) {
                if (!viewModel.isEmailVerified()) {
                    startActivity(Intent(this, LoginEmailVerificationActivity::class.java))
                    finish()
                } else {
                    navigateToRolePage(user.role)
                }
            } else {
                // If login was successful but userData is null, it might be an issue with DB or data model
                // We check if a login result actually happened
                viewModel.loginResult.value?.onSuccess {
                    Toast.makeText(this, "User data not found. Please contact support.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateToRolePage(role: String?) {
        val intent = when (role) {
            "Householder" -> Intent(this, HouseholderPage::class.java)
            "Housekeeper" -> Intent(this, HousekeeperPage::class.java)
            else -> Intent(this, HouseholderPage::class.java) // Default fallback
        }
        startActivity(intent)
        finish()
    }

    private fun validateInputs(email: String, password: String, emailField: TextInputEditText, passwordField: TextInputEditText): Boolean {
        var isValid = true
        
        // Email Validation: format should have "@gmail.com" at the end
        if (email.isEmpty()) {
            emailField.error = "Email is required"
            isValid = false
        } else if (!email.endsWith("@gmail.com")) {
            emailField.error = "Email must end with @gmail.com"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.error = "Invalid email format"
            isValid = false
        } else {
            emailField.error = null
        }

        // Password Validation: 8-18 characters, no spaces
        if (password.isEmpty()) {
            passwordField.error = "Password is required"
            isValid = false
        } else if (password.contains(" ")) {
            passwordField.error = "Password cannot contain spaces"
            isValid = false
        } else if (password.length < 8 || password.length > 18) {
            passwordField.error = "Password must be 8-18 characters"
            isValid = false
        } else {
            passwordField.error = null
        }

        return isValid
    }
}
