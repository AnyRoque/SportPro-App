package pe.edu.esan.sportpro.presentation.teams

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.presentation.navigation.Routes
import pe.edu.esan.sportpro.presentation.navigation.SecuredContext
import pe.edu.esan.sportpro.presentation.navigation.Secured

/** US-03: rutas de equipos del Director Técnico. */
fun NavGraphBuilder.teamsGraph(sec: SecuredContext) {
    val nav = sec.navController
    composable(Routes.MY_TEAMS) {
        sec.Secured("Mis equipos", setOf(Role.DT)) { u ->
            MyTeamsScreen(
                dtUid = u.uid,
                onNewTeam = { nav.navigate(Routes.teamForm()) },
                onOpenRoster = { team -> nav.navigate(Routes.roster(team.id)) },
                onFixTeam = { team -> nav.navigate(Routes.teamForm(team.id)) }
            )
        }
    }
    composable(
        Routes.TEAM_FORM,
        arguments = listOf(navArgument("teamId") { type = NavType.StringType; nullable = true; defaultValue = null })
    ) { entry ->
        val teamId = entry.arguments?.getString("teamId")
        sec.Secured(if (teamId == null) "Nuevo equipo" else "Corregir equipo", setOf(Role.DT), showBack = true) { u ->
            TeamFormScreen(dt = u, teamId = teamId, onDone = { nav.popBackStack() })
        }
    }
}
