package pe.edu.esan.sportpro.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

object EventType {
    const val GOAL = "GOL"
    const val YELLOW_CARD = "TARJETA_AMARILLA"
    const val RED_CARD = "TARJETA_ROJA"
    const val SUBSTITUTION = "CAMBIO"
    const val FOUL = "FALTA"
    const val SHOT_ON_TARGET = "AL_ARCO"
    const val SHOT_OFF_TARGET = "DESVIADO"
    const val CORNER = "CORNER"
    const val OFFSIDE = "FUERA_DE_JUEGO"
    const val PERIOD_START = "INICIO_PERIODO"
    const val PERIOD_END = "FIN_PERIODO"

    val QUICK_EVENTS = listOf(
        GOAL,
        YELLOW_CARD,
        RED_CARD,
        SUBSTITUTION,
        FOUL,
        SHOT_ON_TARGET,
        SHOT_OFF_TARGET,
        CORNER,
        OFFSIDE
    )

    fun labelOf(type: String): String = when (type) {
        GOAL -> "Gol"
        YELLOW_CARD -> "Tarjeta Amarilla"
        RED_CARD -> "Tarjeta Roja"
        SUBSTITUTION -> "Cambio"
        FOUL -> "Falta"
        SHOT_ON_TARGET -> "Tiro al Arco"
        SHOT_OFF_TARGET -> "Tiro Desviado"
        CORNER -> "Tiro de Esquina"
        OFFSIDE -> "Fuera de Juego"
        PERIOD_START -> "Inicio de Periodo"
        PERIOD_END -> "Fin de Periodo"
        else -> type
    }
}

object EventTeam {
    const val OWN = "PROPIO"
    const val RIVAL = "RIVAL"
}

object EventStatus {
    const val ACTIVE = "ACTIVO"
    const val CANCELED = "ANULADO"
}

/**
 * Modelo para eventos registrados en tiempo real durante un partido (US-19).
 */
data class MatchEventModel(
    @DocumentId var id: String = "",
    val matchId: String = "",
    val type: String = EventType.GOAL,
    val minute: Int = 1,
    val minutoCorregido: Boolean = false,
    val team: String = EventTeam.OWN,
    val playerId: String? = null,
    val playerName: String = "",
    val jugadorNoIdentificado: Boolean = false,
    val jugadorSaleId: String? = null,
    val jugadorSaleName: String = "",
    val jugadorEntraId: String? = null,
    val jugadorEntraName: String = "",
    val observation: String = "",
    val registeredBy: String = "",
    val registeredByName: String = "",
    val status: String = EventStatus.ACTIVE,
    val pendingSync: Boolean = false,
    @ServerTimestamp val registeredAt: Timestamp? = null
)
