package pe.edu.esan.sportpro.presentation.matches

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.presentation.navigation.Routes
import pe.edu.esan.sportpro.presentation.navigation.Secured
import pe.edu.esan.sportpro.presentation.navigation.SecuredContext

/**
 * Módulo de navegación para Partidos, Convocatorias y Alineaciones (US-16 / US-17 / US-18).
 */
fun NavGraphBuilder.matchesGraph(sec: SecuredContext) {
    composable(Routes.MATCHES) {
        val viewModel: MatchViewModel = viewModel()
        sec.Secured("Partidos", allowed = setOf(Role.DT, Role.JUG, Role.PAD)) { user ->
            MatchListScreen(
                user = user,
                viewModel = viewModel,
                onNavigateForm = { matchId ->
                    sec.navController.navigate(Routes.matchForm(matchId))
                },
                onNavigateDetail = { matchId ->
                    sec.navController.navigate(Routes.matchDetail(matchId))
                },
                onNavigateCallUp = { matchId, hasCallUp ->
                    if (hasCallUp || user.roleEnum != Role.DT) {
                        sec.navController.navigate(Routes.callUpPanel(matchId))
                    } else {
                        sec.navController.navigate(Routes.callUpForm(matchId))
                    }
                },
                onNavigateRegisterChild = {
                    sec.navController.navigate(Routes.CHILD_FORM)
                },
                onNavigateJoinTeam = {
                    sec.navController.navigate(Routes.JOIN_TEAM)
                }
            )
        }
    }

    composable(
        route = Routes.MATCH_FORM,
        arguments = listOf(
            navArgument("matchId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val matchId = backStackEntry.arguments?.getString("matchId")
        val viewModel: MatchViewModel = viewModel()

        sec.Secured(
            title = if (matchId.isNullOrBlank()) "Programar Partido" else "Editar Partido",
            allowed = setOf(Role.DT),
            showBack = true
        ) { user ->
            MatchFormScreen(
                user = user,
                viewModel = viewModel,
                matchId = matchId,
                onBack = { sec.navController.popBackStack() }
            )
        }
    }

    composable(
        route = Routes.MATCH_DETAIL,
        arguments = listOf(
            navArgument("matchId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
        sec.Secured("Detalle de Partido", allowed = setOf(Role.DT, Role.JUG, Role.PAD), showBack = true) {
            MatchDetailScreen(
                matchId = matchId,
                onNavigateCallUp = { hasCallUp ->
                    if (hasCallUp || sec.authViewModel.session.value.user?.roleEnum != Role.DT) {
                        sec.navController.navigate(Routes.callUpPanel(matchId))
                    } else {
                        sec.navController.navigate(Routes.callUpForm(matchId))
                    }
                },
                onNavigateLineup = {
                    sec.navController.navigate(Routes.lineup(matchId))
                },
                onNavigateConsole = {
                    sec.navController.navigate(Routes.matchConsole(matchId))
                },
                onNavigateLive = {
                    sec.navController.navigate(Routes.liveMatch(matchId))
                },
                onBack = { sec.navController.popBackStack() }
            )
        }
    }

    // US-17: Convocatoria Formulario (DT)
    composable(
        route = Routes.CALL_UP_FORM,
        arguments = listOf(
            navArgument("matchId") { type = NavType.StringType },
            navArgument("isAdding") { type = NavType.BoolType; defaultValue = false }
        )
    ) { backStackEntry ->
        val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
        val isAdding = backStackEntry.arguments?.getBoolean("isAdding") ?: false
        val viewModel: CallUpViewModel = viewModel()

        sec.Secured("Convocatoria", allowed = setOf(Role.DT), showBack = true) { user ->
            CallUpFormScreen(
                user = user,
                viewModel = viewModel,
                matchId = matchId,
                isAdding = isAdding,
                onBack = { sec.navController.popBackStack() }
            )
        }
    }

    // US-17: Panel de Convocatoria (DT / JUG / PAD)
    composable(
        route = Routes.CALL_UP_PANEL,
        arguments = listOf(
            navArgument("matchId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
        val viewModel: CallUpViewModel = viewModel()

        sec.Secured("Panel de Convocatoria", allowed = setOf(Role.DT, Role.JUG, Role.PAD), showBack = true) { user ->
            CallUpPanelScreen(
                user = user,
                viewModel = viewModel,
                matchId = matchId,
                onNavigateAddPlayers = {
                    sec.navController.navigate(Routes.callUpForm(matchId, isAdding = true))
                },
                onNavigateLineup = {
                    sec.navController.navigate(Routes.lineup(matchId))
                },
                onBack = { sec.navController.popBackStack() }
            )
        }
    }

    // US-18: Alineación Táctica (DT / JUG / PAD)
    composable(
        route = Routes.LINEUP,
        arguments = listOf(
            navArgument("matchId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
        val viewModel: LineupViewModel = viewModel()

        sec.Secured("Alineación Táctica", allowed = setOf(Role.DT, Role.JUG, Role.PAD), showBack = true) { user ->
            LineupScreen(
                user = user,
                viewModel = viewModel,
                matchId = matchId,
                onBack = { sec.navController.popBackStack() }
            )
        }
    }

    // US-19: Consola de Registro de Eventos en Tiempo Real (DT / Operador)
    composable(
        route = Routes.MATCH_CONSOLE,
        arguments = listOf(
            navArgument("matchId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
        val viewModel: MatchConsoleViewModel = viewModel()

        sec.Secured("Consola de Registro en Vivo", allowed = setOf(Role.DT, Role.JUG, Role.PAD), showBack = true) { user ->
            MatchConsoleScreen(
                user = user,
                viewModel = viewModel,
                matchId = matchId,
                onNavigateLineup = {
                    sec.navController.navigate(Routes.lineup(matchId))
                },
                onBack = { sec.navController.popBackStack() }
            )
        }
    }

    // US-21: Partido en Vivo Sin Recarga (DT / JUG / PAD)
    composable(
        route = Routes.LIVE_MATCH,
        arguments = listOf(
            navArgument("matchId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
        val viewModel: LiveMatchViewModel = viewModel()

        sec.Secured("Partido en Vivo", allowed = setOf(Role.DT, Role.JUG, Role.PAD), showBack = true) { user ->
            LiveMatchScreen(
                user = user,
                viewModel = viewModel,
                matchId = matchId,
                onBack = { sec.navController.popBackStack() }
            )
        }
    }
}
