package com.example.subtrackai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subtrackai.model.Subscription
import com.example.subtrackai.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel : ViewModel() {

    private val _subscriptions = MutableStateFlow<List<Subscription>>(emptyList())
    val subscriptions: StateFlow<List<Subscription>> = _subscriptions.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    val filteredSubscriptions = combine(_subscriptions, _searchQuery, _selectedCategory) { subs, query, category ->
        subs.filter { 
            (category == "All" || it.category == category) &&
            it.name.contains(query, ignoreCase = true)
        }.sortedByDescending { it.price }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _totalMonthlySpend = MutableStateFlow(0.0)
    val totalMonthlySpend: StateFlow<Double> = _totalMonthlySpend.asStateFlow()

    private val _potentialAnnualSavings = MutableStateFlow(0.0)
    val potentialAnnualSavings: StateFlow<Double> = _potentialAnnualSavings.asStateFlow()

    private val _upcomingTrialEndings = MutableStateFlow<List<Subscription>>(emptyList())
    val upcomingTrialEndings: StateFlow<List<Subscription>> = _upcomingTrialEndings.asStateFlow()

    init {
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated) {
                    observeSubscriptions()
                } else {
                    _subscriptions.value = emptyList()
                }
            }
        }
    }

    fun observeSubscriptions() {
        val user = supabase.auth.currentUserOrNull() ?: return
        val uid = user.id
        
        viewModelScope.launch {
            try {
                val subs = supabase.postgrest["subscriptions"]
                    .select {
                        filter {
                            eq("user_id", uid)
                        }
                    }
                    .decodeList<Subscription>()
                
                _subscriptions.value = subs
                calculateTotalMonthlySpend(subs)
                calculateInsights(subs)
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error fetching subscriptions", e)
            }
        }
    }

    private fun calculateInsights(subs: List<Subscription>) {
        var savings = 0.0
        val trials = mutableListOf<Subscription>()
        val today = Calendar.getInstance()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (sub in subs) {
            if (sub.billingCycle == "Monthly") {
                savings += (sub.price * 12 * 0.2)
            }
            
            val renewalDate = sub.renewalDate ?: ""
            if (sub.isTrial && renewalDate.isNotBlank()) {
                try {
                    val date = format.parse(renewalDate)
                    if (date != null) {
                        val diff = date.time - today.timeInMillis
                        val days = diff / (1000 * 60 * 60 * 24)
                        if (days <= 7) { 
                            trials.add(sub)
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
        _potentialAnnualSavings.value = if (savings > 0 || trials.isNotEmpty()) savings else 0.01
        _upcomingTrialEndings.value = trials
    }

    private fun calculateTotalMonthlySpend(subs: List<Subscription>) {
        var total = 0.0
        for (sub in subs) {
            total += if (sub.billingCycle == "Yearly") {
                sub.price / 12.0
            } else {
                sub.price
            }
        }
        _totalMonthlySpend.value = if (total.isNaN()) 0.0 else total
    }

    fun updateCurrencyPrices(oldSymbol: String, newSymbol: String, settingsViewModel: SettingsViewModel) {
        val subs = _subscriptions.value
        if (subs.isEmpty()) return

        val oldRate = settingsViewModel.getRate(oldSymbol)
        val newRate = settingsViewModel.getRate(newSymbol)
        
        viewModelScope.launch {
            try {
                subs.forEach { sub ->
                    val priceInUsd = sub.price / oldRate
                    val newPrice = Math.round((priceInUsd * newRate) * 100.0) / 100.0
                    
                    sub.id?.let {
                        supabase.postgrest["subscriptions"].update(
                            mapOf("price" to newPrice)
                        ) {
                            filter {
                                eq("id", it)
                            }
                        }
                    }
                }
                observeSubscriptions()
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error updating currency prices", e)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategoryChange(category: String) {
        _selectedCategory.value = category
    }

    fun addSubscription(name: String, price: Double, billingCycle: String, renewalDate: String, category: String, isTrial: Boolean = false) {
        val user = supabase.auth.currentUserOrNull() ?: return
        val newSub = Subscription(
            userId = user.id,
            name = name,
            price = price,
            billingCycle = billingCycle,
            renewalDate = renewalDate,
            category = category,
            isTrial = isTrial
        )
        viewModelScope.launch {
            try {
                supabase.postgrest["subscriptions"].insert(newSub)
                observeSubscriptions()
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error adding subscription: ${e.message}", e)
            }
        }
    }

    fun updateSubscription(id: String, name: String, price: Double, billingCycle: String, renewalDate: String, category: String, isTrial: Boolean = false) {
        val user = supabase.auth.currentUserOrNull() ?: return
        val updatedSub = Subscription(
            id = id,
            userId = user.id,
            name = name,
            price = price,
            billingCycle = billingCycle,
            renewalDate = renewalDate,
            category = category,
            isTrial = isTrial
        )
        viewModelScope.launch {
            try {
                supabase.postgrest["subscriptions"].update(updatedSub) {
                    filter {
                        eq("id", id)
                    }
                }
                observeSubscriptions()
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error updating subscription", e)
            }
        }
    }

    fun deleteSubscription(id: String) {
        viewModelScope.launch {
            try {
                supabase.postgrest["subscriptions"].delete {
                    filter {
                        eq("id", id)
                    }
                }
                observeSubscriptions()
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error deleting subscription", e)
            }
        }
    }
}
