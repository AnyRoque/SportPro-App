package pe.edu.esan.sportpro.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

object RequestStatus {
    const val PENDING = "PENDIENTE"
    const val APPROVED = "APROBADO"
    const val REJECTED = "RECHAZADO"
}

/**
 * Modelo para solicitudes de anulación de sanciones/faltas enviadas por el DT Rival al Administrador (US-20).
 * Colección: eventCancellationRequests/{requestId}
 */
data class EventCancellationRequestModel(
    @DocumentId var id: String = "",
    val matchId: String = "",
    val matchTitle: String = "",
    val eventId: String = "",
    val eventSummary: String = "",
    val requestingDtUid: String = "",
    val requestingDtName: String = "",
    val requestingTeamName: String = "",
    val justification: String = "", // Motivo de justificación obligatorio
    val status: String = RequestStatus.PENDING,
    val adminResponseComment: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val resolvedAt: Timestamp? = null
)
