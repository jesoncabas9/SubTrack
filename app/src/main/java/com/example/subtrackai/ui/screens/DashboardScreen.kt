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
import androidx.compose.ui.graphics.Color
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
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (!isSearching) {
                        Row(modifier = Modifier.padding(start = 16.dp)) {
                            Image(
                                painter = painterResource(id = R.drawable.subtrack_logo),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                },
                title = { 
                    if (isSearching) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            placeholder = { Text("Search...") },
                            modifier = Modifier.fillMaxWidth(0.95f).height(52.dp),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { 
                                    isSearching = false
                                    viewModel.onSearchQueryChange("")
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Search")
                                }
                            }
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "SubTrack",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkMode) Color.White else DeepPurple,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (!isSearching) {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = onNavigateToAnalytics) {
                            Icon(Icons.Default.BarChart, contentDescription = "Analytics")
                        }
                        IconButton(onClick = onNavigateToProfile) {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape,
                                color = DeepPurple.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        com.example.subtrackai.util.ProfileIcons.getIcon(userProfile?.profileIcon),
                                        contentDescription = "Profile",
                                        modifier = Modifier.size(20.dp),
                                        tint = DeepPurple
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
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
                .padding(padding)
                .fillMaxSize()
        ) {
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$timeFrame Spend", color = Color.White.copy(alpha = 0.8f))
                        IconButton(onClick = { timeFrame = if (timeFrame == "Monthly") "Yearly" else "Monthly" }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                        }
                    }
                    val displaySpend = if (timeFrame == "Monthly") totalSpend else totalSpend * 12
                    Text(
                        "${currentCurrency}${String.format("%.2f", displaySpend)}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
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
