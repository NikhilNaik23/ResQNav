package com.resqnav.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.resqnav.app.R
import com.resqnav.app.model.DisasterAlert
import java.text.SimpleDateFormat
import java.util.*

class AlertsAdapter(
    private var alerts: List<DisasterAlert>,
    private val onItemClick: (DisasterAlert) -> Unit
) : RecyclerView.Adapter<AlertsAdapter.AlertViewHolder>() {

    class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val alertType: TextView = itemView.findViewById(R.id.alert_type)
        val alertSeverity: TextView = itemView.findViewById(R.id.alert_severity)
        val alertTitle: TextView = itemView.findViewById(R.id.alert_title)
        val alertDescription: TextView = itemView.findViewById(R.id.alert_description)
        val alertTimestamp: TextView = itemView.findViewById(R.id.alert_timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alert, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alerts[position]

        holder.alertType.text = alert.type.uppercase()
        holder.alertSeverity.text = alert.severity.uppercase()
        holder.alertTitle.text = alert.title
        holder.alertDescription.text = alert.description
        holder.alertTimestamp.text = formatTimestamp(alert.timestamp)

        // Set severity color
        val severityColor = when (alert.severity.lowercase()) {
            "critical" -> android.graphics.Color.parseColor("#D32F2F")
            "high" -> android.graphics.Color.parseColor("#F57C00")
            "medium" -> android.graphics.Color.parseColor("#FBC02D")
            "low" -> android.graphics.Color.parseColor("#388E3C")
            else -> android.graphics.Color.GRAY
        }
        holder.alertSeverity.setBackgroundColor(severityColor)

        holder.itemView.setOnClickListener {
            onItemClick(alert)
        }
    }

    override fun getItemCount(): Int = alerts.size

    fun updateAlerts(newAlerts: List<DisasterAlert>) {
        alerts = newAlerts
        notifyDataSetChanged()
    }

    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000} minutes ago"
            diff < 86400000 -> "${diff / 3600000} hours ago"
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}

