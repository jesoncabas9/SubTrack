package com.example.subtrackai.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.subtrackai.R
import com.example.subtrackai.ui.screens.*
import com.example.subtrackai.ui.theme.DeepPurple
import com.example.subtrackai.ui.theme.SubTrackTheme
import com.example.subtrackai.util.NetworkConnectivityObserver
import com.example.subtrackai.viewmodel.*

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Dashboard : BottomNavItem("dashboard", Icons.Default.Dashboard, "Home")
    object Feed : BottomNavItem("feed", Icons.Default.RssFeed, "Feed")
    object Chat : BottomNavItem("chat_tab", Icons.Default.Chat, "Messages")
    object Notifications : BottomNavItem("notifications", Icons.Default.Notifications, "Notification")
    object Settings : BottomNavItem("settings", Icons.Default.Settings, "Settings")
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val socialViewModel: SocialViewModel = viewModel()
    val peerChatViewModel: PeerChatViewModel = viewModel()
    
    val currentUser by authViewModel.currentUser.collectAsState()
    val isProfileComplete by authViewModel.isProfileComplete.collectAsState()
    val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
    
    // Connectivity Monitoring
    val connectivityObserver = remember { NetworkConnectivityObserver(context) }
    val status by connectivityObserver.observe().collectAsState(initial = com.example.subtrackai.util.ConnectivityObserver.Status.Available)
    
    // Theme Management
    var showInitialization by remember { mutableStateOf(true) }
    val authState by authViewModel.authState.collectAsState()

    // Theme Animation State
    var animateTheme by remember { mutableStateOf(false) }
    var targetThemeState by remember { mutableStateOf(isDarkMode) }

    LaunchedEffect(isDarkMode) {
        if (isDarkMode != targetThemeState) {
            animateTheme = true
            kotlinx.coroutines.delay(800) 
            targetThemeState = isDarkMode 
            kotlinx.coroutines.delay(200)
            animateTheme = false
        }
    }

    SubTrackTheme(darkTheme = targetThemeState) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showInitialization) {
                InitializationScreen(onInitializationComplete = { showInitialization = false })
            } else if (status != com.example.subtrackai.util.ConnectivityObserver.Status.Available) {
                NoConnectionScreen()
            } else if (authState is AuthViewModel.AuthState.Loading || authState is AuthViewModel.AuthState.CheckingProfile) {
                Box(modifier = Modifier.fillMaxSize().background(DeepPurple)) {
                    LoadingScreen(message = "Verifying Identity...")
                }
            } else {
                NavHost(
                    navController = navController, 
                    startDestination = when {
                        currentUser == null -> "login"
                        isProfileComplete == true -> "main"
                        isProfileComplete == false -> "complete_profile"
                        else -> "login" // Default to login while checking
                    }
                ) {
                    composable("login") {
                        LoginScreen(
                            viewModel = authViewModel,
                            onNavigateToCompleteProfile = { 
                                navController.navigate("complete_profile") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onLoginSuccess = { 
                                navController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("complete_profile") {
                        CompleteProfileScreen(
                            viewModel = authViewModel,
                            onComplete = {
                                navController.navigate("main") {
                                    popUpTo("complete_profile") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("main") {
                        if (currentUser == null) {
                            navController.navigate("login") {
                                popUpTo("main") { inclusive = true }
                            }
                        } else if (isProfileComplete == false) {
                            navController.navigate("complete_profile") {
                                popUpTo("main") { inclusive = true }
                            }
                        } else {
                            MainScreen(
                                rootNavController = navController,
                                authViewModel = authViewModel,
                                settingsViewModel = settingsViewModel,
                                socialViewModel = socialViewModel,
                                peerChatViewModel = peerChatViewModel,
                                isDarkMode = targetThemeState,
                                onThemeToggle = { settingsViewModel.toggleDarkMode() },
                                onNavigateToCreatePost = { navController.navigate("create_post") },
                                onNavigateToProfile = { navController.navigate("profile") },
                                onNavigateToVisitorProfile = { userId -> navController.navigate("profile_view/$userId") }
                            )
                        }
                    }

                    composable("create_post") {
                        val feedViewModel: FeedViewModel = viewModel()
                        CreatePostScreen(
                            feedViewModel = feedViewModel,
                            authViewModel = authViewModel,
                            socialViewModel = socialViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("profile") {
                        ProfileScreen(
                            authViewModel = authViewModel,
                            socialViewModel = socialViewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToCreatePost = { navController.navigate("create_post_profile") },
                            onNavigateToProfile = { userId -> navController.navigate("profile_view/$userId") }
                        )
                    }

                    composable("profile_view/{userId}") { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId")
                        ProfileScreen(
                            authViewModel = authViewModel,
                            socialViewModel = socialViewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToCreatePost = {}, 
                            onNavigateToProfile = { otherId -> navController.navigate("profile_view/$otherId") },
                            visitorUserId = userId
                        )
                    }
                    
                    composable("create_post_profile") {
                        val feedViewModel: FeedViewModel = viewModel()
                        CreatePostScreen(
                            feedViewModel = feedViewModel,
                            authViewModel = authViewModel,
                            socialViewModel = socialViewModel,
                            onBack = { navController.popBackStack() },
                            profilePost = true
                        )
                    }
                    
                    composable("chat") {
                        val dashboardViewModel: DashboardViewModel = viewModel()
                        val chatViewModel: ChatViewModel = viewModel()
                        val subs by dashboardViewModel.subscriptions.collectAsState()
                        ChatScreen(
                            viewModel = chatViewModel,
                            subscriptions = subs,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("analytics") {
                        val dashboardViewModel: DashboardViewModel = viewModel()
                        AnalyticsScreen(
                            viewModel = dashboardViewModel,
                            onBack = { navController.popBackStack() },
                            isDarkMode = targetThemeState
                        )
                    }
                }
            }

            // Theme Fall Animation Overlay
            AnimatedVisibility(
                visible = animateTheme,
                enter = slideInVertically(
                    initialOffsetY = { -it }, 
                    animationSpec = tween(800, easing = LinearOutSlowInEasing)
                ),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isDarkMode) 
                                    listOf(Color(0xFF121212), Color(0xFF121212).copy(alpha = 0.8f), Color.Transparent) 
                                    else 
                                    listOf(Color.White, Color.White.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    rootNavController: androidx.navigation.NavHostController,
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    socialViewModel: SocialViewModel,
    peerChatViewModel: PeerChatViewModel,
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit,
    onNavigateToCreatePost: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToVisitorProfile: (String) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val userProfile by socialViewModel.userProfile.collectAsState()
    val context = LocalContext.current

    var isChatActive by remember { mutableStateOf(false) }
    var chatUser by remember { mutableStateOf<com.example.subtrackai.model.UserProfile?>(null) }
    var showChatMenu by remember { mutableStateOf(false) }
    
    val safeBack = {
        if (isChatActive) {
            isChatActive = false
            chatUser = null
        } else if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        } else {
            (context as? android.app.Activity)?.finish()
        }
    }

    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Feed,
        BottomNavItem.Chat,
        BottomNavItem.Notifications,
        BottomNavItem.Settings
    )

    androidx.activity.compose.BackHandler(enabled = true) {
        safeBack()
    }

    Scaffold(
        topBar = {
            if (isChatActive && chatUser != null) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            com.example.subtrackai.ui.components.ProfileAvatar(
                                iconName = chatUser?.profileIcon,
                                avatarUrl = chatUser?.avatarUrl,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    chatUser?.username ?: "",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (chatUser?.isOnline == true) "Online" else "Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (chatUser?.isOnline == true) Color(0xFF4CAF50) else Color.Gray
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { safeBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { showChatMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                            }
                            DropdownMenu(expanded = showChatMenu, onDismissRequest = { showChatMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("View Profile") },
                                    onClick = {
                                        chatUser?.uid?.let { onNavigateToVisitorProfile(it) }
                                        showChatMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                                )
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        val titleText = when (currentDestination?.route) {
                            BottomNavItem.Dashboard.route -> "SubTrack"
                            BottomNavItem.Feed.route -> "Feed"
                            BottomNavItem.Chat.route -> "Messages"
                            BottomNavItem.Notifications.route -> "Notifications"
                            BottomNavItem.Settings.route -> "Settings"
                            else -> "SubTrack"
                        }
                        Text(
                            titleText,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    actions = {
                        if (currentDestination?.route == BottomNavItem.Dashboard.route) {
                            IconButton(onClick = { rootNavController.navigate("analytics") }) {
                                Icon(Icons.Default.BarChart, contentDescription = "Analytics")
                            }
                        }
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = BottomNavItem.Dashboard.route, modifier = Modifier.padding(innerPadding)) {
            composable(BottomNavItem.Dashboard.route) {
                val dashboardViewModel: DashboardViewModel = viewModel()
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    settingsViewModel = settingsViewModel,
                    onNavigateToChat = { rootNavController.navigate("chat") },
                    onNavigateToAnalytics = { rootNavController.navigate("analytics") },
                    onNavigateToProfile = onNavigateToProfile,
                    isDarkMode = isDarkMode
                )
            }
            composable(BottomNavItem.Feed.route) {
                val feedViewModel: FeedViewModel = viewModel()
                FeedScreen(
                    viewModel = feedViewModel,
                    onNavigateToCreatePost = onNavigateToCreatePost,
                    onNavigateToProfile = onNavigateToVisitorProfile,
                    onNavigateToMyProfile = onNavigateToProfile,
                    isDarkMode = isDarkMode
                )
            }
            composable(BottomNavItem.Chat.route) {
                PeerChatScreen(
                    socialViewModel = socialViewModel,
                    chatViewModel = peerChatViewModel,
                    isDarkMode = isDarkMode,
                    selectedUser = chatUser,
                    onUserSelected = { user ->
                        chatUser = user
                        isChatActive = user != null
                    }
                )
            }
            composable(BottomNavItem.Notifications.route) {
                val dashboardViewModel: DashboardViewModel = viewModel()
                NotificationScreen(dashboardViewModel = dashboardViewModel)
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(
                    authViewModel = authViewModel,
                    socialViewModel = socialViewModel,
                    settingsViewModel = settingsViewModel,
                    isDarkMode = isDarkMode,
                    onThemeToggle = onThemeToggle,
                    onNavigateToProfile = onNavigateToProfile
                )
            }
        }
    }
}
