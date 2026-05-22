package com.example.subtrackai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subtrackai.model.Subscription
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
        observeSubscriptions()
    }

    private fun observeSubscriptions() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(uid)
            .collection("subscriptions")
            .get(com.google.firebase.firestore.Source.CACHE) // Try cache first for speed
            .addOnSuccessListener { snapshot ->
                val subs = snapshot?.toObjects(Subscription::class.java) ?: emptyList()
                _subscriptions.value = subs
                calculateTotalMonthlySpend(subs)
                calculateInsights(subs)
            }
            
        // Still keep a listener but with lower frequency or just for updates
        firestore.collection("users")
            .document(uid)
            .collection("subscriptions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val subs = snapshot?.toObjects(Subscription::class.java) ?: emptyList()
                _subscriptions.value = subs
                calculateTotalMonthlySpend(subs)
                calculateInsights(subs)
            }
    }

    private fun calculateInsights(subs: List<Subscription>) {
        var savings = 0.0
        val trials = mutableListOf<Subscription>()
        val today = Calendar.getInstance()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (sub in subs) {
            // Savings Insight: Monthly to Yearly usually saves ~20%
            if (sub.billingCycle == "Monthly") {
                savings += (sub.price * 12 * 0.2)
            }
            
            // Trial Warnings
            if (sub.isTrial && sub.renewalDate.isNotBlank()) {
                try {
                    val date = format.parse(sub.renewalDate)
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
        // If no savings or trials, show a default insight to ensure the card is visible
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
        val uid = auth.currentUser?.uid ?: return
        val subs = _subscriptions.value
        if (subs.isEmpty()) return

        val oldRate = settingsViewModel.getRate(oldSymbol)
        val newRate = settingsViewModel.getRate(newSymbol)
        
        val batch = firestore.batch()
        subs.forEach { sub ->
            // Convert to USD first, then to new currency
            val priceInUsd = sub.price / oldRate
            val newPrice = priceInUsd * newRate
            
            val docRef = firestore.collection("users")
                .document(uid)
                .collection("subscriptions")
                .document(sub.id)
            
            batch.update(docRef, "price", Math.round(newPrice * 100.0) / 100.0)
        }
        
        batch.commit().addOnSuccessListener {
            observeSubscriptions() // Refresh
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategoryChange(category: String) {
        _selectedCategory.value = category
    }

    fun addSubscription(name: String, price: Double, billingCycle: String, renewalDate: String, category: String, isTrial: Boolean = false) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            android.util.Log.e("DashboardViewModel", "Cannot add subscription: User not logged in")
            return
        }
        val uid = currentUser.uid
        val newSub = Subscription(
            name = name,
            price = price,
            billingCycle = billingCycle,
            renewalDate = renewalDate,
            category = category,
            isTrial = isTrial
        )
        viewModelScope.launch {
            firestore.collection("users")
                .document(uid)
                .collection("subscriptions")
                .add(newSub)
                .addOnSuccessListener {
                    android.util.Log.d("DashboardViewModel", "Subscription added successfully with ID: ${it.id}")
                    // Tweak potential savings to force insight update
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("DashboardViewModel", "Error adding subscription", e)
                }
        }
    }

    fun updateSubscription(id: String, name: String, price: Double, billingCycle: String, renewalDate: String, category: String, isTrial: Boolean = false) {
        val uid = auth.currentUser?.uid ?: return
        val updatedSub = Subscription(
            id = id,
            name = name,
            price = price,
            billingCycle = billingCycle,
            renewalDate = renewalDate,
            category = category,
            isTrial = isTrial
        )
        viewModelScope.launch {
            firestore.collection("users")
                .document(uid)
                .collection("subscriptions")
                .document(id)
                .set(updatedSub)
        }
    }

    fun deleteSubscription(id: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            firestore.collection("users")
                .document(uid)
                .collection("subscriptions")
                .document(id)
                .delete()
        }
    }
}
