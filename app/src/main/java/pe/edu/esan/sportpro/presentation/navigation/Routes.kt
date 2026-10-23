package pe.edu.esan.sportpro.presentation.navigation

/** Rutas de navegación (Navigation Compose). */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home"
    const val PROFILE = "profile"

    // DT – equipos (US-03)
    const val MY_TEAMS = "my_teams"
    const val TEAM_FORM = "team_form?teamId={teamId}"
    fun teamForm(teamId: String? = null) = if (teamId == null) "team_form" else "team_form?teamId=$teamId"

    // ADM – Panel de administración (US-06)
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val TEAM_APPROVAL = "team_approval"

    // DT – jugadores (US-04)
    const val ROSTER = "roster/{teamId}"
    fun roster(teamId: String) = "roster/$teamId"
    const val PLAYER_FORM = "player_form/{teamId}?playerId={playerId}"
    fun playerForm(teamId: String, playerId: String? = null) =
        if (playerId == null) "player_form/$teamId" else "player_form/$teamId?playerId=$playerId"
    const val PLAYER_DETAIL = "player_detail/{playerId}"
    fun playerDetail(playerId: String) = "player_detail/$playerId"

    // PAD – hijos (US-02)
    const val MY_CHILDREN = "my_children"
    const val CHILD_FORM = "child_form"

    // DT / JUG / PAD – Entrenamientos (US-11)
    const val TRAININGS = "trainings"
    const val TRAINING_FORM = "training_form?trainingId={trainingId}&duplicateFromId={duplicateFromId}"
    fun trainingForm(trainingId: String? = null, duplicateFromId: String? = null): String {
        val tParam = trainingId ?: ""
        val dParam = duplicateFromId ?: ""
        return "training_form?trainingId=$tParam&duplicateFromId=$dParam"
    }
    const val TRAINING_DETAIL = "training_detail/{trainingId}"
    fun trainingDetail(trainingId: String) = "training_detail/$trainingId"

    // DT – Asistencia a entrenamientos (US-13)
    const val ATTENDANCE = "attendance?trainingId={trainingId}&teamId={teamId}"
    fun attendance(trainingId: String, teamId: String = "") = "attendance?trainingId=$trainingId&teamId=$teamId"
    const val ATTENDANCE_HISTORY = "attendance_history/{teamId}"
    fun attendanceHistory(teamId: String) = "attendance_history/$teamId"

    // JUG / PAD – Consulta de Historial de Asistencia Personal (US-14)
    const val USER_ATTENDANCE_HISTORY = "user_attendance_history"

    val authRoutes = setOf(SPLASH, LOGIN, REGISTER, FORGOT_PASSWORD)
}
