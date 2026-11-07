package pe.edu.esan.sportpro.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

// =====================================================================
// US-22: Anuncios internos
// =====================================================================

object AnnouncementType {
    const val CALL_UP = "Convocatoria"
    const val TRAINING = "Entrenamiento"
    const val SCHEDULE = "Cambio de horario"
    const val GENERAL = "General"
    val ALL = listOf(CALL_UP, TRAINING, SCHEDULE, GENERAL)
}

/** Documento announcements/{id} */
data class AnnouncementModel(
    @DocumentId var id: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = AnnouncementType.GENERAL,
    /** Vacío = toda la academia (solo ADM) */
    val teamId: String = "",
    val teamName: String = "",
    /** Roles destinatarios: DT, JUG, PAD */
    val targetRoles: List<String> = listOf("DT", "JUG", "PAD"),
    /** dd/MM/yyyy: después de esta fecha pasa a Archivados */
    val validUntil: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorRole: String = "",
    val edited: Boolean = false,
    val readBy: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

// =====================================================================
// US-23: Comunidad
// =====================================================================

object PostVisibility {
    const val TEAM = "MI_EQUIPO"
    const val COMMUNITY = "COMUNIDAD"
    fun label(v: String) = if (v == TEAM) "Mi equipo" else "Comunidad"
}

object PostStatus {
    const val VISIBLE = "VISIBLE"
    /** Oculto automáticamente por 3 reportes o por el ADM */
    const val HIDDEN = "OCULTO"
    const val DELETED = "ELIMINADO"
}

object ReactionType {
    const val LIKE = "👍"
    const val CLAP = "👏"
    const val FIRE = "🔥"
    val ALL = listOf(LIKE, CLAP, FIRE)
}

/** Documento posts/{id} */
data class PostModel(
    @DocumentId var id: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val visibility: String = PostVisibility.TEAM,
    val teamId: String = "",
    val teamName: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorRole: String = "",
    /** uid -> reacción */
    val reactions: Map<String, String> = emptyMap(),
    val commentCount: Int = 0,
    val reportCount: Int = 0,
    val status: String = PostStatus.VISIBLE,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    @get:Exclude
    val reactionCount: Int get() = reactions.size
}

/** Documento posts/{postId}/comments/{id} */
data class CommentModel(
    @DocumentId var id: String = "",
    val postId: String = "",
    val text: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorRole: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

// =====================================================================
// US-24: Reportes y moderación
// =====================================================================

object ReportReason {
    const val OFFENSIVE = "Lenguaje ofensivo"
    const val HARASSMENT = "Acoso"
    const val INAPPROPRIATE = "Contenido inapropiado"
    const val SPAM = "Spam o fraude"
    const val MINORS_DATA = "Datos personales de menores"
    const val OTHER = "Otro"
    val ALL = listOf(OFFENSIVE, HARASSMENT, INAPPROPRIATE, SPAM, MINORS_DATA, OTHER)
}

object ReportStatus {
    const val PENDING = "PENDIENTE"
    const val RESOLVED = "RESUELTO"
}

object ModerationAction {
    const val KEEP = "MANTENER"
    const val HIDE = "OCULTAR"
    const val DELETE = "ELIMINAR"
}

/** Documento reports/{postId_reporterUid} (un usuario no puede reportar dos veces lo mismo) */
data class ReportModel(
    @DocumentId var id: String = "",
    val postId: String = "",
    val postText: String = "",
    val postAuthorUid: String = "",
    val postAuthorName: String = "",
    val reason: String = "",
    val detail: String = "",
    val reporterUid: String = "",
    val status: String = ReportStatus.PENDING,
    val action: String = "",
    val resolvedBy: String = "",
    val resolvedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

// =====================================================================
// US-25 a US-27: Resumen narrativo del partido con IA
// =====================================================================

object SummaryStatus {
    const val PENDING_REVIEW = "PENDIENTE_REVISION"
    const val PUBLISHED = "PUBLICADO"
    const val REJECTED = "RECHAZADO"
}

/** Documento summaries/{matchId} (un resumen vigente por partido) */
data class MatchSummaryModel(
    @DocumentId var id: String = "",
    val matchId: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val rival: String = "",
    val date: String = "",
    val ownScore: Int = 0,
    val rivalScore: Int = 0,
    /** Texto original generado por la IA (no se modifica) */
    val aiFacts: String = "",
    val aiAnalysis: String = "",
    /** Texto final editado por el DT */
    val finalFacts: String = "",
    val finalAnalysis: String = "",
    val warnings: List<String> = emptyList(),
    val status: String = SummaryStatus.PENDING_REVIEW,
    val generation: Int = 1,
    val model: String = "",
    val promptVersion: String = "",
    val memberUids: List<String> = emptyList(),
    val createdBy: String = "",
    val approvedBy: String = "",
    val approvedByName: String = "",
    val approvedAt: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
