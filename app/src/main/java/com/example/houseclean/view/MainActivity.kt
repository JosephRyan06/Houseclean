package com.example.houseclean.view

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.houseclean.R
import com.example.houseclean.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance().reference
    private val highlightColor = Color.parseColor("#017497")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupIntroText()
        startLoadingAnimation()

        // Splash screen delay before redirection
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserAndNavigate()
        }, 3000) // 3 seconds delay
    }

    private fun setupIntroText() {
        val textViewIntro = findViewById<TextView>(R.id.textViewIntro)
        
        val intros = listOf(
            Pair("Cleaning services made simple.\nBook trusted housekeepers anytime.", listOf("simple", "trusted housekeepers")),
            Pair("Clean home. Easy booking.\nFind reliable housekeepers in minutes.", listOf("Clean home", "Easy booking", "reliable housekeepers")),
            Pair("Your home deserves a cleaner space.\nBook, schedule, and relax.", listOf("home", "cleaner space")),
            Pair("Connecting homes and hardworking hands.\nFind help or find work.", listOf("homes", "hardworking hands", "find help", "find work"))
        )

        val selectedIntro = intros[Random.nextInt(intros.size)]
        val spannable = SpannableString(selectedIntro.first)

        for (highlight in selectedIntro.second) {
            val startIndex = selectedIntro.first.indexOf(highlight)
            if (startIndex != -1) {
                spannable.setSpan(
                    ForegroundColorSpan(highlightColor),
                    startIndex,
                    startIndex + highlight.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        textViewIntro.text = spannable
    }

    private fun startLoadingAnimation() {
        val textViewLoading = findViewById<TextView>(R.id.textViewLoading)
        val fadeInOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_in) // Simple animation
        textViewLoading.startAnimation(fadeInOut)
    }

    private fun checkUserAndNavigate() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Force reload to get latest verification status
            currentUser.reload().addOnCompleteListener {
                if (it.isSuccessful && !currentUser.isEmailVerified) {
                    startActivity(Intent(this, LoginEmailVerificationActivity::class.java))
                    finish()
                } else {
                    fetchUserDataAndNavigate(currentUser.uid)
                }
            }
        } else {
            goToLogin()
        }
    }

    private fun fetchUserDataAndNavigate(uid: String) {
        database.child("Users").child(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val user = snapshot.getValue(User::class.java)
                    val role = user?.role ?: ""

                    when (role) {
                        "Householder" -> {
                            startActivity(Intent(this, HouseholderPage::class.java))
                            finish()
                        }
                        "Housekeeper" -> {
                            startActivity(Intent(this, HousekeeperPage::class.java))
                            finish()
                        }
                        else -> {
                            auth.signOut()
                            goToLogin()
                        }
                    }
                } else {
                    auth.signOut()
                    goToLogin()
                }
            }
            .addOnFailureListener {
                goToLogin()
            }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
