package pe.edu.esan.sportpro.presentation.auth

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import pe.edu.esan.sportpro.presentation.navigation.Routes

/** US-01: rutas de autenticación. */
fun NavGraphBuilder.authGraph(navController: NavHostController, authViewModel: AuthViewModel) {
    composable(Routes.LOGIN) {
        LoginScreen(
            viewModel = authViewModel,
            onGoRegister = { navController.navigate(Routes.REGISTER) },
            onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) }
        )
    }
    composable(Routes.REGISTER) {
        RegisterScreen(viewModel = authViewModel, onGoLogin = { navController.popBackStack() })
    }
    composable(Routes.FORGOT_PASSWORD) {
        ForgotPasswordScreen(viewModel = authViewModel, onBack = { navController.popBackStack() })
    }
}
