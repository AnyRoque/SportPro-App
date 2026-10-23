package pe.edu.esan.sportpro.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

object AttendanceStatus {
    const val ATTENDED = "ASISTIO"
    const val LATE = "TARDANZA"
    const val ABSENT = "FALTA"
    const val JUSTIFIED = "JUSTIFICADO"

    val ALL = listOf(ATTENDED, LATE, ABSENT, JUSTIFIED)

    fun labelOf(status: String): String = when (status) {
        ATTENDED -> "Asistió"
        LATE -> "Tardanza"
        ABSENT -> "Falta"
        JUSTIFIED -> "Justificado"
        else -> "Sin marcar"
    }
}

/**
 * Registro de asistencia individual por jugador (US-13).
 */
data class PlayerAttendanceItem(
    val playerId: String = "",
    val playerName: String = "",
    val photoUrl: String = "",
    val shirtNumber: Int? = null,
    val position: String = "",
    val status: String = "", // "ASISTIO", "TARDANZA", "FALTA", "JUSTIFICADO"
    val justificationReason: String = ""
)

/**
 * Registro de asistencia completo de una sesión de entrenamiento (US-13).
 */
data class AttendanceRecordModel(
    @DocumentId var id: String = "", // Mismo ID que trainingId
    val trainingId: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val category: String = "",
    val dtUid: String = "",
    val dtName: String = "",
    val date: String = "", // Formato dd/MM/yyyy
    val items: List<PlayerAttendanceItem> = emptyList(),
    val totalAttended: Int = 0,
    val totalLate: Int = 0,
    val totalAbsent: Int = 0,
    val totalJustified: Int = 0,
    val editedBy: String = "",
    @ServerTimestamp val editedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    @get:Exclude
    val totalPlayers: Int get() = items.size

    @get:Exclude
    val isComplete: Boolean get() = items.all { it.status.isNotBlank() }
}

/**
 * Registro de asistencia en el historial personal del jugador (US-14).
 * Almacenado en players/{playerId}/attendanceHistory/{trainingId}.
 */
data class PlayerAttendanceRecord(
    @DocumentId var id: String = "",
    val trainingId: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val date: String = "", // Formato dd/MM/yyyy
    val status: String = "", // "ASISTIO", "TARDANZA", "FALTA", "JUSTIFICADO"
    val justificationReason: String = "",
    @ServerTimestamp val recordedAt: Timestamp? = null
)
