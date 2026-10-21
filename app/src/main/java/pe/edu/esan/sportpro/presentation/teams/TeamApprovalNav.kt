package pe.edu.esan.sportpro.presentation.teams

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.presentation.navigation.Routes
import pe.edu.esan.sportpro.presentation.navigation.SecuredContext
import pe.edu.esan.sportpro.presentation.navigation.Secured

/** US-07: validación de equipos por el Administrador. */
fun NavGraphBuilder.teamApprovalGraph(sec: SecuredContext) {
    composable(Routes.TEAM_APPROVAL) {
        sec.Secured("Validar equipos", setOf(Role.ADM)) { u -> TeamApprovalScreen(adminUid = u.uid) }
    }
}
