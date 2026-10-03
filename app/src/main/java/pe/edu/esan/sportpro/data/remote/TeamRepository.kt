package pe.edu.esan.sportpro.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.TeamStatus
import pe.edu.esan.sportpro.util.Validators

/** Equipos/academias: registro por DT (US-03) y validación por ADM (US-07). */
object TeamRepository {

    private const val TEAMS = "teams"
    private val firestore = FirebaseFirestore.getInstance()
    private val teams get() = firestore.collection(TEAMS)

    private fun nameKey(name: String, category: String) = "${Validators.normalize(name)}_$category"

    private suspend fun existsDuplicate(name: String, category: String, excludeId: String?): Boolean {
        val snapshot = teams.whereEqualTo("nameKey", nameKey(name, category)).get().await()
        return snapshot.documents.any { it.id != excludeId }
    }

    /** Crea el equipo en estado PENDIENTE. El DT que lo registra queda asignado. */
    suspend fun createTeam(team: TeamModel): Result<Unit> {
        return try {
            if (existsDuplicate(team.name, team.category, null))
                throw Exception("Ya existe un equipo con ese nombre y categoría")
            val toSave = team.copy(
                name = team.name.trim(),
                nameKey = nameKey(team.name, team.category),
                status = TeamStatus.PENDING,
                rejectReason = ""
            )
            teams.add(toSave).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Corregir y reenviar un equipo rechazado: vuelve a PENDIENTE. */
    suspend fun updateAndResubmit(team: TeamModel): Result<Unit> {
        return try {
            if (existsDuplicate(team.name, team.category, team.id))
                throw Exception("Ya existe un equipo con ese nombre y categoría")
            teams.document(team.id).update(
                mapOf(
                    "name" to team.name.trim(),
                    "nameKey" to nameKey(team.name, team.category),
                    "category" to team.category,
                    "field" to team.field,
                    "district" to team.district,
                    "trainingDays" to team.trainingDays,
                    "trainingTime" to team.trainingTime,
                    "status" to TeamStatus.PENDING,
                    "rejectReason" to ""
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTeam(teamId: String): Result<TeamModel> {
        return try {
            val doc = teams.document(teamId).get().await()
            Result.success(doc.toObject(TeamModel::class.java) ?: throw Exception("Equipo no encontrado"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApprovedTeams(): Result<List<TeamModel>> {
        return try {
            val snapshot = teams.whereEqualTo("status", TeamStatus.APPROVED).get().await()
            Result.success(snapshot.toObjects(TeamModel::class.java).sortedBy { it.name })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Escucha en tiempo real los equipos del DT. */
    fun observeTeamsByDt(dtUid: String): Flow<List<TeamModel>> =
        observe(teams.whereEqualTo("dtUid", dtUid))

    /** Escucha en tiempo real los equipos por estado (panel del ADM). */
    fun observeTeamsByStatus(status: String): Flow<List<TeamModel>> =
        observe(teams.whereEqualTo("status", status))

    private fun observe(query: Query): Flow<List<TeamModel>> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(TeamModel::class.java).orEmpty()
            trySend(list.sortedByDescending { it.createdAt })
        }
        awaitClose { registration.remove() }
    }

    suspend fun approveTeam(teamId: String, adminUid: String): Result<Unit> =
        review(teamId, adminUid, TeamStatus.APPROVED, "")

    suspend fun rejectTeam(teamId: String, adminUid: String, reason: String): Result<Unit> =
        review(teamId, adminUid, TeamStatus.REJECTED, reason.trim())

    private suspend fun review(teamId: String, adminUid: String, status: String, reason: String): Result<Unit> {
        return try {
            teams.document(teamId).update(
                mapOf(
                    "status" to status,
                    "rejectReason" to reason,
                    "reviewedBy" to adminUid,
                    "reviewedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
