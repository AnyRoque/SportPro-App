package pe.edu.esan.sportpro.data.remote

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pe.edu.esan.sportpro.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Cliente mínimo para Storage; autentica cada solicitud con el ID token de Firebase. */
object SportProPhotoStorage {
    private const val BUCKET = "jugadores-fotos"
    private const val MAX_BYTES = 5 * 1024 * 1024

    private fun config(): Pair<String, String> {
        val host = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val key = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()
        check(host.startsWith("https://") && key.startsWith("sb_publishable_")) {
            "Configura SUPABASE_URL y SUPABASE_PUBLISHABLE_KEY en local.properties"
        }
        return host to key
    }

    private suspend fun token(uid: String): String {
        val current = FirebaseAuth.getInstance().currentUser
            ?: error("Debes iniciar sesión")
        check(current.uid == uid) { "Solo puedes cambiar tu propia foto" }
        return current.getIdToken(false).await().token ?: error("No se pudo obtener el token")
    }

    private fun encodedPath(path: String): String =
        path.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }

    suspend fun upload(context: Context, uid: String, uri: Uri): String {
        val auth = token(uid)
        return withContext(Dispatchers.IO) {
            val (host, key) = config()
            val type = context.contentResolver.getType(uri) ?: ""
            val ext = when (type) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> error("Selecciona una imagen JPG, PNG o WebP")
            }
            val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                input.readNBytes(MAX_BYTES + 1)
            } ?: error("No se pudo leer la imagen")
            check(bytes.isNotEmpty() && bytes.size <= MAX_BYTES) { "La imagen debe pesar máximo 5 MB" }
            val path = "$uid/foto_${System.currentTimeMillis()}.$ext"
            val url = URL("$host/storage/v1/object/$BUCKET/${encodedPath(path)}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 30000
                doOutput = true
                setRequestProperty("apikey", key)
                setRequestProperty("Authorization", "Bearer $auth")
                setRequestProperty("Content-Type", type)
            }
            try {
                conn.outputStream.use { it.write(bytes) }
                val body = (if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                check(conn.responseCode in 200..299) { "Supabase: ${conn.responseCode} $body" }
                path
            } finally { conn.disconnect() }
        }
    }

    suspend fun signedUrl(uid: String, path: String): String {
        require(path.startsWith("$uid/")) { "Foto fuera de tu carpeta" }
        val auth = token(uid)
        return withContext(Dispatchers.IO) {
            val (host, key) = config()
            val url = URL("$host/storage/v1/object/sign/$BUCKET/${encodedPath(path)}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("apikey", key)
                setRequestProperty("Authorization", "Bearer $auth")
                setRequestProperty("Content-Type", "application/json")
            }
            try {
                conn.outputStream.use { it.write("{\"expiresIn\":3600}".toByteArray()) }
                val body = (if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                check(conn.responseCode in 200..299) { "Supabase: ${conn.responseCode} $body" }
                val signed = JSONObject(body).optString("signedURL")
                check(signed.isNotBlank()) { "Supabase no devolvió la URL firmada" }
                when {
                    signed.startsWith("https://") -> signed
                    signed.startsWith("/storage/v1/") -> host + signed
                    signed.startsWith("/") -> host + "/storage/v1" + signed
                    else -> host + "/storage/v1/" + signed
                }
            } finally { conn.disconnect() }
        }
    }
}
