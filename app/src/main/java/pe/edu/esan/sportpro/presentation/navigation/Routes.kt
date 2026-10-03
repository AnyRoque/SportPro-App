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

    // ADM – validación de equipos (US-07)
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

    val authRoutes = setOf(SPLASH, LOGIN, REGISTER, FORGOT_PASSWORD)
}
