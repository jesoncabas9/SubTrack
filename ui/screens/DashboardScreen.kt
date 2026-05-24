package com.example.subtrackai.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subtrackai.model.Subscription
import com.example.subtrackai.ui.components.AddSubscriptionSheet
import com.example.subtrackai.ui.components.SubscriptionDetailsSheet
import com.example.subtrackai.ui.theme.DeepBlue
import com.example.subtrackai.ui.theme.DeepPurple
import com.example.subtrackai.ui.theme.LightPurple
import com.example.subtrackai.viewmodel.DashboardViewModel
import com.example.subtrackai.viewmodel.SettingsViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAIChat: () -> Unit,
    isDarkMode: Boolean
) {
    val subscriptions by viewModel.filteredSubscriptions.collectAsState()
    val allSubscriptions by viewModel.subscriptions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val totalSpend by viewModel.totalMonthlySpend.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val potentialSavings by viewModel.potentialAnnualSavings.collectAsState()
    val currentCurrency by settingsViewModel.selectedCurrency.collectAsState()
    val showInsights by settingsViewModel.showSmartInsights.collectAsState()
    
    val socialViewModel: com.example.subtrackai.viewmodel.SocialViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val userProfile by socialViewModel.userProfile.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState()
    var isSearching by remember { mutableStateOf(false) }
    var timeFrame by remember { mutableStateOf("Monthly") } 
    
    val categories = listOf("All", "Streaming", "Gaming", "Music", "Tools", "Finance", "Other")

    var showDialog by remember { mutableStateOf(false) }
    var showDetailsSheet by remember { mutableStateOf(false) }
    var selectedSubscription by remember { mutableStateOf<Subscription?>(null) }
    var editingSubscription by remember { mutableStateOf<Subscription?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = onNavigateToAIChat,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
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
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshSubscriptions() },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            placeholder = { Text("Search subscriptions...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
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
                                categories.forEach { category ->
                                    FilterChip(
                                        selected = selectedCategory == category,
                                        onClick = { viewModel.onCategoryChange(category) },
                                        label = { Text(category) },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = DeepPurple.copy(alpha = 0.1f),
                                            selectedLabelColor = DeepPurple
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Spend Overview Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = DeepPurple)
                        ) {
                            Column(
                                modifier = Modifier
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(DeepPurple, Color(0xFF5E35B1))
                                        )
                                    )
                                    .padding(24.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Total Spend",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 14.sp
                                        )
                                        
                                        val displaySpend = if (timeFrame == "Yearly") totalSpend * 12 else totalSpend
                                        
                                        AnimatedContent(
                                            targetState = displaySpend,
                                            transitionSpec = {
                                                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                                            },
                                            label = "spendAnimation"
                                        ) { amount ->
                                            Text(
                                                text = "${currentCurrency}${String.format("%.2f", amount)}",
                                                color = Color.White,
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                    
                                    // Timeframe Toggle
                                    Surface(
                                        color = Color.White.withAlpha(0.15f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(modifier = Modifier.padding(4.dp)) {
                                            listOf("Monthly", "Yearly").forEach { frame ->
                                                val isSelected = timeFrame == frame
                                                Surface(
                                                    modifier = Modifier.clickable { timeFrame = frame },
                                                    color = if (isSelected) Color.White else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        frame,
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                        color = if (isSelected) DeepPurple else Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val trialCount = remember(allSubscriptions) { allSubscriptions.count { it.isTrial } }
                                    val activeCount = remember(allSubscriptions) { allSubscriptions.size }
                                    
                                    // Calculate Next Renewal
                                    val nextSub = remember(allSubscriptions) {
                                        val today = Calendar.getInstance().apply { 
                                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                        }.time
                                        val format = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                        allSubscriptions
                                            .filter { (it.renewalDate ?: "").isNotBlank() }
                                            .mapNotNull { try { Pair(it, format.parse(it.renewalDate ?: "")) } catch(e: Exception) { null } }
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
                                        val outFormat = java.text.SimpleDateFormat("MMM dd", Locale.getDefault())
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

                        Spacer(modifier = Modifier.height(24.dp))

                        // Subscriptions List Title
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Your Subscriptions",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${subscriptions.size} total",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                if (subscriptions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.CreditCardOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No subscriptions found", color = Color.Gray)
                            }
                        }
                    }
                } else {
                    items(subscriptions, key = { it.id ?: UUID.randomUUID().toString() }) { sub ->
                        SubscriptionItem(
                            subscription = sub,
                            currency = currentCurrency,
                            onClick = {
                                selectedSubscription = sub
                                showDetailsSheet = true
                            }
                        )
                    }
                }
            }
        }

        // Add/Edit Dialog
        if (showDialog) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showDialog = false
                    editingSubscription = null
                },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                AddSubscriptionSheet(
                    initialName = editingSubscription?.name ?: "",
                    initialPrice = editingSubscription?.price?.toString() ?: "",
                    initialCategory = editingSubscription?.category ?: "Streaming",
                    initialCycle = editingSubscription?.billingCycle ?: "Monthly",
                    initialDate = editingSubscription?.renewalDate ?: "",
                    initialIsTrial = editingSubscription?.isTrial ?: false,
                    initialReminderDays = editingSubscription?.reminderDays ?: 3,
                    currency = currentCurrency,
                    onDismiss = { 
                        showDialog = false
                        editingSubscription = null
                    },
                    onConfirm = { name, price, cycle, date, cat, trial, reminder ->
                        if (editingSubscription == null) {
                            viewModel.addSubscription(name, price, cycle, date, cat, trial, reminder)
                        } else {
                            viewModel.updateSubscription(editingSubscription!!.id!!, name, price, cycle, date, cat, trial, reminder)
                        }
                        showDialog = false
                        editingSubscription = null
                    }
                )
            }
        }

        // Details Sheet
        if (showDetailsSheet && selectedSubscription != null) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showDetailsSheet = false
                    selectedSubscription = null
                },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                SubscriptionDetailsSheet(
                    subscription = selectedSubscription!!,
                    currency = currentCurrency,
                    onDismiss = { 
                        showDetailsSheet = false
                        selectedSubscription = null
                    },
                    onEdit = {
                        editingSubscription = selectedSubscription
                        showDetailsSheet = false
                        showDialog = true
                    },
                    onDelete = {
                        selectedSubscription?.id?.let { viewModel.deleteSubscription(it) }
                        showDetailsSheet = false
                        selectedSubscription = null
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardStatItem(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, size = 14.dp, tint = Color.White.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.width(4.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
    }
}

@Composable
fun SubscriptionItem(subscription: Subscription, currency: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = DeepPurple.copy(alpha = 0.05f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        subscription.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = DeepPurple,
                        fontSize = 20.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(subscription.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                val cycle = subscription.billingCycle ?: "Monthly"
                val date = subscription.renewalDate?.takeIf { it.isNotBlank() } ?: "Not set"
                Text(
                    "$cycle • Next: $date",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            Text(
                "${currency}${subscription.price}",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = if (subscription.isTrial) Color(0xFF4CAF50) else Color.Unspecified
            )
        }
    }
}

private fun Modifier.size(size: Int): Modifier = size(size.dp)
