package com.example.subtrackai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscriptionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var billingCycle by remember { mutableStateOf("Monthly") }
    var renewalDate by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Streaming") }
    var isTrial by remember { mutableStateOf(false) }
    
    val categories = listOf("Streaming", "Gaming", "Music", "Tools", "Finance", "Other")
    val templates = listOf(
        Triple("Netflix", 15.99, "Streaming"),
        Triple("Spotify", 10.99, "Music"),
        Triple("Disney+", 7.99, "Streaming"),
        Triple("Xbox Game Pass", 14.99, "Gaming"),
        Triple("ChatGPT Plus", 20.00, "Tools")
    )
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate = datePickerState.selectedDateMillis
                    if (selectedDate != null) {
                        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        renewalDate = formatter.format(Date(selectedDate))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subscription") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Quick Templates", fontSize = 12.sp, color = Color.Gray)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    templates.forEach { (tName, tPrice, tCat) ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                name = tName
                                price = tPrice.toString()
                                category = tCat
                            },
                            label = { Text(tName, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g., Netflix)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price (Enter 0 for Free Trial)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = isTrial, onCheckedChange = { isTrial = it })
                    Text("Is this a Free Trial?")
                }

                Text("Category")
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Text("Billing Cycle")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RadioButton(
                        selected = billingCycle == "Monthly",
                        onClick = { billingCycle = "Monthly" }
                    )
                    Text("Monthly")
                    RadioButton(
                        selected = billingCycle == "Yearly",
                        onClick = { billingCycle = "Yearly" }
                    )
                    Text("Yearly")
                }

                OutlinedTextField(
                    value = renewalDate,
                    onValueChange = { /* Read-only via picker */ },
                    label = { Text(if (isTrial) "Trial End Date" else "Renewal Date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    readOnly = true,
                    enabled = false, // Workaround to ensure click goes to field modifier or decoration
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val priceVal = price.toDoubleOrNull() ?: 0.0
                    onConfirm(name, priceVal, billingCycle, renewalDate, category, isTrial)
                },
                enabled = name.isNotBlank() && (price.isNotBlank() || isTrial) && renewalDate.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
