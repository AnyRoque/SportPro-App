package pe.edu.esan.sportpro.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.ExerciseModel
import pe.edu.esan.sportpro.data.model.NotificationModel
import pe.edu.esan.sportpro.data.model.ObjectiveType
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.TeamStatus
import pe.edu.esan.sportpro.data.model.TrainingModel
import pe.edu.esan.sportpro.data.model.TrainingStatus
import pe.edu.esan.sportpro.data.model.UserModel
import java.util.UUID

/**
 * Repositorio de Cloud Firestore para la gestión de Sesiones de Entrenamiento (US-11).
 */
object TrainingRepository {

    private const val TRAININGS = "trainings"
    private const val TEAMS = "teams"
    private const val PLAYERS = "players"
    private const val NOTIFICATIONS = "notifications"

    private val firestore = FirebaseFirestore.getInstance()
    private val trainings get() = firestore.collection(TRAININGS)
    private val teams get() = firestore.collection(TEAMS)
    private val players get() = firestore.collection(PLAYERS)
    private val notifications get() = firestore.collection(NOTIFICATIONS)

    /**
     * Biblioteca predefinida de ejercicios para facilitar la planificación (US-12).
     */
    fun getExerciseLibrary(): List<ExerciseModel> {
        return listOf(
            ExerciseModel(
                id = "lib_1",
                name = "Rondo 4 vs 2 con transición rápida",
                description = "Mantener la posesión del balón a 1 o 2 toques. Al recuperar, buscar pase de salida.",
                type = ObjectiveType.TECHNICAL,
                durationMinutes = 15
            ),
            ExerciseModel(
                id = "lib_2",
                name = "Control orientativo y tiro a puerta",
                description = "Recibir pase filtrado, realizar control orientativo sobre marca pasiva y definir al poste lejano.",
                type = ObjectiveType.TECHNICAL,
                durationMinutes = 15
            ),
            ExerciseModel(
                id = "lib_3",
                name = "Presión alta y repliegue defensivo 5v5",
                description = "Trabajo táctico de bloque alto. Provocar el error en salida rival y replegar si superan primera línea.",
                type = ObjectiveType.TACTICAL,
                durationMinutes = 20
            ),
            ExerciseModel(
                id = "lib_4",
                name = "Salida de balón bajo presión desde el arquero",
                description = "Automatismo de salida con centrales abiertos y pivote descendiendo a recibir.",
                type = ObjectiveType.TACTICAL,
                durationMinutes = 20
            ),
            ExerciseModel(
                id = "lib_5",
                name = "Circuito de potencia aeróbica y coordinativa",
                description = "Saltos de valla, estacas en zigzag y esprint de 20 metros con aceleración progresiva.",
                type = ObjectiveType.PHYSICAL,
                durationMinutes = 15
            ),
            ExerciseModel(
                id = "lib_6",
                name = "Partidillo reducido 3v3 con comodines exteriores",
                description = "Intensidad máxima en espacio de 25x20m para favorecer toma de decisiones y desmarques.",
                type = ObjectiveType.OTHER,
                durationMinutes = 20
            )
        )
    }

    /**
     * Obtiene los equipos en estado APROBADO asignados al Director Técnico (DT).
     */
    suspend fun getApprovedTeamsByDt(dtUid: String): Result<List<TeamModel>> {
        return try {
            val snapshot = teams
                .whereEqualTo("dtUid", dtUid)
                .whereEqualTo("status", TeamStatus.APPROVED)
                .get()
                .await()
            val list = snapshot.toObjects(TeamModel::class.java).sortedBy { it.name }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crea una nueva sesión de entrenamiento en Firestore y envía notificaciones automáticas.
     */
    suspend fun createTraining(training: TrainingModel): Result<String> {
        return try {
            val docRef = trainings.document()
            val toSave = training.copy(
                id = docRef.id,
                status = TrainingStatus.PROGRAMMED,
                cancelReason = ""
            )
            docRef.set(toSave).await()

            // Disparar notificaciones a jugadores y padres vinculados al equipo
            dispatchTrainingNotifications(
                teamId = training.teamId,
                title = "Nuevo Entrenamiento Programado",
                message = "Se programó entrenamiento para el ${training.date} a las ${training.startTime} hs en ${training.location}.",
                trainingId = docRef.id
            )

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza una sesión de entrenamiento existente.
     */
    suspend fun updateTraining(training: TrainingModel): Result<Unit> {
        return try {
            trainings.document(training.id).set(training).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cancela una sesión de entrenamiento futura y notifica a los jugadores y padres.
     */
    suspend fun cancelTraining(trainingId: String, reason: String, dtUid: String): Result<Unit> {
        return try {
            val doc = trainings.document(trainingId).get().await()
            val training = doc.toObject(TrainingModel::class.java) ?: throw Exception("Entrenamiento no encontrado")

            if (training.dtUid != dtUid) throw Exception("No tienes permiso para cancelar esta sesión")

            trainings.document(trainingId).update(
                mapOf(
                    "status" to TrainingStatus.CANCELED,
                    "cancelReason" to reason.trim()
                )
            ).await()

            dispatchTrainingNotifications(
                teamId = training.teamId,
                title = "Entrenamiento Cancelado",
                message = "El entrenamiento del ${training.date} fue cancelado. Motivo: ${reason.trim()}",
                trainingId = trainingId
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene los detalles de una sesión específica.
     */
    suspend fun getTraining(trainingId: String): Result<TrainingModel> {
        return try {
            val doc = trainings.document(trainingId).get().await()
            val model = doc.toObject(TrainingModel::class.java) ?: throw Exception("Sesión no encontrada")
            Result.success(model)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observa en tiempo real las sesiones de entrenamiento creadas por el DT.
     */
    fun observeTrainingsByDt(dtUid: String): Flow<List<TrainingModel>> =
        observe(trainings.whereEqualTo("dtUid", dtUid))

    /**
     * Observa en tiempo real las sesiones de entrenamiento de un equipo específico.
     */
    fun observeTrainingsByTeam(teamId: String): Flow<List<TrainingModel>> =
        observe(trainings.whereEqualTo("teamId", teamId))

    /**
     * Observa las sesiones según el rol del usuario (DT, JUG o PAD).
     */
    fun observeTrainingsForUser(user: UserModel): Flow<List<TrainingModel>> = callbackFlow {
        if (user.roleEnum == Role.DT) {
            val registration = trainings.whereEqualTo("dtUid", user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val list = snapshot?.toObjects(TrainingModel::class.java).orEmpty()
                    trySend(list)
                }
            awaitClose { registration.remove() }
        } else if (user.roleEnum == Role.PAD) {
            val playerQuery = players.whereEqualTo("parentUid", user.uid)
            val registration = playerQuery.addSnapshotListener { pSnapshot, pError ->
                if (pError != null) {
                    close(pError)
                    return@addSnapshotListener
                }
                val teamIds = pSnapshot?.toObjects(PlayerModel::class.java).orEmpty()
                    .map { it.teamId }
                    .filter { it.isNotBlank() }
                    .distinct()

                if (teamIds.isEmpty()) {
                    trySend(emptyList())
                } else {
                    trainings.whereIn("teamId", teamIds.take(10))
                        .addSnapshotListener { tSnapshot, tError ->
                            if (tError != null) {
                                close(tError)
                                return@addSnapshotListener
                            }
                            val list = tSnapshot?.toObjects(TrainingModel::class.java).orEmpty()
                            trySend(list)
                        }
                }
            }
            awaitClose { registration.remove() }
        } else {
            // JUG
            val playerQuery = players.whereEqualTo("userUid", user.uid)
            val registration = playerQuery.addSnapshotListener { pSnapshot, pError ->
                if (pError != null) {
                    close(pError)
                    return@addSnapshotListener
                }
                val teamIds = pSnapshot?.toObjects(PlayerModel::class.java).orEmpty()
                    .map { it.teamId }
                    .filter { it.isNotBlank() }
                    .distinct()

                if (teamIds.isEmpty()) {
                    trySend(emptyList())
                } else {
                    trainings.whereIn("teamId", teamIds.take(10))
                        .addSnapshotListener { tSnapshot, tError ->
                            if (tError != null) {
                                close(tError)
                                return@addSnapshotListener
                            }
                            val list = tSnapshot?.toObjects(TrainingModel::class.java).orEmpty()
                            trySend(list)
                        }
                }
            }
            awaitClose { registration.remove() }
        }
    }

    private fun observe(query: Query): Flow<List<TrainingModel>> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(TrainingModel::class.java).orEmpty()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    /**
     * Genera y guarda notificaciones en Firestore para todos los destinatarios del equipo.
     */
    private suspend fun dispatchTrainingNotifications(
        teamId: String,
        title: String,
        message: String,
        trainingId: String
    ) {
        try {
            val playerDocs = players.whereEqualTo("teamId", teamId).get().await()
            val playerList = playerDocs.toObjects(PlayerModel::class.java)

            val recipientUids = mutableSetOf<String>()
            playerList.forEach { p ->
                if (p.userUid.isNotBlank()) recipientUids.add(p.userUid)
                if (p.parentUid.isNotBlank()) recipientUids.add(p.parentUid)
            }

            recipientUids.forEach { uid ->
                val notif = hashMapOf(
                    "recipientUid" to uid,
                    "teamId" to teamId,
                    "title" to title,
                    "message" to message,
                    "type" to "TRAINING",
                    "relatedId" to trainingId,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                notifications.add(notif)
            }
        } catch (_: Exception) {
            // Ignorar fallos secundarios en notificaciones
        }
    }
}
