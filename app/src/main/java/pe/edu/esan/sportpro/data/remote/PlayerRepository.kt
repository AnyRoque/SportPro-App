package pe.edu.esan.sportpro.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.LinkStatus
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.UserModel

/** Jugadores: perfil gestionado por el DT (US-04) y registro de hijos por el PAD (US-02). */
object PlayerRepository {

    private const val PLAYERS = "players"
    /** dniIndex/{dni} -> { playerId }  garantiza DNI único sin exponer datos de otros jugadores */
    private const val DNI_INDEX = "dniIndex"

    private val firestore = FirebaseFirestore.getInstance()
    private val players get() = firestore.collection(PLAYERS)

    /** Crea un jugador. Si tiene DNI, lo reserva en dniIndex dentro de la misma transacción. */
    suspend fun createPlayer(player: PlayerModel): Result<String> {
        return try {
            var playerToSave = player
            if (player.email.isNotBlank() && player.userUid.isBlank() && player.parentUid.isBlank()) {
                val userMatch = findRegisteredUserByEmail(player.email).getOrNull()
                if (userMatch != null) {
                    playerToSave = if (userMatch.role == "JUG") {
                        player.copy(userUid = userMatch.uid, linkStatus = LinkStatus.ACCEPTED)
                    } else if (userMatch.role == "PAD") {
                        player.copy(parentUid = userMatch.uid, linkStatus = LinkStatus.ACCEPTED)
                    } else player
                }
            }

            val playerRef = players.document()
            firestore.runTransaction { tx ->
                if (playerToSave.dni.isNotBlank()) {
                    val dniRef = firestore.collection(DNI_INDEX).document(playerToSave.dni)
                    if (tx.get(dniRef).exists()) throw Exception("El DNI ya está registrado en el sistema")
                    tx.set(dniRef, mapOf("playerId" to playerRef.id))
                }
                tx.set(playerRef, playerToSave)
                playerRef.id
            }.await()
            Result.success(playerRef.id)
        } catch (e: Exception) {
            // Las excepciones dentro de la transacción llegan envueltas
            Result.failure(Exception(e.cause?.message ?: e.message))
        }
    }

    /** Actualiza un perfil existente (el DNI no se modifica). */
    suspend fun updatePlayer(player: PlayerModel): Result<Unit> {
        return try {
            players.document(player.id).set(player).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlayer(playerId: String): Result<PlayerModel> {
        return try {
            val doc = players.document(playerId).get().await()
            Result.success(doc.toObject(PlayerModel::class.java) ?: throw Exception("Jugador no encontrado"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Plantel del equipo. Se filtra también por dtUid para que la consulta cumpla
     * las reglas de seguridad (solo el DT del equipo puede leer sus jugadores).
     */
    fun observePlayersByTeam(teamId: String, dtUid: String): Flow<List<PlayerModel>> =
        observe(players.whereEqualTo("teamId", teamId).whereEqualTo("dtUid", dtUid))

    fun observeChildrenByParent(parentUid: String): Flow<List<PlayerModel>> =
        observe(players.whereEqualTo("parentUid", parentUid))

    fun observePlayersByUser(userUid: String): Flow<List<PlayerModel>> =
        observe(players.whereEqualTo("userUid", userUid))

    private fun observe(query: Query): Flow<List<PlayerModel>> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObjects(PlayerModel::class.java).orEmpty().sortedBy { it.lastName })
        }
        awaitClose { registration.remove() }
    }

    suspend fun acceptLink(playerId: String): Result<Unit> {
        val res = updateField(playerId, "linkStatus", LinkStatus.ACCEPTED)
        if (res.isSuccess) {
            try {
                val p = getPlayer(playerId).getOrNull()
                if (p != null) {
                    val recipient = p.userUid.ifBlank { p.parentUid }
                    if (recipient.isNotBlank()) {
                        val notif = hashMapOf(
                            "recipientUid" to recipient,
                            "teamId" to p.teamId,
                            "title" to "Solicitud Aceptada",
                            "message" to "¡Tu solicitud para unirte al equipo ${p.teamName} ha sido ACEPTADA!",
                            "type" to "TEAM_ACCEPT",
                            "relatedId" to p.teamId,
                            "read" to false,
                            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                        firestore.collection("notifications").add(notif)
                    }
                }
            } catch (_: Exception) {}
        }
        return res
    }

    suspend fun rejectLink(playerId: String): Result<Unit> {
        val res = updateField(playerId, "linkStatus", LinkStatus.REJECTED)
        if (res.isSuccess) {
            try {
                val p = getPlayer(playerId).getOrNull()
                if (p != null) {
                    val recipient = p.userUid.ifBlank { p.parentUid }
                    if (recipient.isNotBlank()) {
                        val notif = hashMapOf(
                            "recipientUid" to recipient,
                            "teamId" to p.teamId,
                            "title" to "Solicitud Rechazada",
                            "message" to "Tu solicitud para unirte al equipo ${p.teamName} ha sido rechazada.",
                            "type" to "TEAM_REJECT",
                            "relatedId" to p.teamId,
                            "read" to false,
                            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                        firestore.collection("notifications").add(notif)
                    }
                }
            } catch (_: Exception) {}
        }
        return res
    }

    /** Dar de baja / reactivar sin borrar historial. */
    suspend fun setActive(playerId: String, active: Boolean): Result<Unit> = updateField(playerId, "active", active)

    private suspend fun updateField(playerId: String, field: String, value: Any): Result<Unit> {
        return try {
            players.document(playerId).update(field, value).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findRegisteredUserByEmail(email: String): Result<UserModel?> {
        return try {
            val cleanEmail = email.trim().lowercase()
            if (cleanEmail.isBlank()) return Result.success(null)
            val snapshot = firestore.collection("users")
                .whereEqualTo("email", cleanEmail)
                .get()
                .await()
            val user = snapshot.toObjects(UserModel::class.java).firstOrNull()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addRegisteredUserToTeam(user: UserModel, teamId: String, teamName: String, dtUid: String): Result<String> {
        return try {
            val isJug = user.roleEnum == Role.JUG
            val isPad = user.roleEnum == Role.PAD

            val newPlayer = PlayerModel(
                teamId = teamId,
                teamName = teamName,
                dtUid = dtUid,
                name = user.name,
                lastName = user.lastName,
                email = user.email,
                phone = user.phone,
                userUid = if (isJug) user.uid else "",
                parentUid = if (isPad) user.uid else "",
                linkStatus = LinkStatus.ACCEPTED,
                active = true,
                createdBy = dtUid
            )
            createPlayer(newPlayer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestToJoinTeam(user: UserModel, team: TeamModel, position: String = ""): Result<String> {
        return try {
            val isJug = user.roleEnum == Role.JUG
            val isPad = user.roleEnum == Role.PAD

            val requestPlayer = PlayerModel(
                teamId = team.id,
                teamName = team.name,
                dtUid = team.dtUid,
                category = team.category,
                name = user.name,
                lastName = user.lastName,
                email = user.email,
                phone = user.phone,
                position = position,
                userUid = if (isJug) user.uid else "",
                parentUid = if (isPad) user.uid else "",
                linkStatus = LinkStatus.PENDING,
                active = true,
                createdBy = user.uid
            )

            val docIdResult = createPlayer(requestPlayer)

            val notif = hashMapOf(
                "recipientUid" to team.dtUid,
                "teamId" to team.id,
                "title" to "Nueva Solicitud de Ingreso",
                "message" to "${user.fullName} ha solicitado unirse a tu equipo ${team.name}.",
                "type" to "TEAM_REQUEST",
                "relatedId" to team.id,
                "read" to false,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            firestore.collection("notifications").add(notif)

            docIdResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
