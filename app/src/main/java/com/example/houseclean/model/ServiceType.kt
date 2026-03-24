package com.example.houseclean.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ServiceType(
    val name: String,
    val minPrice: Double,
    val maxPrice: Double,
    val iconResId: Int,
    val description: String,
    var isSelected: Boolean = false
) : Parcelable
