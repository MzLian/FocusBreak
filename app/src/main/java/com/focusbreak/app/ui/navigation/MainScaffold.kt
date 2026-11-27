package com.focusbreak.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.focusbreak.app.ui.screens.ActivityScreen
import com.focusbreak.app.ui.screens.FocusScreen
import com.focusbreak.app.ui.screens.HomeScreen
import com.focusbreak.app.ui.screens.ProfileScreen
import com.focusbreak.app.ui.theme.MintSurface

@Composable
fun MainScaffold() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    val navigateTo: (String) -> Unit = { route ->
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Destination.entries.forEach { d ->
                    NavigationBarItem(
                        selected = current == d.route,
                        onClick = { navigateTo(d.route) },
                        icon = { Icon(d.icon, contentDescription = d.label) },
                        label = { Text(d.label) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MintSurface)
                    )
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = Destination.Home.route, modifier = Modifier.padding(padding)) {
            composable(Destination.Home.route) {
                HomeScreen(
                    onStartFocus = { navigateTo(Destination.Focus.route) },
                    onOpenProfile = { navigateTo(Destination.Profile.route) },
                )
            }
            composable(Destination.Activity.route) { ActivityScreen() }
            composable(Destination.Focus.route) { FocusScreen() }
            composable(Destination.Profile.route) { ProfileScreen() }
        }
    }
}
