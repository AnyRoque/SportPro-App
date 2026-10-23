package pe.edu.esan.sportpro.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.AttendanceRecordModel
import pe.edu.esan.sportpro.data.model.PlayerAttendanceRecord
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.model.TrainingStatus
import pe.edu.esan.sportpro.util.Validators
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Repositorio de Cloud Firestore para el Registro de Asistencia a Entrenamientos (US-13, US-14).
 */
object AttendanceRepository {

    private const val ATTENDANCE = "attendance"
    private const val TRAININGS = "trainings"
    private const val PLAYERS = "players"

    private val firestore = FirebaseFirestore.getInstance()
    private val attendanceRef get() = firestore.collection(ATTENDANCE)
    private val trainingsRef get() = firestore.collection(TRAININGS)
    private val playersRef get() = firestore.collection(PLAYERS)

    /**
     * Guarda o actualiza el registro de asistencia en Firestore y marca la sesión como "REALIZADA".
     */
    suspend fun saveAttendanceRecord(record: AttendanceRecordModel): Result<Unit> {
        return try {
            val docId = record.trainingId.ifBlank { record.id }
            val recordToSave = record.copy(
                id = docId,
                trainingId = docId,
                editedAt = null
            )

            // 1. Guardar en colección 'attendance'
            attendanceRef.document(docId).set(recordToSave).await()

            // 2. Cambiar estado de la sesión de entrenamiento a 'REALIZADA'
            trainingsRef.document(docId).update(
                mapOf(
                    "status" to TrainingStatus.COMPLETED,
                    "completedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // 3. Registrar el historial en la subcolección de cada jugador
            record.items.forEach { item ->
                if (item.playerId.isNotBlank()) {
                    try {
                        val playerHistoryRef = playersRef
                            .document(item.playerId)
                            .collection("attendanceHistory")
                            .document(docId)

                        val historyData = hashMapOf(
                            "trainingId" to docId,
                            "teamId" to record.teamId,
                            "teamName" to record.teamName,
                            "date" to record.date,
                            "status" to item.status,
                            "justificationReason" to item.justificationReason,
                            "recordedAt" to FieldValue.serverTimestamp()
                        )
                        playerHistoryRef.set(historyData).await()
                    } catch (_: Exception) {}
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene el registro de asistencia de una sesión específica.
     */
    suspend fun getAttendanceRecord(trainingId: String): Result<AttendanceRecordModel> {
        return try {
            val doc = attendanceRef.document(trainingId).get().await()
            val model = doc.toObject(AttendanceRecordModel::class.java)
                ?: throw Exception("No se encontró registro de asistencia para esta sesión")
            Result.success(model)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene el último registro de asistencia de un equipo para la función "Copiar asistencia previa".
     */
    suspend fun getLastAttendanceRecordForTeam(teamId: String): Result<AttendanceRecordModel?> {
        return try {
            val snapshot = attendanceRef
                .whereEqualTo("teamId", teamId)
                .get()
                .await()

            val lastRecord = snapshot.toObjects(AttendanceRecordModel::class.java)
                .sortedByDescending { it.date }
                .firstOrNull()

            Result.success(lastRecord)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene los jugadores activos vinculados a un equipo específico.
     */
    suspend fun getActivePlayersForTeam(teamId: String, dtUid: String): Result<List<PlayerModel>> {
        return try {
            val snapshot = playersRef
                .whereEqualTo("teamId", teamId)
                .whereEqualTo("active", true)
                .get()
                .await()

            val players = snapshot.toObjects(PlayerModel::class.java)
                .sortedBy { it.lastName }

            Result.success(players)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene la cuenta de jugador vinculada al UID de un usuario Jugador (JUG).
     */
    suspend fun getPlayerByUserUid(userUid: String): Result<PlayerModel> {
        return try {
            val snapshot = playersRef
                .whereEqualTo("userUid", userUid)
                .get()
                .await()

            val player = snapshot.toObjects(PlayerModel::class.java).firstOrNull()
                ?: throw Exception("No se encontró perfil de jugador vinculado a esta cuenta.")

            Result.success(player)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene los hijos (jugadores) vinculados al UID de un Padre de familia (PAD).
     */
    suspend fun getChildrenByParentUid(parentUid: String): Result<List<PlayerModel>> {
        return try {
            val snapshot = playersRef
                .whereEqualTo("parentUid", parentUid)
                .get()
                .await()

            val children = snapshot.toObjects(PlayerModel::class.java).sortedBy { it.name }
            Result.success(children)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observa en tiempo real el historial personal de asistencia de un jugador (US-14).
     */
    fun observePlayerAttendanceHistory(playerId: String): Flow<List<PlayerAttendanceRecord>> = callbackFlow {
        if (playerId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val registration = playersRef
            .document(playerId)
            .collection("attendanceHistory")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(PlayerAttendanceRecord::class.java).orEmpty()
                trySend(list.sortedByDescending { it.date })
            }

        awaitClose { registration.remove() }
    }

    /**
     * Observa el historial de asistencias de un equipo en tiempo real.
     */
    fun observeAttendanceHistoryByTeam(teamId: String): Flow<List<AttendanceRecordModel>> = callbackFlow {
        val registration = attendanceRef
            .whereEqualTo("teamId", teamId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(AttendanceRecordModel::class.java).orEmpty()
                trySend(list.sortedByDescending { it.date })
            }
        awaitClose { registration.remove() }
    }

    /**
     * Verifica si el registro de asistencia aún es editable (dentro del límite de 48 horas).
     */
    fun canEditAttendance(dateString: String): Boolean {
        return try {
            val recordDate = Validators.parseDate(dateString) ?: return false
            val today = LocalDate.now()
            val daysBetween = ChronoUnit.DAYS.between(recordDate, today)
            daysBetween in 0..2
        } catch (_: Exception) {
            false
        }
    }
}
