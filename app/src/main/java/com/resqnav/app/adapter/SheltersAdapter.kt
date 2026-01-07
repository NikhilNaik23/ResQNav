package com.resqnav.app.adapter

import android.content.res.ColorStateList
import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.resqnav.app.R
import com.resqnav.app.model.Shelter

class SheltersAdapter(
    private var shelters: List<Shelter>,
    private var userLocation: Location?,
    private val onItemClick: (Shelter) -> Unit,
    private val onViewDirectionClick: (Shelter) -> Unit,
    private val onNavigateClick: (Shelter) -> Unit
) : RecyclerView.Adapter<SheltersAdapter.ShelterViewHolder>() {

    class ShelterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val shelterName: TextView = itemView.findViewById(R.id.shelter_name)
        val shelterAddress: TextView = itemView.findViewById(R.id.shelter_address)
        val shelterCapacity: TextView = itemView.findViewById(R.id.shelter_capacity)
        val shelterContact: TextView = itemView.findViewById(R.id.shelter_contact)
        val shelterFacilities: TextView = itemView.findViewById(R.id.shelter_facilities)
        val shelterDistance: TextView = itemView.findViewById(R.id.shelter_distance)
        val capacityProgress: ProgressBar = itemView.findViewById(R.id.capacity_progress)
        val btnViewDirection: Button = itemView.findViewById(R.id.btn_view_direction)
        val btnNavigate: Button = itemView.findViewById(R.id.btn_navigate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShelterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shelter, parent, false)
        return ShelterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShelterViewHolder, position: Int) {
        val shelter = shelters[position]

        holder.shelterName.text = shelter.name
        holder.shelterAddress.text = shelter.address
        holder.shelterCapacity.text = "${shelter.currentOccupancy} / ${shelter.capacity} people"
        holder.shelterContact.text = shelter.contactNumber

        // Format facilities nicely
        val facilitiesList = shelter.facilities.split(",").joinToString(" • ") { it.trim() }
        holder.shelterFacilities.text = facilitiesList

        // Calculate and set capacity progress
        val occupancyPercent = ((shelter.currentOccupancy.toFloat() / shelter.capacity.toFloat()) * 100).toInt()
        holder.capacityProgress.progress = occupancyPercent

        // Change progress color based on occupancy
        when {
            occupancyPercent >= 80 -> {
                holder.capacityProgress.progressTintList = ColorStateList.valueOf(0xFFF44336.toInt()) // Red
            }
            occupancyPercent >= 60 -> {
                holder.capacityProgress.progressTintList = ColorStateList.valueOf(0xFFFF9800.toInt()) // Orange
            }
            else -> {
                holder.capacityProgress.progressTintList = ColorStateList.valueOf(0xFF4CAF50.toInt()) // Green
            }
        }

        // Calculate and display distance from user's location
        if (userLocation != null) {
            val distance = calculateDistance(
                userLocation!!.latitude,
                userLocation!!.longitude,
                shelter.latitude,
                shelter.longitude
            )
            holder.shelterDistance.text = "📍 %.1f km away".format(distance)
        } else {
            holder.shelterDistance.text = "Distance: Location unavailable"
        }

        holder.itemView.setOnClickListener {
            onItemClick(shelter)
        }

        // Handle View Direction button click
        holder.btnViewDirection.setOnClickListener {
            onViewDirectionClick(shelter)
        }

        // Handle Navigate button click
        holder.btnNavigate.setOnClickListener {
            onNavigateClick(shelter)
        }
    }

    override fun getItemCount(): Int = shelters.size

    fun updateShelters(newShelters: List<Shelter>, location: Location?) {
        shelters = newShelters
        userLocation = location
        notifyDataSetChanged()
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] / 1000 // Convert to kilometers
    }
}
