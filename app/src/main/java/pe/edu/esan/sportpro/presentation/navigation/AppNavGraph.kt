package pe.edu.esan.sportpro.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.presentation.auth.AuthViewModel
import pe.edu.esan.sportpro.presentation.auth.authGraph
import pe.edu.esan.sportpro.presentation.children.childrenGraph
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.presentation.home.HomeScreen
import pe.edu.esan.sportpro.presentation.players.playersGraph
import pe.edu.esan.sportpro.presentation.profile.ProfileScreen
import pe.edu.esan.sportpro.presentation.teams.teamApprovalGraph
import pe.edu.esan.sportpro.presentation.teams.teamsGraph

/**
 * Grafo principal. Cada módulo registra sus rutas en su propio archivo *Nav.kt
 * (authGraph, teamsGraph, teamApprovalGraph, playersGraph, childrenGraph).
 */
@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val session by authViewModel.session.collectAsState()
    val user = session.user
    val sec = SecuredContext(navController, authViewModel)

    // Redirección según la sesión: sin usuario -> login; con usuario -> inicio de su rol
    LaunchedEffect(session.checking, user?.uid) {
        if (session.checking) return@LaunchedEffect
        val current = navController.currentDestination?.route
        if (user == null) {
            if (current !in Routes.authRoutes || current == Routes.SPLASH) navController.clearAndGo(Routes.LOGIN)
        } else if (current in Routes.authRoutes) {
            navController.clearAndGo(Routes.HOME)
            if (session.justRegistered && user.roleEnum == Role.PAD) navController.navigate(Routes.CHILD_FORM)
            authViewModel.consumeJustRegistered()
        }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) { LoadingBox() }

        // Comunes
        composable(Routes.HOME) {
            sec.Secured("SportPro") { u -> HomeScreen(u) { route -> navController.navigate(route) } }
        }
        composable(Routes.PROFILE) {
            sec.Secured("Mi perfil") { u -> ProfileScreen(u, onLogout = { authViewModel.logout() }) }
        }

        // Módulos
        authGraph(navController, authViewModel)   // US-01
        childrenGraph(sec)                         // US-02
        teamsGraph(sec)                            // US-03
        playersGraph(sec)                          // US-04
        teamApprovalGraph(sec)                     // US-07
    }
}
