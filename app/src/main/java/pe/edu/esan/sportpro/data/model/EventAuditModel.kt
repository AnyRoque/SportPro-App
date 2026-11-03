package pe.edu.esan.sportpro.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

object AuditChangeType {
    const val EDIT = "EDICION"
    const val CANCELLATION = "ANULACION"
}

/**
 * Modelo para la bitácora de auditoría y trazabilidad de cambios en eventos del partido (US-20).
 * Subcolección: matches/{matchId}/events/{eventId}/history/{historyId}
 */
data class EventAuditModel(
    @DocumentId var id: String = "",
    val eventId: String = "",
    val matchId: String = "",
    val tipoCambio: String = AuditChangeType.EDIT, // EDICION o ANULACION
    val usuarioUid: String = "",
    val usuarioNombre: String = "",
    val motivo: String = "", // Campo obligatorio
    val valorOriginal: String = "",
    val valorNuevo: String = "",
    @ServerTimestamp val fechaHora: Timestamp? = null
)
