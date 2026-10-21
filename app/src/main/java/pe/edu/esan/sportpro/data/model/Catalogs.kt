package pe.edu.esan.sportpro.data.model

/** Listas fijas usadas en formularios. */
object Catalogs {
    val categories = listOf("Sub-8", "Sub-10", "Sub-12", "Sub-15", "Sub-17", "Primera")
    val positions = listOf("Portero", "Defensa central", "Lateral", "Mediocampista", "Extremo", "Delantero")
    val feet = listOf("Derecho", "Izquierdo", "Ambidiestro")
    val relations = listOf("Padre", "Madre", "Apoderado")
    val sexes = listOf("Masculino", "Femenino")
    val weekDays = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
}

/** Estados de un equipo (US-03 / US-07). */
object TeamStatus {
    const val PENDING = "PENDIENTE"
    const val APPROVED = "APROBADO"
    const val REJECTED = "RECHAZADO"
}

/** Estado del vínculo de un jugador con su equipo (US-02 / US-04). */
object LinkStatus {
    const val PENDING = "PENDIENTE"
    const val ACCEPTED = "ACEPTADO"
    const val REJECTED = "RECHAZADO"
}
