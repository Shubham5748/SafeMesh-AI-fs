package com.safemesh.ai.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.safemesh.ai.ui.screens.EmergencyContactsScreen
import com.safemesh.ai.ui.screens.EmergencyOverlayScreen
import com.safemesh.ai.ui.screens.HomeScreen
import com.safemesh.ai.ui.screens.PermissionManagerScreen
import com.safemesh.ai.ui.screens.ProfileScreen
import com.safemesh.ai.ui.screens.SafetyScreen
import com.safemesh.ai.ui.screens.SplashScreen
import com.safemesh.ai.ui.theme.AccentBlue
import com.safemesh.ai.ui.theme.BgDark
import com.safemesh.ai.ui.theme.CardDark
import com.safemesh.ai.ui.theme.TextMuted
import com.safemesh.ai.ui.theme.TextPrimary
import com.safemesh.ai.ui.viewmodel.EmergencyViewModel
import com.safemesh.ai.ui.viewmodel.SosUiState

sealed class Screen(val route: String, val label: String? = null, val icon: ImageVector? = null) {
    object Splash : Screen("splash")
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Safety : Screen("safety", "Safety", Icons.Default.Shield)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Contacts : Screen("contacts")
    object Permissions : Screen("permissions")
}

@Composable
fun MainAppNavigation(
    viewModel: EmergencyViewModel
) {
    val navController = rememberNavController()
    var showSplash by remember { mutableStateOf(true) }

    val sosState by viewModel.sosState.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val backendUrl by viewModel.backendUrl.collectAsState()
    val contacts by viewModel.contacts.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Safety,
        Screen.Profile
    )

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (currentRoute in bottomNavItems.map { it.route }) {
                    NavigationBar(
                        containerColor = CardDark,
                        contentColor = TextPrimary
                    ) {
                        bottomNavItems.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(Screen.Home.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = screen.icon!!,
                                        contentDescription = screen.label
                                    )
                                },
                                label = { Text(screen.label!!) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AccentBlue,
                                    selectedTextColor = AccentBlue,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            },
            containerColor = BgDark
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        userName = userName,
                        onSosClick = {
                            viewModel.startSosCountdown(reason = "Manual SOS Trigger from Home")
                        }
                    )
                }

                composable(Screen.Safety.route) {
                    SafetyScreen(
                        viewModel = viewModel
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        userName = userName,
                        backendUrl = backendUrl,
                        onNavigateToContacts = { navController.navigate(Screen.Contacts.route) },
                        onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) },
                        onSaveBackendUrl = { viewModel.updateBackendUrl(it) },
                        onSaveUserName = { viewModel.updateUserName(it) }
                    )
                }

                composable(Screen.Contacts.route) {
                    EmergencyContactsScreen(
                        contacts = contacts,
                        onAddContact = { name, phone, rel ->
                            viewModel.addContact(name, phone, rel)
                        },
                        onDeleteContact = { id ->
                            viewModel.removeContact(id)
                        }
                    )
                }

                composable(Screen.Permissions.route) {
                    PermissionManagerScreen()
                }
            }
        }

        // Fullscreen Emergency Overlay (when in countdown, dispatching, delivered, error)
        if (sosState !is SosUiState.Idle) {
            EmergencyOverlayScreen(
                state = sosState,
                onCancel = { viewModel.cancelSos() },
                onSendNow = { reason -> viewModel.sendNow(reason) },
                onClose = { viewModel.resetSosState() }
            )
        }
    }
}
