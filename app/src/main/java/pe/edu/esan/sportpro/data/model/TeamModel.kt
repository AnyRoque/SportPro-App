package pe.edu.esan.sportpro.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/** Documento teams/{teamId} (US-03 / US-07) */
data class TeamModel(
    @DocumentId var id: String = "",
    val name: String = "",
    /** nombre normalizado + categoría, usado para evitar duplicados */
    val nameKey: String = "",
    val category: String = "",
    val field: String = "",
    val district: String = "",
    val trainingDays: List<String> = emptyList(),
    val trainingTime: String = "",
    val dtUid: String = "",
    val dtName: String = "",
    val status: String = TeamStatus.PENDING,
    val rejectReason: String = "",
    val reviewedBy: String = "",
    val reviewedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)
