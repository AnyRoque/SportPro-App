package pe.edu.esan.sportpro.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

object MatchStatus {
    const val PROGRAMMED = "PROGRAMADO"
    const val IN_PROGRESS = "EN_CURSO"
    const val FINISHED = "FINALIZADO"
    const val CANCELED = "CANCELADO"
}

object MatchCondition {
    const val LOCAL = "LOCAL"
    const val VISITOR = "VISITANTE"

    val ALL = listOf(LOCAL, VISITOR)
}

object MatchType {
    const val FRIENDLY = "Amistoso"
    const val TOURNAMENT = "Torneo"
    const val LEAGUE = "Liga"

    val ALL = listOf(FRIENDLY, TOURNAMENT, LEAGUE)
}

object MatchModality {
    const val F7 = 7
    const val F9 = 9
    const val F11 = 11

    val ALL = listOf(F7, F9, F11)

    fun labelOf(modality: Int): String = when (modality) {
        F7 -> "Fútbol 7"
        F9 -> "Fútbol 9"
        F11 -> "Fútbol 11"
        else -> "Fútbol $modality"
    }
}

/**
 * Documento matches/{matchId} (US-16: Programación de Partidos).
 */
data class MatchModel(
    @DocumentId var id: String = "",
    val dtUid: String = "",
    val dtName: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val category: String = "",
    val rival: String = "",
    val matchType: String = MatchType.FRIENDLY,
    val condition: String = MatchCondition.LOCAL,
    val modality: Int = MatchModality.F7,
    val periodMinutes: Int = 30,
    val date: String = "", // Formato dd/MM/yyyy
    val time: String = "10:00", // Formato HH:mm
    val location: String = "",
    val notes: String = "",
    val operatorUid: String = "",
    val operatorName: String = "",
    val memberUids: List<String> = emptyList(),
    val status: String = MatchStatus.PROGRAMMED,
    val cancelReason: String = "",
    val hasCallUp: Boolean = false,
    val callUpDeadlineDate: String = "",
    val callUpDeadlineTime: String = "",
    val formation: String = "",
    val hasLineup: Boolean = false,
    val score: Map<String, Int> = mapOf("own" to 0, "rival" to 0),
    val timerStartTime: Timestamp? = null,
    val timerAccumulatedSeconds: Long = 0L,
    val timerIsRunning: Boolean = false,
    val currentPeriod: Int = 0,
    val timerPauseTime: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    @get:Exclude
    val ownScore: Int get() = score["own"] ?: 0

    @get:Exclude
    val rivalScore: Int get() = score["rival"] ?: 0

    @get:Exclude
    val totalMatchDuration: Int get() = periodMinutes * 2

    @get:Exclude
    val calculatedElapsedSeconds: Long
        get() {
            if (!timerIsRunning || timerStartTime == null) return timerAccumulatedSeconds
            val nowSeconds = System.currentTimeMillis() / 1000L
            val startSeconds = timerStartTime.seconds
            val elapsedSinceStart = (nowSeconds - startSeconds).coerceAtLeast(0L)
            return timerAccumulatedSeconds + elapsedSinceStart
        }

    @get:Exclude
    val calculatedCurrentMinute: Int
        get() {
            val totalSeconds = calculatedElapsedSeconds
            val minutes = (totalSeconds / 60L).toInt() + 1
            return minutes.coerceAtLeast(1)
        }
}
