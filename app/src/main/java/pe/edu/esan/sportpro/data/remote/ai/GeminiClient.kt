package pe.edu.esan.sportpro.data.remote.ai

import okhttp3.OkHttpClient
import pe.edu.esan.sportpro.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente Retrofit para el proveedor de IA (mismo patrón que RetrofitInstance del curso).
 * La clave se lee de local.properties -> BuildConfig.GEMINI_API_KEY (no se sube al repositorio).
 */
object GeminiClient {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    val model: String get() = BuildConfig.GEMINI_MODEL
    val isConfigured: Boolean get() = BuildConfig.GEMINI_API_KEY.isNotBlank()

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
                .build()
            chain.proceed(request)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: GeminiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiService::class.java)
    }
}
