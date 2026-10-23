package pe.edu.esan.sportpro.presentation.training

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
 * Módulo de navegación para Planificación de Entrenamientos (US-11).
 */
fun NavGraphBuilder.trainingGraph(sec: SecuredContext) {
    composable(Routes.TRAININGS) {
        val viewModel: TrainingViewModel = viewModel()
        sec.Secured("Entrenamientos", allowed = setOf(Role.DT, Role.JUG, Role.PAD)) { user ->
            TrainingListScreen(
                user = user,
                viewModel = viewModel,
                onNavigateForm = { trainingId, duplicateFromId ->
                    sec.navController.navigate(Routes.trainingForm(trainingId, duplicateFromId))
                },
                onNavigateDetail = { trainingId ->
                    sec.navController.navigate(Routes.trainingDetail(trainingId))
                },
                onNavigateAttendance = { trainingId, teamId ->
                    sec.navController.navigate(Routes.attendance(trainingId, teamId))
                }
            )
        }
    }

    composable(
        route = Routes.TRAINING_FORM,
        arguments = listOf(
            navArgument("trainingId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument("duplicateFromId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val trainingId = backStackEntry.arguments?.getString("trainingId")
        val duplicateFromId = backStackEntry.arguments?.getString("duplicateFromId")
        val viewModel: TrainingViewModel = viewModel()

        sec.Secured("Planificar Entrenamiento", allowed = setOf(Role.DT), showBack = true) { user ->
            TrainingFormScreen(
                user = user,
                viewModel = viewModel,
                trainingId = trainingId,
                duplicateFromId = duplicateFromId,
                onBack = { sec.navController.popBackStack() }
            )
        }
    }

    composable(
        route = Routes.TRAINING_DETAIL,
        arguments = listOf(
            navArgument("trainingId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val trainingId = backStackEntry.arguments?.getString("trainingId") ?: ""
        sec.Secured("Detalle de Entrenamiento", allowed = setOf(Role.DT, Role.JUG, Role.PAD), showBack = true) {
            TrainingDetailScreen(
                trainingId = trainingId,
                onBack = { sec.navController.popBackStack() }
            )
        }
    }

    // US-13: Registro de Asistencia
    composable(
        route = Routes.ATTENDANCE,
        arguments = listOf(
            navArgument("trainingId") { type = NavType.StringType },
            navArgument("teamId") { type = NavType.StringType; defaultValue = "" }
        )
    ) { backStackEntry ->
        val trainingId = backStackEntry.arguments?.getString("trainingId") ?: ""
        val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
        val viewModel: AttendanceViewModel = viewModel()

        sec.Secured("Registro de Asistencia", allowed = setOf(Role.DT), showBack = true) { user ->
            AttendanceScreen(
                user = user,
                viewModel = viewModel,
                trainingId = trainingId,
                teamId = teamId,
                onBack = { sec.navController.popBackStack() }
            )
        }
    }

    composable(
        route = Routes.ATTENDANCE_HISTORY,
        arguments = listOf(
            navArgument("teamId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val teamId = backStackEntry.arguments?.getString("teamId") ?: ""

        sec.Secured("Historial de Asistencias", allowed = setOf(Role.DT, Role.JUG, Role.PAD), showBack = true) { user ->
            AttendanceHistoryScreen(
                user = user,
                teamId = teamId,
                onNavigateAttendance = { trainingId ->
                    sec.navController.navigate(Routes.attendance(trainingId, teamId))
                },
                onBack = { sec.navController.popBackStack() }
            )
        }
    }

    // US-14: Consulta de Asistencia Personal (JUG / PAD)
    composable(Routes.USER_ATTENDANCE_HISTORY) {
        val viewModel: UserAttendanceViewModel = viewModel()

        sec.Secured("Historial de Asistencia", allowed = setOf(Role.JUG, Role.PAD), showBack = true) { user ->
            UserAttendanceScreen(
                user = user,
                viewModel = viewModel,
                onBack = { sec.navController.popBackStack() }
            )
        }
    }
}
