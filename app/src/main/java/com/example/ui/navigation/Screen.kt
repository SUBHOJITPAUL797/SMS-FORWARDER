package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object RoleSelect : Screen("role_select")
    object ClientPairing : Screen("client_pairing")
    object HostPairing : Screen("host_pairing")
    object HostHome : Screen("host_home")
    object ClientHome : Screen("client_home")
    object DeveloperProfile : Screen("developer_profile")
}
