package com.focusbreak.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Inicio", Icons.Outlined.Home),
    Activity("activity", "Actividad", Icons.Outlined.BarChart),
    Focus("focus", "Concentración", Icons.Outlined.CenterFocusStrong),
    Profile("profile", "Perfil", Icons.Outlined.Person),
}
