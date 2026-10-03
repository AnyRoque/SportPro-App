package pe.edu.esan.sportpro.presentation.players

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.presentation.navigation.Routes
import pe.edu.esan.sportpro.presentation.navigation.SecuredContext
import pe.edu.esan.sportpro.presentation.navigation.Secured

/** US-04: plantel y perfil de jugadores (Director Técnico). */
fun NavGraphBuilder.playersGraph(sec: SecuredContext) {
    val nav = sec.navController
    composable(Routes.ROSTER, arguments = listOf(navArgument("teamId") { type = NavType.StringType })) { entry ->
        val teamId = entry.arguments?.getString("teamId").orEmpty()
        sec.Secured("Plantel", setOf(Role.DT), showBack = true) { u ->
            RosterScreen(
                teamId = teamId,
                dtUid = u.uid,
                onAddPlayer = { nav.navigate(Routes.playerForm(teamId)) },
                onOpenPlayer = { p -> nav.navigate(Routes.playerDetail(p.id)) }
            )
        }
    }
    composable(
        Routes.PLAYER_FORM,
        arguments = listOf(
            navArgument("teamId") { type = NavType.StringType },
            navArgument("playerId") { type = NavType.StringType; nullable = true; defaultValue = null }
        )
    ) { entry ->
        val teamId = entry.arguments?.getString("teamId").orEmpty()
        val playerId = entry.arguments?.getString("playerId")
        sec.Secured(if (playerId == null) "Nuevo jugador" else "Editar jugador", setOf(Role.DT), showBack = true) { u ->
            PlayerFormScreen(teamId = teamId, playerId = playerId, dtUid = u.uid, onDone = { nav.popBackStack() })
        }
    }
    composable(Routes.PLAYER_DETAIL, arguments = listOf(navArgument("playerId") { type = NavType.StringType })) { entry ->
        val playerId = entry.arguments?.getString("playerId").orEmpty()
        sec.Secured("Perfil del jugador", setOf(Role.DT), showBack = true) {
            PlayerDetailScreen(
                playerId = playerId,
                onEdit = { p -> nav.navigate(Routes.playerForm(p.teamId, p.id)) }
            )
        }
    }
}
