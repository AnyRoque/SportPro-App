package pe.edu.esan.sportpro.presentation.children

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.presentation.navigation.Routes
import pe.edu.esan.sportpro.presentation.navigation.SecuredContext
import pe.edu.esan.sportpro.presentation.navigation.Secured

/** US-02: rutas del padre de familia. */
fun NavGraphBuilder.childrenGraph(sec: SecuredContext) {
    val nav = sec.navController
    composable(Routes.MY_CHILDREN) {
        sec.Secured("Mis hijos", setOf(Role.PAD)) { u ->
            MyChildrenScreen(parentUid = u.uid, onAddChild = { nav.navigate(Routes.CHILD_FORM) })
        }
    }
    composable(Routes.CHILD_FORM) {
        sec.Secured("Registro de hijos", setOf(Role.PAD), showBack = true) { u ->
            ChildFormScreen(parent = u, onDone = {
                nav.navigate(Routes.MY_CHILDREN) { popUpTo(Routes.HOME); launchSingleTop = true }
            })
        }
    }
}
