package com.example.houseclean.view

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.houseclean.R
import com.example.houseclean.model.User
import com.google.android.material.card.MaterialCardView
import java.util.Locale

class HousekeeperSelectableAdapter(
    private var housekeepers: List<User>,
    private val checkAvailability: (User) -> Boolean,
    private val onHousekeeperSelected: (User) -> Unit,
    private val onProfileDetails: (User) -> Unit
) : RecyclerView.Adapter<HousekeeperSelectableAdapter.ViewHolder>() {

    private var selectedPosition = -1

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvSelectableHKName)
        val tvContact: TextView = view.findViewById(R.id.tvSelectableHKContact)
        val tvExperience: TextView = view.findViewById(R.id.tvSelectableHKExperience)
        val rbRating: RatingBar = view.findViewById(R.id.rbSelectableHK)
        val tvRatings: TextView = view.findViewById(R.id.tvSelectableHKRatings)
        val btnProfileDetails: Button = view.findViewById(R.id.btnProfileDetails)
        val vProfileBg: View = view.findViewById(R.id.vProfileBg)
        val tvProfileInitial: TextView = view.findViewById(R.id.tvProfileInitial)
        val tvStatus: TextView = view.findViewById(R.id.tvAvailabilityStatus)
        val vStatusDot: View = view.findViewById(R.id.vAvailabilityDot)

        fun bind(hk: User, position: Int) {
            val isActuallyAvailable = checkAvailability(hk)
            val displayName = hk.getDisplayName()
            tvName.text = displayName.lowercase()
            tvContact.text = "Contact: ${if (hk.contact.isNotBlank()) hk.contact else hk.phone.ifBlank { "N/A" }}"
            tvExperience.text = "${hk.experience.ifBlank { "0" }} yrs experience"
            
            if (hk.ratingCount > 0) {
                rbRating.visibility = View.VISIBLE
                rbRating.rating = hk.ratingAverage.toFloat()
                tvRatings.text = String.format(Locale.getDefault(), "%.1f (%d)", hk.ratingAverage, hk.ratingCount)
            } else {
                rbRating.visibility = View.GONE
                tvRatings.text = "No ratings yet"
            }

            val initial = if (displayName.isNotBlank()) displayName.take(1).uppercase() else "?"
            tvProfileInitial.text = initial
            
            val colors = listOf("#017497", "#2DCC91", "#F48B29", "#E74C3C", "#9B59B6")
            val colorIndex = Math.abs(hk.uid.hashCode()) % colors.size
            val color = Color.parseColor(colors[colorIndex])
            
            val background = vProfileBg.background as? GradientDrawable
            background?.setColor(color)

            // Dynamic availability status text and color
            tvStatus.text = if (isActuallyAvailable) "Available for your slot" else "Unavailable"
            val statusColor = Color.parseColor(if (isActuallyAvailable) "#2DCC91" else "#F48B29")
            tvStatus.setTextColor(statusColor)
            vStatusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(statusColor)

            itemView.isSelected = position == selectedPosition
            val cardView = itemView as? MaterialCardView
            if (position == selectedPosition) {
                cardView?.setCardBackgroundColor(Color.parseColor("#E0F7FA"))
                cardView?.strokeWidth = 4
                cardView?.strokeColor = Color.parseColor("#017497")
            } else {
                cardView?.setCardBackgroundColor(Color.parseColor("#F8F9FA"))
                cardView?.strokeWidth = 0
            }

            // Only selectable if actually available
            itemView.setOnClickListener {
                if (isActuallyAvailable) {
                    val prevSelected = selectedPosition
                    selectedPosition = adapterPosition
                    notifyItemChanged(prevSelected)
                    notifyItemChanged(selectedPosition)
                    onHousekeeperSelected(hk)
                }
            }

            btnProfileDetails.setOnClickListener {
                onProfileDetails(hk)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_housekeeper_selectable, parent, false)
        
        // Set card width to fill screen with padding
        val layoutParams = view.layoutParams
        layoutParams.width = (parent.width * 0.85).toInt() 
        view.layoutParams = layoutParams

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(housekeepers[position], position)
    }

    override fun getItemCount() = housekeepers.size

    fun updateData(newList: List<User>) {
        housekeepers = newList
        selectedPosition = -1
        notifyDataSetChanged()
    }
}
