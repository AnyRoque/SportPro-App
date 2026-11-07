package pe.edu.esan.sportpro.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.CommentModel
import pe.edu.esan.sportpro.data.model.ModerationAction
import pe.edu.esan.sportpro.data.model.PostModel
import pe.edu.esan.sportpro.data.model.PostStatus
import pe.edu.esan.sportpro.data.model.ReportModel
import pe.edu.esan.sportpro.data.model.ReportStatus
import pe.edu.esan.sportpro.util.Validators

/** US-23 (comunidad) y US-24 (reportes y moderación). */
object CommunityRepository {

    private const val POSTS = "posts"
    private const val COMMENTS = "comments"
    private const val REPORTS = "reports"
    private const val NOTIFICATIONS = "notifications"
    /** Con este número de reportes de usuarios distintos el contenido se oculta hasta revisión */
    const val AUTO_HIDE_REPORTS = 3

    private val firestore = FirebaseFirestore.getInstance()
    private val posts get() = firestore.collection(POSTS)
    private val reports get() = firestore.collection(REPORTS)

    /** Lista básica de palabras bloqueadas (US-23). Se puede ampliar. */
    private val blockedWords = listOf("idiota", "estupido", "imbecil", "tarado", "mierda", "puta", "huevon", "cojudo")

    fun containsOffensiveWords(text: String): Boolean {
        val normalized = Validators.normalize(text)
        return blockedWords.any { Regex("\\b$it\\b").containsMatchIn(normalized) }
    }

    // ---------------- Publicaciones ----------------

    fun observeFeed(): Flow<List<PostModel>> = callbackFlow {
        val reg = posts.orderBy("createdAt", Query.Direction.DESCENDING).limit(100)
            .addSnapshotListener { snap, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snap?.toObjects(PostModel::class.java).orEmpty())
            }
        awaitClose { reg.remove() }
    }

    fun observePost(postId: String): Flow<PostModel?> = callbackFlow {
        val reg = posts.document(postId).addSnapshotListener { snap, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            trySend(snap?.toObject(PostModel::class.java))
        }
        awaitClose { reg.remove() }
    }

    suspend fun createPost(post: PostModel): Result<Unit> = try {
        if (containsOffensiveWords(post.text)) throw Exception("La publicación contiene palabras no permitidas")
        posts.add(post).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Eliminar propia publicación: se marca como eliminada (no se borra físicamente). */
    suspend fun deletePost(postId: String): Result<Unit> = try {
        posts.document(postId).update("status", PostStatus.DELETED).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Una reacción por usuario: si repite la misma se quita, si elige otra se cambia. */
    suspend fun toggleReaction(post: PostModel, uid: String, reaction: String): Result<Unit> = try {
        val current = post.reactions[uid]
        val value: Any = if (current == reaction) FieldValue.delete() else reaction
        posts.document(post.id).update("reactions.$uid", value).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ---------------- Comentarios ----------------

    fun observeComments(postId: String): Flow<List<CommentModel>> = callbackFlow {
        val reg = posts.document(postId).collection(COMMENTS)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snap?.toObjects(CommentModel::class.java).orEmpty())
            }
        awaitClose { reg.remove() }
    }

    suspend fun addComment(comment: CommentModel): Result<Unit> = try {
        if (containsOffensiveWords(comment.text)) throw Exception("El comentario contiene palabras no permitidas")
        val postRef = posts.document(comment.postId)
        val commentRef = postRef.collection(COMMENTS).document()
        firestore.runBatch { b ->
            b.set(commentRef, comment)
            b.update(postRef, "commentCount", FieldValue.increment(1))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> = try {
        val postRef = posts.document(postId)
        firestore.runBatch { b ->
            b.delete(postRef.collection(COMMENTS).document(commentId))
            b.update(postRef, "commentCount", FieldValue.increment(-1))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ---------------- Reportes (US-24) ----------------

    /**
     * El ID del reporte es postId_reporterUid: así un usuario no puede reportar dos veces lo mismo.
     * Al llegar a 3 reportes el contenido se oculta temporalmente (no se elimina).
     */
    suspend fun reportPost(post: PostModel, reporterUid: String, reason: String, detail: String): Result<Unit> = try {
        val reportRef = reports.document("${post.id}_$reporterUid")
        if (reportRef.get().await().exists()) throw Exception("Ya reportaste esta publicación")
        val report = ReportModel(
            postId = post.id, postText = post.text.take(300),
            postAuthorUid = post.authorUid, postAuthorName = post.authorName,
            reason = reason, detail = detail.trim(), reporterUid = reporterUid
        )
        val postRef = posts.document(post.id)
        firestore.runBatch { b ->
            b.set(reportRef, report)
            b.update(postRef, "reportCount", FieldValue.increment(1))
            if (post.reportCount + 1 >= AUTO_HIDE_REPORTS && post.status == PostStatus.VISIBLE) {
                b.update(postRef, "status", PostStatus.HIDDEN)
            }
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** ADM: reportes pendientes, ordenados por cantidad de reportes de cada publicación. */
    fun observePendingReports(): Flow<List<ReportModel>> = callbackFlow {
        val reg = reports.whereEqualTo("status", ReportStatus.PENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snap?.toObjects(ReportModel::class.java).orEmpty())
            }
        awaitClose { reg.remove() }
    }

    /**
     * ADM resuelve todos los reportes de una publicación: Mantener, Ocultar o Eliminar.
     * Se registra quién y cuándo, y se notifica al autor y a los reportantes.
     */
    suspend fun moderate(postId: String, action: String, adminUid: String): Result<Unit> = try {
        val pending = reports.whereEqualTo("postId", postId)
            .whereEqualTo("status", ReportStatus.PENDING).get().await()
            .toObjects(ReportModel::class.java)
        val postRef = posts.document(postId)
        val newStatus = when (action) {
            ModerationAction.KEEP -> PostStatus.VISIBLE
            ModerationAction.HIDE -> PostStatus.HIDDEN
            else -> PostStatus.DELETED
        }
        firestore.runBatch { b ->
            b.update(postRef, "status", newStatus)
            pending.forEach { r ->
                b.update(
                    reports.document(r.id), mapOf(
                        "status" to ReportStatus.RESOLVED,
                        "action" to action,
                        "resolvedBy" to adminUid,
                        "resolvedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }.await()
        val actionLabel = when (action) {
            ModerationAction.KEEP -> "se mantuvo"
            ModerationAction.HIDE -> "fue ocultada"
            else -> "fue eliminada"
        }
        pending.map { it.reporterUid }.distinct().forEach {
            notify(it, "Tu reporte fue revisado", "La publicación reportada $actionLabel.", postId)
        }
        pending.firstOrNull()?.let { r ->
            if (action != ModerationAction.KEEP) {
                notify(r.postAuthorUid, "Tu publicación $actionLabel", "Un administrador revisó reportes sobre tu publicación.", postId)
            }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun notify(uid: String, title: String, message: String, postId: String) {
        if (uid.isBlank()) return
        firestore.collection(NOTIFICATIONS).add(
            hashMapOf(
                "recipientUid" to uid, "title" to title, "message" to message,
                "type" to "COMMUNITY", "relatedId" to postId, "read" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
    }
}
