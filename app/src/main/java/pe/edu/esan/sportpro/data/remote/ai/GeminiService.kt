package pe.edu.esan.sportpro.data.remote.ai

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/** API REST de Gemini (Google AI Studio). Documentación: ai.google.dev/api/generate-content */
interface GeminiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// ---------- Request ----------
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig = GeminiGenerationConfig()
)

data class GeminiContent(val parts: List<GeminiPart>, val role: String = "user")

data class GeminiPart(val text: String)

data class GeminiGenerationConfig(
    val temperature: Double = 0.3,
    val responseMimeType: String = "application/json"
)

// ---------- Response ----------
data class GeminiResponse(val candidates: List<GeminiCandidate>? = null)

data class GeminiCandidate(val content: GeminiContent? = null)
