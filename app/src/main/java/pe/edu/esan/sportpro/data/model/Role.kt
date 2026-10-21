package pe.edu.esan.sportpro.data.model

/**
 * Roles del sistema (stakeholders del proyecto).
 * ADM no puede autorregistrarse: se crea por formulario interno (US-06).
 */
enum class Role(val label: String) {
    ADM("Administrador"),
    DT("Director Técnico"),
    JUG("Jugador"),
    PAD("Padre de familia");

    companion object {
        /** Roles disponibles en la pantalla de registro público (US-01). */
        val selfRegister = listOf(DT, JUG, PAD)

        fun fromOrNull(value: String?): Role? = entries.firstOrNull { it.name == value }
    }
}
