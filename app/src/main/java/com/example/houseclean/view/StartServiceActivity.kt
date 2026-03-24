package com.example.houseclean.view

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.houseclean.R
import com.example.houseclean.viewmodel.StartServiceViewModel

class StartServiceActivity : AppCompatActivity() {

    val viewModel: StartServiceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_start_service)

        // Set system bars color and handle insets
        window.statusBarColor = Color.parseColor("#333A5B")
        window.navigationBarColor = Color.parseColor("#333A5B")
        
        val mainView = findViewById<View>(R.id.main_layout)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Handle system back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount <= 1) {
                    showExitConfirmationDialog()
                } else {
                    supportFragmentManager.popBackStack()
                }
            }
        })

        if (savedInstanceState == null) {
            navigateToStep(Step1PlanFragment())
        }
    }

    fun navigateToStep(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.stepContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cancel Service")
            .setMessage("Are you sure you want to cancel setting up this service? All inputs will be lost.")
            .setPositiveButton("Yes") { _, _ ->
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }
}
