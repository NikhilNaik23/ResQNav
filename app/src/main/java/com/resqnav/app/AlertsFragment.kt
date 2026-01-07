package com.resqnav.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.resqnav.app.adapter.AlertsAdapter
import com.resqnav.app.api.DisasterApi
import com.resqnav.app.model.DisasterAlert
import com.resqnav.app.viewmodel.DisasterViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AlertsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AlertsAdapter
    private lateinit var viewModel: DisasterViewModel
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var emptyState: View

    // Filter UI components
    private lateinit var btnToggleFilters: MaterialButton
    private lateinit var filterCard: View
    private lateinit var chipGroupType: ChipGroup
    private lateinit var chipGroupSeverity: ChipGroup
    private lateinit var chipGroupRegion: ChipGroup
    private lateinit var btnClearFilters: MaterialButton

    private var filtersExpanded = false

    // Filter state
    private val selectedTypes = mutableSetOf<String>()
    private val selectedSeverities = mutableSetOf<String>()
    private val selectedRegions = mutableSetOf<String>()
    private var allAlerts = listOf<DisasterAlert>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alerts, container, false)

        recyclerView = view.findViewById(R.id.alerts_recycler_view)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        emptyState = view.findViewById(R.id.empty_state)

        // Initialize filter UI components
        btnToggleFilters = view.findViewById(R.id.btn_toggle_filters)
        filterCard = view.findViewById(R.id.filter_card)
        chipGroupType = view.findViewById(R.id.chip_group_type)
        chipGroupSeverity = view.findViewById(R.id.chip_group_severity)
        chipGroupRegion = view.findViewById(R.id.chip_group_region)
        btnClearFilters = view.findViewById(R.id.btn_clear_filters)

        // Setup filter toggle functionality
        setupFilterToggle()

        setupRecyclerView()
        setupViewModel()
        setupSwipeRefresh()
        setupFilters()

        // Ensure all filters are cleared on start
        clearAllFiltersOnStart()

        // Fetch real-time earthquake data only (no sample data)
        fetchRealTimeAlerts()

        return view
    }

    private fun clearAllFiltersOnStart() {
        // Clear filter sets
        selectedTypes.clear()
        selectedSeverities.clear()
        selectedRegions.clear()

        // Uncheck all chips programmatically
        chipGroupType.clearCheck()
        chipGroupSeverity.clearCheck()
        chipGroupRegion.clearCheck()

        android.util.Log.d("AlertsFragment", "Filters cleared on start. Selected types: ${selectedTypes.size}, severities: ${selectedSeverities.size}, regions: ${selectedRegions.size}")
    }

    private fun setupFilterToggle() {
        btnToggleFilters.setOnClickListener {
            filtersExpanded = !filtersExpanded

            if (filtersExpanded) {
                // Show filters
                filterCard.visibility = View.VISIBLE
                btnToggleFilters.icon = requireContext().getDrawable(android.R.drawable.arrow_up_float)
                btnToggleFilters.text = "🔍 Hide Filters"
            } else {
                // Hide filters
                filterCard.visibility = View.GONE
                btnToggleFilters.icon = requireContext().getDrawable(android.R.drawable.arrow_down_float)
                btnToggleFilters.text = "🔍 Filters"
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = AlertsAdapter(emptyList()) { alert ->
            onAlertClick(alert)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[DisasterViewModel::class.java]

        viewModel.allActiveAlerts.observe(viewLifecycleOwner) { alerts ->
            allAlerts = alerts
            applyFilters()
            swipeRefresh.isRefreshing = false
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            fetchRealTimeAlerts()
        }
    }

    private fun setupFilters() {
        // Disaster Type Filters
        setupChipGroupListener(chipGroupType, selectedTypes, mapOf(
            R.id.chip_earthquake to "Earthquake",
            R.id.chip_flood to "Flood",
            R.id.chip_fire to "Fire",
            R.id.chip_cyclone to "Cyclone",
            R.id.chip_tsunami to "Tsunami"
        ))

        // Severity Level Filters
        setupChipGroupListener(chipGroupSeverity, selectedSeverities, mapOf(
            R.id.chip_critical to "CRITICAL",
            R.id.chip_high to "HIGH",
            R.id.chip_medium to "MEDIUM",
            R.id.chip_low to "LOW"
        ))

        // Region Filters (Continents)
        setupChipGroupListener(chipGroupRegion, selectedRegions, mapOf(
            R.id.chip_asia to "Asia",
            R.id.chip_europe to "Europe",
            R.id.chip_north_america to "North America",
            R.id.chip_south_america to "South America",
            R.id.chip_africa to "Africa",
            R.id.chip_oceania to "Oceania"
        ))

        // Clear Filters Button
        btnClearFilters.setOnClickListener {
            clearAllFilters()
        }
    }

    private fun setupChipGroupListener(
        chipGroup: ChipGroup,
        selectedSet: MutableSet<String>,
        chipMapping: Map<Int, String>
    ) {
        chipMapping.forEach { (chipId, value) ->
            chipGroup.findViewById<Chip>(chipId)?.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedSet.add(value)
                } else {
                    selectedSet.remove(value)
                }
                applyFilters()
            }
        }
    }

    private fun clearAllFilters() {
        selectedTypes.clear()
        selectedSeverities.clear()
        selectedRegions.clear()

        // Uncheck all chips
        for (i in 0 until chipGroupType.childCount) {
            (chipGroupType.getChildAt(i) as? Chip)?.isChecked = false
        }
        for (i in 0 until chipGroupSeverity.childCount) {
            (chipGroupSeverity.getChildAt(i) as? Chip)?.isChecked = false
        }
        for (i in 0 until chipGroupRegion.childCount) {
            (chipGroupRegion.getChildAt(i) as? Chip)?.isChecked = false
        }

        applyFilters()
    }

    private fun applyFilters() {
        android.util.Log.d("AlertsFragment", "=== Applying Filters ===")
        android.util.Log.d("AlertsFragment", "Total alerts in database: ${allAlerts.size}")
        android.util.Log.d("AlertsFragment", "Selected types: $selectedTypes")
        android.util.Log.d("AlertsFragment", "Selected severities: $selectedSeverities")
        android.util.Log.d("AlertsFragment", "Selected regions: $selectedRegions")

        // Start with all alerts sorted by timestamp (newest first)
        var filteredAlerts = allAlerts.sortedByDescending { it.timestamp }

        // Log disaster types in database
        val typeCounts = allAlerts.groupBy { it.type }.mapValues { it.value.size }
        android.util.Log.d("AlertsFragment", "Disasters by type: $typeCounts")

        // Filter by disaster type (case-insensitive) - only if filters are selected
        if (selectedTypes.isNotEmpty()) {
            filteredAlerts = filteredAlerts.filter { alert ->
                selectedTypes.any { selectedType ->
                    alert.type.equals(selectedType, ignoreCase = true)
                }
            }
            android.util.Log.d("AlertsFragment", "After type filter: ${filteredAlerts.size} alerts")
        }

        // Filter by severity (case-insensitive) - only if filters are selected
        if (selectedSeverities.isNotEmpty()) {
            filteredAlerts = filteredAlerts.filter { alert ->
                selectedSeverities.any { selectedSeverity ->
                    alert.severity.equals(selectedSeverity, ignoreCase = true)
                }
            }
            android.util.Log.d("AlertsFragment", "After severity filter: ${filteredAlerts.size} alerts")
        }

        // Filter by region (based on location) - only if filters are selected
        if (selectedRegions.isNotEmpty()) {
            filteredAlerts = filteredAlerts.filter { alert ->
                val region = getRegionFromCoordinates(alert.latitude, alert.longitude)
                selectedRegions.contains(region)
            }
            android.util.Log.d("AlertsFragment", "After region filter: ${filteredAlerts.size} alerts")
        }

        android.util.Log.d("AlertsFragment", "Final filtered alerts: ${filteredAlerts.size}")

        // Update adapter
        adapter.updateAlerts(filteredAlerts)

        // Update UI visibility
        if (filteredAlerts.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun getRegionFromCoordinates(lat: Double, lon: Double): String {
        return when {
            // Asia (approximate boundaries)
            lat in -10.0..80.0 && lon in 25.0..180.0 -> "Asia"
            // Europe
            lat in 35.0..75.0 && lon in -25.0..60.0 -> "Europe"
            // North America
            lat in 15.0..85.0 && lon in -170.0..-50.0 -> "North America"
            // South America
            lat in -60.0..15.0 && lon in -85.0..-30.0 -> "South America"
            // Africa
            lat in -35.0..40.0 && lon in -20.0..55.0 -> "Africa"
            // Oceania (Australia, Pacific Islands)
            lat in -50.0..0.0 && lon in 110.0..180.0 -> "Oceania"
            else -> "Other"
        }
    }

    private fun fetchRealTimeAlerts() {
        swipeRefresh.isRefreshing = true

        // Use the new multi-disaster API to fetch ALL disaster types globally
        viewModel.refreshAllDisasters()

        // Observe loading state and error messages
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            swipeRefresh.isRefreshing = isLoading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        Toast.makeText(
            requireContext(),
            "Fetching disasters from NASA EONET and USGS APIs...",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun onAlertClick(alert: DisasterAlert) {
        Toast.makeText(
            requireContext(),
            "Alert: ${alert.title}\nSeverity: ${alert.severity}",
            Toast.LENGTH_LONG
        ).show()
    }
}
