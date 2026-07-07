package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AssetDetailScreen
import com.example.ui.AssetListScreen
import com.example.ui.SecurityDashboard
import com.example.ui.SecurityViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TacticalDark

sealed interface Screen {
    object Dashboard : Screen
    object AssetList : Screen
    data class AssetDetail(val assetId: Int) : Screen
}

class MainActivity : ComponentActivity() {
    private val viewModel: SecurityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

                // Track screen stack for back navigation
                val navigationBackStack = remember { mutableStateListOf<Screen>(Screen.Dashboard) }

                val navigateTo = { screen: Screen ->
                    if (navigationBackStack.lastOrNull() != screen) {
                        navigationBackStack.add(screen)
                    }
                    currentScreen = screen
                }

                val navigateBack = {
                    if (navigationBackStack.size > 1) {
                        navigationBackStack.removeLast()
                        currentScreen = navigationBackStack.last()
                    } else {
                        currentScreen = Screen.Dashboard
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Display bottom bar only if we are on Dashboard or AssetList
                        if (currentScreen is Screen.Dashboard || currentScreen is Screen.AssetList) {
                            NavigationBar(
                                containerColor = TacticalDark,
                                tonalElevation = 8.dp,
                                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen is Screen.Dashboard,
                                    onClick = { navigateTo(Screen.Dashboard) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Dashboard,
                                            contentDescription = "Dashboard"
                                        )
                                    },
                                    label = {
                                        Text(
                                            "DASHBOARD",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("nav_dashboard")
                                )

                                NavigationBarItem(
                                    selected = currentScreen is Screen.AssetList,
                                    onClick = { navigateTo(Screen.AssetList) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Dns,
                                            contentDescription = "Target Assets"
                                        )
                                    },
                                    label = {
                                        Text(
                                            "TARGETS",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("nav_assets")
                                )
                            }
                        }
                    },
                    containerColor = TacticalDark
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(TacticalDark)
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "screen_transition"
                        ) { screen ->
                            when (screen) {
                                is Screen.Dashboard -> {
                                    SecurityDashboard(
                                        viewModel = viewModel,
                                        onNavigateToAsset = { asset ->
                                            navigateTo(Screen.AssetDetail(asset.id))
                                        },
                                        onNavigateToAssetList = {
                                            navigateTo(Screen.AssetList)
                                        }
                                    )
                                }

                                is Screen.AssetList -> {
                                    AssetListScreen(
                                        viewModel = viewModel,
                                        onNavigateToAsset = { asset ->
                                            navigateTo(Screen.AssetDetail(asset.id))
                                        }
                                    )
                                }

                                is Screen.AssetDetail -> {
                                    AssetDetailScreen(
                                        assetId = screen.assetId,
                                        viewModel = viewModel,
                                        onNavigateBack = { navigateBack() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
