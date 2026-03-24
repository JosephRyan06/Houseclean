package com.example.houseclean.view

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.houseclean.R
import com.example.houseclean.model.ServiceType
import java.util.Locale

class ServiceInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_info)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        toolbar.setNavigationOnClickListener { finish() }

        val service = intent.getParcelableExtra<ServiceType>("service")
        if (service != null) {
            displayServiceInfo(service)
        } else {
            finish()
        }
    }

    private fun displayServiceInfo(service: ServiceType) {
        findViewById<ImageView>(R.id.ivServiceDetailImage).setImageResource(service.iconResId)
        findViewById<TextView>(R.id.tvServiceDetailName).text = service.name
        findViewById<TextView>(R.id.tvServiceDetailDescription).text = service.description
        findViewById<TextView>(R.id.tvServiceDetailPrice).text = String.format(
            Locale.getDefault(), "PHP %.0f – %.0f", service.minPrice, service.maxPrice
        )
    }
}
