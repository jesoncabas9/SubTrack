package com.example.subtrackai.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.subtrackai.ui.screens.*
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
    val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
    
    // Connectivity Monitoring
    val connectivityObserver = remember { NetworkConnectivityObserver(context) }
    val status by connectivityObserver.observe().collectAsState(initial = com.example.subtrackai.util.ConnectivityObserver.Status.Available)
    
    // Theme Management
    var showInitialization by remember { mutableStateOf(true) }
    
    val authState by authViewModel.authState.collectAsState()

    SubTrackTheme(darkTheme = isDarkMode) {
        if (showInitialization) {
            InitializationScreen(onInitializationComplete = { showInitialization = false })
        } else if (status != com.example.subtrackai.util.ConnectivityObserver.Status.Available) {
            NoConnectionScreen()
        } else if (authState is AuthViewModel.AuthState.Loading) {
            Box(modifier = Modifier.fillMaxSize().background(com.example.subtrackai.ui.theme.DeepPurple)) {
                LoadingScreen(
                    message = if (currentUser != null) "Signing out..." else "Signing in..."
                )
            }
        } else {
            NavHost(navController = navController, startDestination = if (currentUser != null) "main" else "login") {
                composable("login") {
                    LoginScreen(
                        viewModel = authViewModel,
                        onNavigateToSignUp = { navController.navigate("signup") },
                        onLoginSuccess = { 
                            navController.navigate("main") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }
                
                composable("signup") {
                    SignUpScreen(
                        viewModel = authViewModel,
                        onNavigateToLogin = { navController.navigate("login") }
                    )
                }

                composable("main") {
                    if (currentUser == null) {
                        navController.navigate("login") {
                            popUpTo("main") { inclusive = true }
                        }
                    } else {
                        MainScreen(
                            rootNavController = navController,
                            authViewModel = authViewModel,
                            settingsViewModel = settingsViewModel,
                            socialViewModel = socialViewModel,
                            peerChatViewModel = peerChatViewModel,
                            isDarkMode = isDarkMode,
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
                        onNavigateToCreatePost = { navController.navigate("create_post_profile") }
                    )
                }

                composable("profile_view/{userId}") { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId")
                    ProfileScreen(
                        authViewModel = authViewModel,
                        socialViewModel = socialViewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToCreatePost = {}, // Visitors can't post to someone else's profile
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
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

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

    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Feed,
        BottomNavItem.Chat,
        BottomNavItem.Notifications,
        BottomNavItem.Settings
    )

    Scaffold(
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
                    onNavigateToMyProfile = onNavigateToProfile
                )
            }
            composable(BottomNavItem.Chat.route) {
                PeerChatScreen(
                    socialViewModel = socialViewModel,
                    chatViewModel = peerChatViewModel
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
