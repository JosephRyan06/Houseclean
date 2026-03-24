package com.example.houseclean.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.houseclean.R
import com.example.houseclean.model.ServiceType
import java.util.Locale

class ServiceTypeAdapter(
    private val services: List<ServiceType>,
    private val onServiceSelected: (ServiceType) -> Unit,
    private val onViewDetails: (ServiceType) -> Unit
) : RecyclerView.Adapter<ServiceTypeAdapter.ViewHolder>() {

    private var selectedPosition = -1

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.ivServiceIcon)
        val tvName: TextView = view.findViewById(R.id.tvServiceName)
        val tvPrice: TextView = view.findViewById(R.id.tvServicePrice)
        val btnViewDetails: TextView = view.findViewById(R.id.btnViewDetails)
        val selectionIndicator: View = view.findViewById(R.id.selectionIndicator)

        fun bind(service: ServiceType, position: Int) {
            image.setImageResource(service.iconResId)
            tvName.text = service.name
            tvPrice.text = String.format(Locale.getDefault(), "P%.0f – P%.0f", service.minPrice, service.maxPrice)
            
            selectionIndicator.visibility = if (position == selectedPosition) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                val oldPosition = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)
                onServiceSelected(service)
            }

            btnViewDetails.setOnClickListener {
                onViewDetails(service)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_service_type_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(services[position], position)
    }

    override fun getItemCount() = services.size
}
