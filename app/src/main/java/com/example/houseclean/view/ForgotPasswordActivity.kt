package com.example.houseclean.view

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.houseclean.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        // Set system bars color
        val navColor = Color.parseColor("#333A5B")
        window.statusBarColor = navColor
        window.navigationBarColor = navColor

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val inputEmail = findViewById<TextInputEditText>(R.id.inputEmailReset)
        val btnSend = findViewById<Button>(R.id.btnSendReset)
        val btnBackToLogin = findViewById<TextView>(R.id.btnBackToLogin)

        btnSend.setOnClickListener {
            val email = inputEmail.text.toString().trim()

            if (email.isEmpty()) {
                inputEmail.error = "Email is required"
                inputEmail.requestFocus()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Reset link sent to your email", Toast.LENGTH_LONG).show()
                        finish() // Go back to login after sending
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        btnBackToLogin.setOnClickListener {
            finish()
        }
    }
}
