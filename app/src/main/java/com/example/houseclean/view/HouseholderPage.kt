package com.example.houseclean.view

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.houseclean.R
import com.example.houseclean.model.UserRepository
import com.example.houseclean.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class HouseholderPage : AppCompatActivity() {
    private val userRepository = UserRepository()
    private lateinit var tvInitials: TextView
    private lateinit var profileIconContainer: FrameLayout
    private lateinit var profileBg: View
    private lateinit var statusStroke: View
    
    private lateinit var navHome: LinearLayout
    private lateinit var navRequest: LinearLayout
    private lateinit var navPayment: LinearLayout
    private lateinit var navAlert: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_householder_page)
        
        // Set the system bars colors (Top and Bottom)
        val navColor = Color.parseColor("#333A5B")
        window.statusBarColor = navColor
        window.navigationBarColor = navColor
        
        val mainView = findViewById<View>(R.id.main)
        tvInitials = findViewById(R.id.tvProfileInitials)
        profileIconContainer = findViewById(R.id.profileIconContainer)
        profileBg = findViewById(R.id.profileBg)
        statusStroke = findViewById(R.id.viewStatusStroke)

        navHome = findViewById(R.id.nav_home_container)
        navRequest = findViewById(R.id.nav_request_container)
        navPayment = findViewById(R.id.nav_payment_container)
        navAlert = findViewById(R.id.nav_alert_container)

        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupProfileIcon()
        setupNavigation()

        if (savedInstanceState == null) {
            selectTab(R.id.nav_home_container)
        }
    }

    private fun setupProfileIcon() {
        val uid = userRepository.getCurrentUserUid() ?: return
        userRepository.getUserData(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    val initials = "${it.firstName.take(1)}${it.lastName.take(1)}".uppercase()
                    tvInitials.text = initials
                    
                    val colors = listOf("#E57373", "#F06292", "#BA68C8", "#9575CD", "#7986CB", "#64B5F6", "#4FC3F7", "#4DD0E1", "#4DB6AC", "#81C784", "#AED581", "#DCE775", "#FFF176", "#FFD54F", "#FFB74D", "#FF8A65")
                    val colorIndex = Math.abs(it.uid.hashCode()) % colors.size
                    val randomColor = Color.parseColor(colors[colorIndex])
                    (profileBg.background as? GradientDrawable)?.setColor(randomColor)
                    
                    val strokeColor = if (it.status.equals("active", ignoreCase = true)) Color.parseColor("#2DCC91") else Color.parseColor("#888888")
                    (statusStroke.background as? GradientDrawable)?.setStroke(6, strokeColor)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        profileIconContainer.setOnClickListener {
            // Navigate to ProfileViewActivity instead of showing a fragment
            startActivity(Intent(this, ProfileViewActivity::class.java))
        }
    }

    private fun setupNavigation() {
        navHome.setOnClickListener { selectTab(R.id.nav_home_container) }
        navRequest.setOnClickListener { selectTab(R.id.nav_request_container) }
        navPayment.setOnClickListener { selectTab(R.id.nav_payment_container) }
        navAlert.setOnClickListener { selectTab(R.id.nav_alert_container) }
    }

    private fun selectTab(containerId: Int, shouldNavigate: Boolean = true) {
        resetAllTabs()
        val container = findViewById<LinearLayout>(containerId)
        val indicator = container.getChildAt(0)
        val content = container.getChildAt(1) as LinearLayout
        val icon = content.getChildAt(0) as ImageView
        val text = content.getChildAt(1) as TextView

        indicator.visibility = View.VISIBLE
        content.setBackgroundResource(R.drawable.nav_selected_bg_square)
        icon.setColorFilter(Color.parseColor("#017497"))
        text.setTextColor(Color.parseColor("#017497"))

        if (shouldNavigate) {
            when (containerId) {
                R.id.nav_home_container -> replaceFragment(HomeFragment())
                R.id.nav_request_container -> replaceFragment(RequestFragment())
                R.id.nav_payment_container -> replaceFragment(PaymentFragment())
                R.id.nav_alert_container -> replaceFragment(NotificationFragment())
            }
        }
    }

    private fun resetAllTabs() {
        val navIds = listOf(R.id.nav_home_container, R.id.nav_request_container, R.id.nav_payment_container, R.id.nav_alert_container)
        for (id in navIds) {
            val container = findViewById<LinearLayout>(id)
            val indicator = container.getChildAt(0)
            val content = container.getChildAt(1) as LinearLayout
            val icon = content.getChildAt(0) as ImageView
            val text = content.getChildAt(1) as TextView

            indicator.visibility = View.INVISIBLE
            content.background = null
            icon.setColorFilter(Color.parseColor("#888888"))
            text.setTextColor(Color.parseColor("#888888"))
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .setReorderingAllowed(true)
            .commit()
    }
}
