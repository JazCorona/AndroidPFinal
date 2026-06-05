package com.example.proyectofinal.navigation

// === Gráfo de navegación principal de la aplicación ===
// Define la estructura de navegación con NavHost, incluyendo una barra inferior
// con dos pestañas (Matchmaking y Perfil) y las rutas a todas las pantallas.

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.proyectofinal.data.repository.GamerRepository
import com.example.proyectofinal.ui.screens.lobby.LobbyScreen
import com.example.proyectofinal.ui.screens.login.LoginScreen
import com.example.proyectofinal.ui.screens.matchmaking.MatchmakingScreen
import com.example.proyectofinal.ui.screens.profile.ProfileScreen
import com.example.proyectofinal.ui.screens.splash.SplashScreen
import com.example.proyectofinal.ui.screens.tournaments.TournamentsScreen

// Elemento de la barra de navegación inferior: ruta, etiqueta e icono
private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
)

// Ítems de la barra inferior: Matchmaking y Perfil
private val bottomNavItems = listOf(
    BottomNavItem(Routes.Matchmaking.route, "Match") { Icon(Icons.Outlined.Refresh, contentDescription = "Match") },
    BottomNavItem(Routes.Profile.route, "Perfil") { Icon(Icons.Outlined.Person, contentDescription = "Perfil") }
)

// Conjunto de rutas que muestran la barra inferior
private val bottomNavRoutes = bottomNavItems.map { it.route }.toSet()

@Composable
fun NavGraph(
    navController: NavHostController,
    repository: GamerRepository
) {
    // Obtiene la ruta actual para decidir si mostrar la barra inferior
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        // Barra de navegación inferior visible solo en Matchmaking y Perfil
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = item.icon,
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Routes.Matchmaking.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Definición de todas las rutas de la aplicación
        NavHost(
            navController = navController,
            startDestination = Routes.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.Splash.route) {
                SplashScreen(navController = navController)
            }
            composable(Routes.Login.route) {
                LoginScreen(navController = navController, repository = repository)
            }
            composable(Routes.Matchmaking.route) {
                MatchmakingScreen(navController = navController, onBack = { navController.popBackStack() })
            }
            composable(Routes.Profile.route) {
                ProfileScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.Tournaments.route) {
                TournamentsScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.Lobby.route) {
                LobbyScreen(
                    navController = navController,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
