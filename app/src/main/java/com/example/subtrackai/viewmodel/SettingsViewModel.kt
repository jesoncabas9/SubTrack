package com.example.subtrackai.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel : ViewModel() {
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _selectedCurrency = MutableStateFlow("₱")
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    private val _showSmartInsights = MutableStateFlow(true)
    val showSmartInsights: StateFlow<Boolean> = _showSmartInsights.asStateFlow()

    val currencies = listOf(
        "₱", "$", "€", "£", "¥", "₹", "₽", "₩", "A$", "C$", "S$", "NZ$", "R$", "zł", "kr", "฿"
    )

    // Rough conversion rates relative to USD (1.0)
    private val conversionRates = mapOf(
        "$" to 1.0,
        "₱" to 56.0,
        "€" to 0.92,
        "£" to 0.79,
        "¥" to 150.0,
        "₹" to 83.0,
        "₽" to 92.0,
        "₩" to 1330.0,
        "A$" to 1.52,
        "C$" to 1.35,
        "S$" to 1.34,
        "NZ$" to 1.63,
        "R$" to 4.97,
        "zł" to 4.0,
        "kr" to 10.4,
        "฿" to 36.0
    )

    fun getRate(symbol: String): Double = conversionRates[symbol] ?: 1.0

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setCurrency(symbol: String) {
        _selectedCurrency.value = symbol
    }

    fun setShowSmartInsights(show: Boolean) {
        _showSmartInsights.value = show
    }
}
