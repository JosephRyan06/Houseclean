package com.example.houseclean.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.houseclean.R
import com.example.houseclean.model.PriceRange
import com.example.houseclean.model.ServiceType
import com.example.houseclean.view.adapter.ServiceTypeAdapter
import com.example.houseclean.viewmodel.StartServiceViewModel
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

class Step1PlanFragment : Fragment() {

    private val viewModel: StartServiceViewModel by activityViewModels()
    private var selectedService: ServiceType? = null

    private lateinit var tvEstimatedPriceValue: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_step1_plan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvServices = view.findViewById<RecyclerView>(R.id.rvServices)
        val rgPaymentMethod = view.findViewById<RadioGroup>(R.id.rgPaymentMethod)
        val etNotes = view.findViewById<TextInputEditText>(R.id.etNotes)
        tvEstimatedPriceValue = view.findViewById(R.id.tvEstimatedPriceValue)
        val btnNext = view.findViewById<Button>(R.id.btnNextStep1)

        val services = listOf(
            ServiceType(
                "General Housecleaning", 500.0, 1500.0, R.drawable.general_housecleaning,
                "Covers sweeping, mopping, dusting, basic tidying. Usually 2–3 hours for small homes/condos."
            ),
            ServiceType(
                "Kitchen Cleaning", 800.0, 2000.0, R.drawable.kitchen_cleaning,
                "Includes grease removal, appliance wipe-down, sink scrubbing. Higher if deep cleaning ovens/fridges."
            ),
            ServiceType(
                "Bathroom Cleaning", 700.0, 1500.0, R.drawable.bathroom_cleaning,
                "Focus on tiles, toilet, shower, mirrors. Mold removal may add cost."
            ),
            ServiceType(
                "Bedroom Cleaning", 500.0, 1200.0, R.drawable.bedroom_cleaning,
                "Dusting, vacuuming, bed changing. Price depends on number of rooms."
            ),
            ServiceType(
                "Deep Cleaning", 2000.0, 5000.0, R.drawable.deep_cleaning,
                "Intensive cleaning of entire property, including hidden areas, appliances, and disinfecting."
            ),
            ServiceType(
                "Outdoor Cleaning", 1000.0, 3000.0, R.drawable.outdoor_cleaning,
                "Garden sweeping, garage cleaning, exterior walls."
            ),
            ServiceType(
                "Appliance Cleaning", 500.0, 1500.0, R.drawable.appliance_cleaning,
                "Fridge, oven, aircon. Price depends on size and dirt level."
            )
        )

        rvServices.layoutManager = GridLayoutManager(requireContext(), 2)
        val adapter = ServiceTypeAdapter(
            services,
            onServiceSelected = { service ->
                selectedService = service
                tvEstimatedPriceValue.text = String.format(
                    Locale.getDefault(), "PHP %.0f – %.0f", service.minPrice, service.maxPrice
                )
                viewModel.updateServiceType(service.name, PriceRange(service.minPrice, service.maxPrice))
                viewModel.updateDuration(1, service.minPrice) // Default to min price
            },
            onViewDetails = { service ->
                val intent = Intent(requireContext(), ServiceInfoActivity::class.java)
                intent.putExtra("service", service)
                startActivity(intent)
            }
        )
        rvServices.adapter = adapter

        btnNext.setOnClickListener {
            val paymentMethod = when (rgPaymentMethod.checkedRadioButtonId) {
                R.id.rbStaticQR -> "STATIC_QR"
                R.id.rbCashOnHand -> "CASH_ON_HAND"
                else -> ""
            }

            if (selectedService == null) {
                Toast.makeText(context, "Please choose a service", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (paymentMethod.isEmpty()) {
                Toast.makeText(context, "Please choose a payment method", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val notes = etNotes.text?.toString() ?: ""
            viewModel.updatePaymentAndNotes(paymentMethod, notes)
            (activity as? StartServiceActivity)?.navigateToStep(Step2ScheduleFragment())
        }
    }
}
