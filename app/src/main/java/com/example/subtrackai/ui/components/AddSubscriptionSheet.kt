package com.example.subtrackai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subtrackai.ui.theme.DeepPurple
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscriptionSheet(
    initialName: String = "",
    initialPrice: String = "",
    initialCategory: String = "Streaming",
    initialCycle: String = "Monthly",
    initialDate: String = "",
    initialIsTrial: Boolean = false,
    initialReminderDays: Int = 3,
    currency: String = "$",
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String, String, Boolean, Int) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var price by remember { mutableStateOf(initialPrice) }
    var billingCycle by remember { mutableStateOf(initialCycle) }
    var renewalDate by remember { mutableStateOf(initialDate) }
    var category by remember { mutableStateOf(initialCategory) }
    var isTrial by remember { mutableStateOf(initialIsTrial) }
    var reminderDays by remember { mutableStateOf(initialReminderDays) }
    
    val categories = listOf("Streaming", "Gaming", "Music", "Tools", "Finance", "Other")
    val reminderOptions = (1..25).toList()
    
    val templates = listOf(
        Triple("Netflix", 15.99, "Streaming"),
        Triple("Spotify", 10.99, "Music"),
        Triple("Disney+", 7.99, "Streaming"),
        Triple("Xbox Game Pass", 14.99, "Gaming"),
        Triple("ChatGPT Plus", 20.00, "Tools")
    )
    
    // Store original price to restore after trial toggle
    var lastEnteredPrice by remember { mutableStateOf(initialPrice) }

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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (initialName.isEmpty()) "Add Subscription" else "Edit Subscription",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (initialName.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                templates.forEach { (tName, tPrice, tCat) ->
                    FilterChip(
                        selected = name == tName,
                        onClick = {
                            name = tName
                            val pStr = tPrice.toString()
                            price = if (isTrial) "0.0" else pStr
                            lastEnteredPrice = pStr
                            category = tCat
                        },
                        label = { Text(tName) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DeepPurple.copy(alpha = 0.1f),
                            selectedLabelColor = DeepPurple
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Service Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DeepPurple,
                focusedLabelColor = DeepPurple
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = price,
                onValueChange = { 
                    price = it
                    if (!isTrial) lastEnteredPrice = it
                },
                label = { Text("Price") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text(currency, color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepPurple,
                    focusedLabelColor = DeepPurple
                ),
                enabled = !isTrial
            )
            
            Box(modifier = Modifier.weight(1.2f)) {
                OutlinedTextField(
                    value = renewalDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Renewal Date") },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = DeepPurple, modifier = Modifier.size(20.dp))
                    },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = DeepPurple,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                )
                // Overlay to catch clicks
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showDatePicker = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DeepPurple.copy(alpha = 0.1f),
                            selectedLabelColor = DeepPurple
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reminder Section Polish with Slider/Scrollable row
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = DeepPurple, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reminder Settings", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Notify me", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        reminderOptions.forEach { days ->
                            val isSelected = reminderDays == days
                            Surface(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { reminderDays = days },
                                color = if (isSelected) DeepPurple else MaterialTheme.colorScheme.surface,
                                border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) else null,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("$days", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("days before", fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Free Trial", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (isTrial) "Price automatically set to 0" else "Toggle if currently in trial period", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = if (isTrial) DeepPurple else Color.Gray
                )
            }
            Switch(
                checked = isTrial, 
                onCheckedChange = { 
                    isTrial = it 
                    if (it) {
                        price = "0.0"
                    } else {
                        price = lastEnteredPrice
                    }
                },
                colors = SwitchDefaults.colors(checkedThumbColor = DeepPurple, checkedTrackColor = DeepPurple.copy(alpha = 0.3f))
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SegmentedButtonCustom(
                selected = billingCycle == "Monthly",
                onClick = { billingCycle = "Monthly" },
                label = "Monthly",
                modifier = Modifier.weight(1f)
            )
            SegmentedButtonCustom(
                selected = billingCycle == "Yearly",
                onClick = { billingCycle = "Yearly" },
                label = "Yearly",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val priceVal = price.toDoubleOrNull() ?: 0.0
                onConfirm(name, priceVal, billingCycle, renewalDate, category, isTrial, reminderDays)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepPurple),
            enabled = name.isNotBlank() && (price.isNotBlank() || isTrial) && renewalDate.isNotBlank()
        ) {
            Text(if (initialName.isEmpty()) "Create Subscription" else "Update Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun SegmentedButtonCustom(selected: Boolean, onClick: () -> Unit, label: String, modifier: Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) DeepPurple else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
