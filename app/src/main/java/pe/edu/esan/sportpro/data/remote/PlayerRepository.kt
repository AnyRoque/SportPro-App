package pe.edu.esan.sportpro.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.LinkStatus
import pe.edu.esan.sportpro.data.model.PlayerModel

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
            val playerRef = players.document()
            firestore.runTransaction { tx ->
                if (player.dni.isNotBlank()) {
                    val dniRef = firestore.collection(DNI_INDEX).document(player.dni)
                    if (tx.get(dniRef).exists()) throw Exception("El DNI ya está registrado en el sistema")
                    tx.set(dniRef, mapOf("playerId" to playerRef.id))
                }
                tx.set(playerRef, player)
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

    suspend fun acceptLink(playerId: String): Result<Unit> = updateField(playerId, "linkStatus", LinkStatus.ACCEPTED)

    suspend fun rejectLink(playerId: String): Result<Unit> = updateField(playerId, "linkStatus", LinkStatus.REJECTED)

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
}
