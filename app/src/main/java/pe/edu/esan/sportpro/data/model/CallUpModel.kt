package pe.edu.esan.sportpro.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import pe.edu.esan.sportpro.util.Validators
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object CallUpStatus {
    const val PENDING = "PENDIENTE"
    const val CONFIRMED = "CONFIRMADO"
    const val REJECTED = "RECHAZADO"
    const val NO_RESPONSE = "SIN_RESPUESTA"
}

object RejectReason {
    const val INJURY = "Lesión"
    const val TRAVEL = "Viaje"
    const val STUDIES = "Estudios"
    const val OTHER = "Otro"

    val ALL = listOf(INJURY, TRAVEL, STUDIES, OTHER)
}

object TacticalFormations {
    val F11 = listOf("4-3-3", "4-4-2", "3-5-2", "4-2-3-1", "3-4-3")
    val F9 = listOf("3-3-2", "3-4-1", "2-4-2", "3-2-3")
    val F7 = listOf("2-3-1", "3-2-1", "2-2-2", "3-1-2")

    fun forModality(modality: Int): List<String> = when (modality) {
        7 -> F7
        9 -> F9
        11 -> F11
        else -> F11
    }
}

/**
 * Documento matches/{matchId}/callups/{playerId} (US-17: Convocatoria y Disponibilidad).
 */
data class CallUpItemModel(
    @DocumentId var id: String = "", // playerId
    val matchId: String = "",
    val playerId: String = "",
    val playerName: String = "",
    val playerPhotoUrl: String = "",
    val position: String = "",
    val isMinor: Boolean = false,
    val isStarter: Boolean = false, // Titular (true) vs Suplente (false)
    val userUid: String = "",
    val parentUid: String = "",
    val responseStatus: String = CallUpStatus.PENDING,
    val rejectReason: String = "",
    val rejectComment: String = "",
    val deadlineDate: String = "", // Formato dd/MM/yyyy
    val deadlineTime: String = "", // Formato HH:mm
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val isExpired: Boolean
        get() {
            if (deadlineDate.isBlank()) return false
            val date = try {
                val parts = deadlineDate.trim().split("/", "-")
                if (parts.size == 3) {
                    val day = parts[0].padStart(2, '0')
                    val month = parts[1].padStart(2, '0')
                    val year = parts[2]
                    Validators.parseDate("$day/$month/$year")
                } else {
                    Validators.parseDate(deadlineDate)
                }
            } catch (_: Exception) {
                null
            } ?: return false

            val time = try {
                val timeParts = deadlineTime.trim().split(":")
                val hour = timeParts.getOrNull(0)?.padStart(2, '0') ?: "23"
                val min = timeParts.getOrNull(1)?.padStart(2, '0') ?: "59"
                LocalTime.parse("$hour:$min", DateTimeFormatter.ofPattern("HH:mm"))
            } catch (_: Exception) {
                LocalTime.of(23, 59)
            }

            val deadlineDateTime = LocalDateTime.of(date, time)
            return LocalDateTime.now().isAfter(deadlineDateTime)
        }

    @get:Exclude
    val computedStatus: String
        get() {
            return if (responseStatus == CallUpStatus.PENDING && isExpired) {
                CallUpStatus.NO_RESPONSE
            } else {
                responseStatus
            }
        }
}
