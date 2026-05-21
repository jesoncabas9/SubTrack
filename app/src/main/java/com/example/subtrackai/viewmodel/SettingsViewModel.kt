package com.example.subtrackai.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel : ViewModel() {
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _selectedCurrency = MutableStateFlow("$")
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    private val _showSmartInsights = MutableStateFlow(true)
    val showSmartInsights: StateFlow<Boolean> = _showSmartInsights.asStateFlow()

    val currencies = listOf(
        "$", "€", "£", "¥", "₹", "₽", "₩", "A$", "C$", "S$", "NZ$", "R$", "zł", "kr", "฿"
    )

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
