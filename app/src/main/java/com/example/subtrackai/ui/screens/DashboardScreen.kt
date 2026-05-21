package com.example.subtrackai.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subtrackai.R
import com.example.subtrackai.model.Subscription
import com.example.subtrackai.ui.components.AddSubscriptionDialog
import com.example.subtrackai.ui.theme.DeepBlue
import com.example.subtrackai.ui.theme.DeepPurple
import com.example.subtrackai.viewmodel.DashboardViewModel
import com.example.subtrackai.viewmodel.SettingsViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToProfile: () -> Unit,
    isDarkMode: Boolean
) {
    val subscriptions by viewModel.filteredSubscriptions.collectAsState()
    val allSubscriptions by viewModel.subscriptions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val totalSpend by viewModel.totalMonthlySpend.collectAsState()
    val potentialSavings by viewModel.potentialAnnualSavings.collectAsState()
    val currentCurrency by settingsViewModel.selectedCurrency.collectAsState()
    val showInsights by settingsViewModel.showSmartInsights.collectAsState()
    
    val socialViewModel: com.example.subtrackai.viewmodel.SocialViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val userProfile by socialViewModel.userProfile.collectAsState()

    var isSearching by remember { mutableStateOf(false) }
    var timeFrame by remember { mutableStateOf("Monthly") } 
    
    val categories = listOf("All", "Streaming", "Gaming", "Music", "Tools", "Finance", "Other")

    var showDialog by remember { mutableStateOf(false) }
    var editingSubscription by remember { mutableStateOf<Subscription?>(null) }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = onNavigateToChat,
                    containerColor = DeepPurple,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "AI Chat")
                }
                
                ExtendedFloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = DeepBlue,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Sub") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Search subscriptions...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Search")
                        }
                    }
                }
            )

            // Category Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { viewModel.onCategoryChange(cat) },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeepPurple.copy(alpha = 0.1f),
                                selectedLabelColor = DeepPurple
                            )
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Smart Insights Card
            if (showInsights) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepPurple.copy(alpha = 0.05f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = DeepPurple.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = DeepPurple)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Smart Insights", fontWeight = FontWeight.Bold, color = DeepPurple)
                            Text(
                                if (potentialSavings > 0) "You could save up to ${currentCurrency}${String.format("%.2f", potentialSavings)} / year by switching some monthly plans to annual billing."
                                else "Your subscriptions look optimized!",
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Spend Overview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = DeepPurple)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(DeepPurple, Color(0xFF7C4DFF))
                            )
                        )
                ) {
                    // Decorative Blobs
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f),
                            radius = size.minDimension * 0.4f,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.2f)
                        )
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.05f),
                            radius = size.minDimension * 0.3f,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.8f)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "$timeFrame Spend", 
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(
                                onClick = { timeFrame = if (timeFrame == "Monthly") "Yearly" else "Monthly" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                            }
                        }
                        
                        val displaySpend = if (timeFrame == "Monthly") totalSpend else totalSpend * 12
                        Text(
                            "${currentCurrency}${String.format("%.2f", displaySpend)}",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Beneficial Data Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val trialCount = remember(allSubscriptions) { allSubscriptions.count { it.isTrial } }
                            val activeCount = remember(allSubscriptions) { allSubscriptions.size }
                            
                            // Calculate Next Renewal
                            val nextSub = remember(allSubscriptions) {
                                val today = Calendar.getInstance().apply { 
                                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                }.time
                                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                allSubscriptions
                                    .filter { it.renewalDate.isNotBlank() }
                                    .mapNotNull { try { Pair(it, format.parse(it.renewalDate)) } catch(e: Exception) { null } }
                                    .filter { it.second != null && !it.second!!.before(today) }
                                    .minByOrNull { it.second!!.time }
                            }

                            DashboardStatItem(
                                icon = Icons.Default.Repeat,
                                value = "$activeCount",
                                label = "Active"
                            )
                            
                            VerticalDivider(modifier = Modifier.height(32.dp), color = Color.White.copy(alpha = 0.2f))

                            DashboardStatItem(
                                icon = Icons.Default.Timer,
                                value = "$trialCount",
                                label = "Trials"
                            )

                            VerticalDivider(modifier = Modifier.height(32.dp), color = Color.White.copy(alpha = 0.2f))

                            val nextRenewalText = nextSub?.let { 
                                val outFormat = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                                "${it.first.name} (${outFormat.format(it.second!!)})"
                            } ?: "None"
                            
                            DashboardStatItem(
                                icon = Icons.Default.Event,
                                value = if (nextRenewalText.length > 12) nextRenewalText.take(9) + "..." else nextRenewalText,
                                label = "Next Bill"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Subscriptions List
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text("Your Subscriptions", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("${subscriptions.size} Total", color = Color.Gray, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (subscriptions.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No subscriptions found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(subscriptions) { sub ->
                        SubscriptionItem(
                            subscription = sub,
                            currency = currentCurrency,
                            onClick = {
                                editingSubscription = sub
                                showDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddSubscriptionDialog(
            onDismiss = { 
                showDialog = false
                editingSubscription = null
            },
            onConfirm = { name, price, cycle, date, cat, isTrial ->
                if (editingSubscription != null) {
                    // Update logic would go here, for now just add as new or mock
                    viewModel.addSubscription(name, price, cycle, date, cat, isTrial)
                } else {
                    viewModel.addSubscription(name, price, cycle, date, cat, isTrial)
                }
                showDialog = false
                editingSubscription = null
            }
        )
    }
}

@Composable
private fun DashboardStatItem(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
        Icon(
            icon, 
            contentDescription = null, 
            tint = Color.White.copy(alpha = 0.7f), 
            modifier = Modifier.size(16.dp)
        )
        Text(
            value, 
            fontWeight = FontWeight.Bold, 
            color = Color.White, 
            fontSize = 13.sp,
            maxLines = 1
        )
        Text(
            label, 
            color = Color.White.copy(alpha = 0.6f), 
            fontSize = 10.sp
        )
    }
}

@Composable
fun SubscriptionItem(subscription: Subscription, currency: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = DeepPurple.copy(alpha = 0.05f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        subscription.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = DeepPurple
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(subscription.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "${subscription.billingCycle} • Next: ${subscription.renewalDate}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            Text(
                "${currency}${subscription.price}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = DeepPurple
            )
        }
    }
}
