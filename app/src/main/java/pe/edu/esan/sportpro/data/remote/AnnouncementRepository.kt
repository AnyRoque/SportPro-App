package pe.edu.esan.sportpro.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.AnnouncementModel
import pe.edu.esan.sportpro.data.model.LinkStatus
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.TeamStatus
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.util.Validators
import java.time.LocalDate

/** Equipos a los que pertenece un usuario (sirve para anuncios y comunidad). */
object UserTeamsRepository {

    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getTeamsForUser(user: UserModel): List<TeamModel> = try {
        when (user.roleEnum) {
            Role.ADM -> firestore.collection("teams").whereEqualTo("status", TeamStatus.APPROVED)
                .get().await().toObjects(TeamModel::class.java)
            Role.DT -> firestore.collection("teams").whereEqualTo("dtUid", user.uid)
                .get().await().toObjects(TeamModel::class.java)
                .filter { it.status == TeamStatus.APPROVED }
            Role.JUG, Role.PAD -> {
                val field = if (user.roleEnum == Role.JUG) "userUid" else "parentUid"
                val players = firestore.collection("players").whereEqualTo(field, user.uid)
                    .get().await().toObjects(PlayerModel::class.java)
                    .filter { it.linkStatus == LinkStatus.ACCEPTED && it.active }
                players.distinctBy { it.teamId }.map { TeamModel(id = it.teamId, name = it.teamName, category = it.category) }
            }
            null -> emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }
}

/** US-22: anuncios internos publicados por ADM o DT. */
object AnnouncementRepository {

    private const val ANNOUNCEMENTS = "announcements"
    private const val NOTIFICATIONS = "notifications"
    private val firestore = FirebaseFirestore.getInstance()
    private val announcements get() = firestore.collection(ANNOUNCEMENTS)

    /** Anuncios de toda la academia (teamId vacío) y de los equipos del usuario, en tiempo real. */
    fun observeAll(): Flow<List<AnnouncementModel>> = callbackFlow {
        val reg = announcements.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snap?.toObjects(AnnouncementModel::class.java).orEmpty())
            }
        awaitClose { reg.remove() }
    }

    /** Filtro de destinatarios: ADM ve todo; los demás solo su equipo/academia y su rol. */
    fun isForUser(a: AnnouncementModel, user: UserModel, teamIds: Set<String>): Boolean {
        if (user.roleEnum == Role.ADM) return true
        if (a.authorUid == user.uid) return true
        val teamOk = a.teamId.isBlank() || a.teamId in teamIds
        return teamOk && user.role in a.targetRoles
    }

    /** Vigente si la fecha de vigencia es hoy o posterior. */
    fun isActive(a: AnnouncementModel, today: LocalDate = LocalDate.now()): Boolean {
        val until = Validators.parseDate(a.validUntil) ?: return true
        return !until.isBefore(today)
    }

    suspend fun get(id: String): AnnouncementModel? = try {
        announcements.document(id).get().await().toObject(AnnouncementModel::class.java)
    } catch (e: Exception) {
        null
    }

    suspend fun save(a: AnnouncementModel, recipientUids: List<String>): Result<Unit> = try {
        if (a.id.isBlank()) {
            val ref = announcements.add(a).await()
            notify(recipientUids, a, ref.id)
        } else {
            announcements.document(a.id).set(a.copy(edited = true)).await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun delete(id: String): Result<Unit> = try {
        announcements.document(id).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun markRead(id: String, uid: String) {
        try {
            announcements.document(id).update("readBy", FieldValue.arrayUnion(uid)).await()
        } catch (_: Exception) {}
    }

    /** UIDs de los miembros del equipo (DT, jugadores con cuenta y padres) filtrados por rol. */
    suspend fun recipientsFor(teamId: String, roles: List<String>): List<String> = try {
        val set = mutableSetOf<String>()
        if (teamId.isBlank()) {
            firestore.collection("users").get().await().toObjects(UserModel::class.java)
                .filter { it.role in roles && it.active }.forEach { set.add(it.uid) }
        } else {
            val team = firestore.collection("teams").document(teamId).get().await().toObject(TeamModel::class.java)
            if (team != null && "DT" in roles) set.add(team.dtUid)
            firestore.collection("players").whereEqualTo("teamId", teamId).get().await()
                .toObjects(PlayerModel::class.java)
                .filter { it.linkStatus == LinkStatus.ACCEPTED }
                .forEach { p ->
                    if ("JUG" in roles && p.userUid.isNotBlank()) set.add(p.userUid)
                    if ("PAD" in roles && p.parentUid.isNotBlank()) set.add(p.parentUid)
                }
        }
        set.toList()
    } catch (e: Exception) {
        emptyList()
    }

    /** Notificación interna (misma colección "notifications" que usa el módulo de partidos). */
    private fun notify(uids: List<String>, a: AnnouncementModel, id: String) {
        uids.forEach { uid ->
            firestore.collection(NOTIFICATIONS).add(
                hashMapOf(
                    "recipientUid" to uid,
                    "teamId" to a.teamId,
                    "title" to "Nuevo anuncio: ${a.title}",
                    "message" to a.description.take(120),
                    "type" to "ANNOUNCEMENT",
                    "relatedId" to id,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }
    }
}
