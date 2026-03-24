package com.example.houseclean.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.houseclean.R
import com.example.houseclean.model.ServiceRequest
import java.text.SimpleDateFormat
import java.util.*

class ServiceRequestAdapter(
    private var requests: List<ServiceRequest>,
    private val onConfirm: (ServiceRequest) -> Unit,
    private val onDecline: (ServiceRequest) -> Unit
) : RecyclerView.Adapter<ServiceRequestAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHouseholderName: TextView = view.findViewById(R.id.tvReqHouseholderName)
        val tvServiceType: TextView = view.findViewById(R.id.tvReqServiceType)
        val tvSalary: TextView = view.findViewById(R.id.tvReqSalary)
        val tvDateTime: TextView = view.findViewById(R.id.tvReqDateTime)
        val btnConfirm: Button = view.findViewById(R.id.btnConfirm)
        val btnDecline: Button = view.findViewById(R.id.btnDecline)
        val tvInitials: TextView = view.findViewById(R.id.tvProfileInitials)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]
        holder.tvHouseholderName.text = request.householderName
        holder.tvServiceType.text = request.serviceType
        holder.tvSalary.text = "PHP ${request.totalPrice.toInt()}"
        holder.tvDateTime.text = formatSchedule(request.startDate)
        
        // Extract initials
        val names = request.householderName.split(" ")
        val initials = if (names.size >= 2) {
            "${names[0].take(1)}${names[names.size-1].take(1)}".uppercase()
        } else if (names.isNotEmpty()) {
            names[0].take(1).uppercase()
        } else "LF"
        holder.tvInitials.text = initials

        holder.btnConfirm.setOnClickListener { onConfirm(request) }
        holder.btnDecline.setOnClickListener { onDecline(request) }
    }

    private fun formatSchedule(dateStr: String): String {
        return try {
            val date = if (dateStr.contains("T")) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(dateStr)
            } else {
                SimpleDateFormat("d/M/yyyy", Locale.getDefault()).parse(dateStr)
            }
            SimpleDateFormat("MMMM d, yyyy, h:mm a", Locale.getDefault()).format(date!!)
        } catch (e: Exception) {
            dateStr
        }
    }

    override fun getItemCount() = requests.size

    fun updateList(newRequests: List<ServiceRequest>) {
        requests = newRequests
        notifyDataSetChanged()
    }
}
