package pe.edu.esan.sportpro.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.presentation.auth.AuthViewModel
import pe.edu.esan.sportpro.presentation.components.AccessDenied
import pe.edu.esan.sportpro.presentation.components.LoadingBox

/** Lo que necesita cada módulo para registrar sus rutas protegidas. */
class SecuredContext(val navController: NavHostController, val authViewModel: AuthViewModel)

/**
 * Envuelve una pantalla con el menú lateral y verifica que el rol del usuario esté autorizado.
 * Si un usuario intenta abrir una ruta de otro rol, ve "Acceso no autorizado" (US-01).
 */
@Composable
fun SecuredContext.Secured(
    title: String,
    allowed: Set<Role> = Role.entries.toSet(),
    showBack: Boolean = false,
    content: @Composable (UserModel) -> Unit
) {
    val session by authViewModel.session.collectAsState()
    val user = session.user
    if (user == null) {
        LoadingBox()
        return
    }
    DrawerScaffold(navController, user, title, onLogout = { authViewModel.logout() }, showBack = showBack) {
        if (user.roleEnum in allowed) content(user) else AccessDenied()
    }
}

/** Navega a [route] limpiando todo el back stack. */
fun NavHostController.clearAndGo(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
