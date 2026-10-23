package pe.edu.esan.sportpro.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

object TrainingStatus {
    const val PROGRAMMED = "PROGRAMADA"
    const val COMPLETED = "REALIZADA"
    const val CANCELED = "CANCELADA"
}

object ObjectiveType {
    const val TECHNICAL = "TÉCNICO"
    const val TACTICAL = "TÁCTICO"
    const val PHYSICAL = "FÍSICO"
    const val OTHER = "OTRO"

    val ALL = listOf(TECHNICAL, TACTICAL, PHYSICAL, OTHER)
}

/**
 * Ejercicio individual dentro de una sesión de entrenamiento (US-11, US-12).
 */
data class ExerciseModel(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = ObjectiveType.TECHNICAL,
    val durationMinutes: Int = 10
)

/**
 * Documento trainings/{trainingId} (US-11: Planificación de sesiones de entrenamiento).
 */
data class TrainingModel(
    @DocumentId var id: String = "",
    val dtUid: String = "",
    val dtName: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val category: String = "",
    val date: String = "", // Formato dd/MM/yyyy
    val startTime: String = "17:00", // Formato HH:mm
    val durationMinutes: Int = 60,
    val location: String = "",
    val objectiveType: String = ObjectiveType.TECHNICAL,
    val objectiveDescription: String = "",
    val exercises: List<ExerciseModel> = emptyList(),
    val status: String = TrainingStatus.PROGRAMMED,
    val cancelReason: String = "",
    val duplicatedFromId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    @get:Exclude
    val totalExerciseDuration: Int get() = exercises.sumOf { it.durationMinutes }

    @get:Exclude
    val isValidDuration: Boolean get() = totalExerciseDuration <= durationMinutes
}

/**
 * Notificación enviada a jugadores y padres cuando se crea o cancela un entrenamiento.
 */
data class NotificationModel(
    @DocumentId var id: String = "",
    val recipientUid: String = "",
    val teamId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "TRAINING",
    val relatedId: String = "",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)
