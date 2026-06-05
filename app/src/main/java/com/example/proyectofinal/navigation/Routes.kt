package com.example.proyectofinal.navigation

// === Definición de todas las rutas de navegación de la app ===
// Cada objeto data object representa una pantalla con su ruta URL simbólica.

sealed class Routes(val route: String) {
    data object Splash : Routes("splash")
    data object Login : Routes("login")
    data object Matchmaking : Routes("matchmaking")
    data object Profile : Routes("profile")
    data object Tournaments : Routes("tournaments")
    data object Lobby : Routes("lobby")
}
