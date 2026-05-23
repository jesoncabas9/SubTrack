package com.example.subtrackai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.subtrackai.model.Subscription
import com.example.subtrackai.ui.theme.DeepPurple
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSubscriptionDialog(
    subscription: Subscription,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String, String, Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(subscription.name) }
    var price by remember { mutableStateOf(subscription.price.toString()) }
    var billingCycle by remember { mutableStateOf(subscription.billingCycle ?: "Monthly") }
    var renewalDate by remember { mutableStateOf(subscription.renewalDate ?: "") }
    var category by remember { mutableStateOf(subscription.category ?: "Other") }
    var isTrial by remember { mutableStateOf(subscription.isTrial) }
    
    val categories = listOf("Streaming", "Gaming", "Music", "Tools", "Finance", "Other")
    
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
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Subscription")
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    RadioButton(selected = billingCycle == "Monthly", onClick = { billingCycle = "Monthly" })
                    Text("Monthly")
                    RadioButton(selected = billingCycle == "Yearly", onClick = { billingCycle = "Yearly" })
                    Text("Yearly")
                }

                OutlinedTextField(
                    value = renewalDate,
                    onValueChange = { },
                    label = { Text(if (isTrial) "Trial End Date" else "Renewal Date") },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    readOnly = true,
                    enabled = false,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
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
                enabled = name.isNotBlank() && (price.isNotBlank() || isTrial)
            ) { Text("Save Changes") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
