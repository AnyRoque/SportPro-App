package pe.edu.esan.sportpro.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.EventStatus
import pe.edu.esan.sportpro.data.model.EventTeam
import pe.edu.esan.sportpro.data.model.EventType
import pe.edu.esan.sportpro.data.model.MatchEventModel
import pe.edu.esan.sportpro.data.model.MatchModel
import pe.edu.esan.sportpro.data.model.MatchStatus
import pe.edu.esan.sportpro.data.model.MatchSummaryModel
import pe.edu.esan.sportpro.data.model.SummaryStatus
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.ai.GeminiClient
import pe.edu.esan.sportpro.data.remote.ai.GeminiContent
import pe.edu.esan.sportpro.data.remote.ai.GeminiPart
import pe.edu.esan.sportpro.data.remote.ai.GeminiRequest
import pe.edu.esan.sportpro.util.Validators
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * US-25: genera el resumen narrativo con IA usando SOLO los eventos registrados.
 * US-26: revisión, edición y aprobación del DT.
 * US-27: consulta del resumen publicado.
 */
object SummaryRepository {

    private const val SUMMARIES = "summaries"
    private const val NOTIFICATIONS = "notifications"
    const val MAX_GENERATIONS = 3
    const val PROMPT_VERSION = "v1"

    private val firestore = FirebaseFirestore.getInstance()
    private val summaries get() = firestore.collection(SUMMARIES)

    private val PROMPT = """
        Eres un redactor deportivo. Escribe en español el resumen de un partido de fútbol formativo
        usando EXCLUSIVAMENTE los datos del JSON que te paso.
        Reglas:
        - No inventes jugadores, jugadas, minutos ni resultados que no estén en el JSON.
        - Si un evento no tiene jugador, no le asignes uno.
        - Respeta el marcador exacto del JSON.
        - Separa "hechos" (lo que dicen los datos, en orden cronológico) de "analisis" (interpretación breve y prudente, máximo 3 oraciones).
        - Tono positivo y respetuoso: son menores de edad. No critiques a jugadores individuales.
        - Responde SOLO con JSON válido: {"hechos": "...", "analisis": "...", "jugadores_mencionados": ["..."]}
    """.trimIndent()

    fun observeSummary(matchId: String): Flow<MatchSummaryModel?> = callbackFlow {
        val reg = summaries.document(matchId).addSnapshotListener { snap, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            trySend(snap?.toObject(MatchSummaryModel::class.java))
        }
        awaitClose { reg.remove() }
    }

    /** US-27: resúmenes publicados de los partidos del usuario. */
    fun observePublishedForUser(uid: String): Flow<List<MatchSummaryModel>> = callbackFlow {
        val reg = summaries.whereArrayContains("memberUids", uid)
            .addSnapshotListener { snap, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snap?.toObjects(MatchSummaryModel::class.java).orEmpty()
                    .filter { it.status == SummaryStatus.PUBLISHED }
                    .sortedByDescending { Validators.parseDate(it.date) }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** Eventos válidos para la IA: solo los ACTIVOS (los anulados quedan fuera). */
    fun activeEvents(events: List<MatchEventModel>) =
        events.filter { it.status == EventStatus.ACTIVE }.sortedBy { it.minute }

    /** Mínimo: partido finalizado con inicio, fin y al menos un evento adicional. */
    fun hasEnoughEvents(match: MatchModel, events: List<MatchEventModel>): Boolean {
        val active = activeEvents(events)
        val hasStart = active.any { it.type == EventType.PERIOD_START }
        val others = active.count { it.type != EventType.PERIOD_START && it.type != EventType.PERIOD_END }
        return match.status == MatchStatus.FINISHED && hasStart && others >= 1
    }

    /** "Juan Pérez Soto" -> "J. Pérez" (no se envían nombres completos). */
    fun shortName(full: String): String {
        val parts = full.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> ""
            parts.size == 1 -> parts[0]
            else -> "${parts[0].first()}. ${parts[1]}"
        }
    }

    /** JSON mínimo, sin DNI, contactos ni datos físicos. */
    fun buildPayload(match: MatchModel, events: List<MatchEventModel>): String {
        val root = JsonObject()
        root.add("partido", JsonObject().apply {
            addProperty("equipo", match.teamName)
            addProperty("rival", match.rival)
            addProperty("fecha", match.date)
            addProperty("categoria", match.category)
            add("resultado", JsonObject().apply {
                addProperty("propio", match.ownScore)
                addProperty("rival", match.rivalScore)
            })
        })
        val arr = com.google.gson.JsonArray()
        activeEvents(events).forEach { e ->
            arr.add(JsonObject().apply {
                addProperty("min", e.minute)
                addProperty("tipo", EventType.labelOf(e.type))
                addProperty("lado", if (e.team == EventTeam.OWN) "PROPIO" else "RIVAL")
                when {
                    e.type == EventType.SUBSTITUTION -> {
                        addProperty("sale", shortName(e.jugadorSaleName))
                        addProperty("entra", shortName(e.jugadorEntraName))
                    }
                    e.jugadorNoIdentificado || e.playerName.isBlank() -> add("jugador", null)
                    else -> addProperty("jugador", shortName(e.playerName))
                }
                if (e.observation.isNotBlank()) addProperty("obs", e.observation.take(140))
            })
        }
        root.add("eventos", arr)
        return root.toString()
    }

    /** Nombres que pueden aparecer en el resumen (los registrados en eventos). */
    private fun knownNames(events: List<MatchEventModel>): Set<String> =
        activeEvents(events).flatMap { listOf(it.playerName, it.jugadorSaleName, it.jugadorEntraName) }
            .filter { it.isNotBlank() }
            .flatMap { listOf(shortName(it), it) }
            .map { Validators.normalize(it) }
            .toSet()

    /** Validación automática de la respuesta de la IA (US-25). */
    fun validate(facts: String, mentioned: List<String>, match: MatchModel, events: List<MatchEventModel>): List<String> {
        val warnings = mutableListOf<String>()
        val known = knownNames(events)
        val unknown = mentioned.filter { name ->
            val n = Validators.normalize(name)
            n.isNotBlank() && known.none { k -> k.contains(n) || n.contains(k) }
        }
        if (unknown.isNotEmpty()) warnings += "Menciona jugadores no registrados: ${unknown.joinToString()}"
        val score1 = "${match.ownScore}-${match.rivalScore}"
        val score2 = "${match.ownScore} a ${match.rivalScore}"
        val score3 = "${match.rivalScore}-${match.ownScore}"
        val f = facts.replace(" ", "")
        if (!f.contains(score1.replace(" ", "")) && !facts.contains(score2) && !f.contains(score3)) {
            warnings += "No se encontró el marcador $score1 en el texto: verifícalo"
        }
        return warnings
    }

    /**
     * Genera (o regenera) el resumen. Queda en PENDIENTE_REVISION hasta que el DT lo apruebe.
     */
    suspend fun generate(match: MatchModel, events: List<MatchEventModel>, dt: UserModel): Result<MatchSummaryModel> {
        return try {
            if (!GeminiClient.isConfigured) throw Exception("Falta configurar GEMINI_API_KEY en local.properties")
            if (dt.uid != match.dtUid) throw Exception("Solo el Director Técnico del partido puede generar el resumen")
            if (!hasEnoughEvents(match, events))
                throw Exception("No hay eventos suficientes para generar un resumen completo")

            val previous = summaries.document(match.id).get().await().toObject(MatchSummaryModel::class.java)
            val generation = (previous?.generation ?: 0) + 1
            if (previous != null && previous.status == SummaryStatus.PUBLISHED)
                throw Exception("El resumen ya fue publicado")
            if (generation > MAX_GENERATIONS)
                throw Exception("Alcanzaste el límite de $MAX_GENERATIONS generaciones para este partido")

            val payload = buildPayload(match, events)
            val request = GeminiRequest(contents = listOf(GeminiContent(parts = listOf(GeminiPart("$PROMPT\n\nJSON:\n$payload")))))
            val response = GeminiClient.api.generateContent(GeminiClient.model, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("La IA no devolvió respuesta. Intenta nuevamente")

            val json = try {
                JsonParser.parseString(text.trim().removePrefix("```json").removeSuffix("```").trim()).asJsonObject
            } catch (e: Exception) {
                throw Exception("La respuesta de la IA no tiene el formato esperado. Intenta nuevamente")
            }
            val facts = json.get("hechos")?.asString.orEmpty()
            val analysis = json.get("analisis")?.asString.orEmpty()
            val mentioned = json.getAsJsonArray("jugadores_mencionados")?.map { it.asString }.orEmpty()
            if (facts.isBlank()) throw Exception("La IA devolvió un resumen vacío. Intenta nuevamente")

            val summary = MatchSummaryModel(
                matchId = match.id, teamId = match.teamId, teamName = match.teamName,
                rival = match.rival, date = match.date,
                ownScore = match.ownScore, rivalScore = match.rivalScore,
                aiFacts = facts, aiAnalysis = analysis,
                finalFacts = facts, finalAnalysis = analysis,
                warnings = validate(facts, mentioned, match, events),
                status = SummaryStatus.PENDING_REVIEW,
                generation = generation, model = GeminiClient.model, promptVersion = PROMPT_VERSION,
                memberUids = match.memberUids, createdBy = dt.uid
            )
            summaries.document(match.id).set(summary).await()
            Result.success(summary)
        } catch (e: retrofit2.HttpException) {
            Result.failure(Exception("Error del proveedor de IA (${e.code()}). Revisa la clave o intenta nuevamente"))
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Sin conexión con el proveedor de IA. Reintentar"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** US-26: el DT guarda su edición sin publicar. El texto original de la IA no se toca. */
    suspend fun saveEdit(matchId: String, facts: String, analysis: String): Result<Unit> = try {
        summaries.document(matchId).update(mapOf("finalFacts" to facts, "finalAnalysis" to analysis)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** US-26: Aprobar y publicar. Registra DT y fecha, y notifica al equipo. */
    suspend fun approve(summary: MatchSummaryModel, facts: String, analysis: String, dt: UserModel): Result<Unit> = try {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        summaries.document(summary.matchId).update(
            mapOf(
                "finalFacts" to facts, "finalAnalysis" to analysis,
                "status" to SummaryStatus.PUBLISHED,
                "approvedBy" to dt.uid, "approvedByName" to dt.fullName, "approvedAt" to now
            )
        ).await()
        summary.memberUids.filter { it != dt.uid }.forEach { uid ->
            firestore.collection(NOTIFICATIONS).add(
                hashMapOf(
                    "recipientUid" to uid, "teamId" to summary.teamId,
                    "title" to "Resumen publicado: ${summary.teamName} vs ${summary.rival}",
                    "message" to "Ya puedes leer el resumen del partido.",
                    "type" to "SUMMARY", "relatedId" to summary.matchId, "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** US-26: rechazar (se descarta y se puede regenerar) o despublicar. */
    suspend fun setStatus(matchId: String, status: String): Result<Unit> = try {
        summaries.document(matchId).update("status", status).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
