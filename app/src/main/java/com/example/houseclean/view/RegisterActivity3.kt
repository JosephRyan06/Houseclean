package com.example.houseclean.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.houseclean.R

class RegisterActivity3 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_step_3)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showCancelDialog()
            }
        })

        val lastName = intent.getStringExtra("lastName") ?: ""
        val firstName = intent.getStringExtra("firstName") ?: ""
        val barangay = intent.getStringExtra("barangay") ?: ""
        val street = intent.getStringExtra("street") ?: ""
        val landmark = intent.getStringExtra("landmark") ?: ""
        val email = intent.getStringExtra("email") ?: ""
        val phone = intent.getStringExtra("phone") ?: ""
        val password = intent.getStringExtra("password") ?: ""

        findViewById<TextView>(R.id.tvReviewHouseholder).text = "Householder: $lastName, $firstName"
        findViewById<TextView>(R.id.tvReviewAddress).text = "Home Address: $street, $barangay, $landmark"
        findViewById<TextView>(R.id.tvReviewEmail).text = "Email: $email"
        findViewById<TextView>(R.id.tvReviewPassword).text = "Password: " + "*".repeat(password.length)

        findViewById<Button>(R.id.buttonRegister).setOnClickListener {
            val intent = Intent(this, EmailVerificationActivity::class.java).apply {
                putExtras(this@RegisterActivity3.intent)
            }
            startActivity(intent)
        }
    }

    private fun showCancelDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cancel Registration")
            .setMessage("Are you sure you want to cancel your registration progress? All entered data will be lost.")
            .setPositiveButton("Yes") { _, _ ->
                finishAffinity()
                startActivity(Intent(this, LoginActivity::class.java))
            }
            .setNegativeButton("No", null)
            .show()
    }
}
