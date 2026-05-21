package com.example.subtrackai.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subtrackai.R
import com.example.subtrackai.model.Subscription
import com.example.subtrackai.ui.components.AddSubscriptionDialog
import com.example.subtrackai.ui.components.EditSubscriptionDialog
import com.example.subtrackai.ui.components.SavingsInsightCard
import com.example.subtrackai.ui.theme.DeepBlue
import com.example.subtrackai.ui.theme.DeepPurple
import com.example.subtrackai.ui.theme.GradientEnd
import com.example.subtrackai.ui.theme.GradientStart
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
    val upcomingTrials by viewModel.upcomingTrialEndings.collectAsState()
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
                title = { 
                    if (isSearching) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            placeholder = { Text("Search...") },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "SubTrack",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkMode) Color.White else DeepPurple
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (!isSearching) {
                        Image(
                            painter = painterResource(id = R.drawable.subtrack_logo),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
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
                                shape = androidx.compose.foundation.shape.CircleShape,
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
            // Category Selector with slide indicator
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
                            label = { Text(cat) }
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Slide",
                    tint = Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SavingsInsightCard(
                potentialSavings = potentialSavings,
                upcomingTrials = upcomingTrials,
                showInsights = showInsights
            )

            // Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(160.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(GradientStart, GradientEnd)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "$timeFrame Spend",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            var showTimeMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showTimeMenu = true }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = showTimeMenu,
                                    onDismissRequest = { showTimeMenu = false }
                                ) {
                                    listOf("Daily", "Monthly", "Yearly").forEach { frame ->
                                        DropdownMenuItem(
                                            text = { Text(frame) },
                                            onClick = {
                                                timeFrame = frame
                                                showTimeMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        val displayedSpend = when(timeFrame) {
                            "Daily" -> totalSpend / 30.0
                            "Yearly" -> totalSpend * 12.0
                            else -> totalSpend
                        }
                        
                        Text(
                            text = "$currentCurrency${"%.2f".format(displayedSpend)}",
                            color = Color.White,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Your Subscriptions",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text("${subscriptions.size} Total", fontSize = 14.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(subscriptions) { subscription ->
                    SubscriptionItem(subscription, currentCurrency, onClick = { editingSubscription = subscription })
                }
            }
        }

        if (showDialog) {
            AddSubscriptionDialog(
                onDismiss = { showDialog = false },
                onConfirm = { name, price, cycle, date, cat, isTrial ->
                    viewModel.addSubscription(name, price, cycle, date, cat, isTrial)
                    showDialog = false
                }
            )
        }

        editingSubscription?.let { sub ->
            EditSubscriptionDialog(
                subscription = sub,
                onDismiss = { editingSubscription = null },
                onConfirm = { name, price, cycle, date, cat, isTrial ->
                    viewModel.updateSubscription(sub.id, name, price, cycle, date, cat, isTrial)
                    editingSubscription = null
                },
                onDelete = {
                    viewModel.deleteSubscription(sub.id)
                    editingSubscription = null
                }
            )
        }
    }
}

@Composable
fun SubscriptionItem(subscription: Subscription, currentCurrency: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = DeepPurple.copy(alpha = 0.1f)
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
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(subscription.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        if (subscription.isTrial) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(color = Color(0xFFFF9800), shape = RoundedCornerShape(4.dp)) {
                                Text("TRIAL", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text("${subscription.billingCycle} • ${if (subscription.isTrial) "Ends" else "Next"}: ${subscription.renewalDate}", fontSize = 13.sp, color = Color.Gray)
                }
            }
            Text("$currentCurrency${"%.2f".format(subscription.price)}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = DeepBlue)
        }
    }
}
