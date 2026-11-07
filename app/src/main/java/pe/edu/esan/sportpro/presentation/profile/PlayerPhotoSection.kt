package pe.edu.esan.sportpro.presentation.profile

import android.net.Uri
import pe.edu.esan.sportpro.data.remote.SportProPhotoStorage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.BuildConfig

/** US-05: solo JUG. No modifica pantallas del DT, PAD ni ADM. */
@Composable
fun PlayerPhotoSection(uid: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photoUrl by remember(uid) { mutableStateOf<String?>(null) }
    var status by remember(uid) { mutableStateOf("") }
    var busy by remember(uid) { mutableStateOf(false) }

    LaunchedEffect(uid) {
        try {
            val snapshot = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
            val path = snapshot.getString("photoStoragePath")
            if (!path.isNullOrBlank()) photoUrl = SportProPhotoStorage.signedUrl(uid, path)
        } catch (ex: Exception) {
            status = "No se pudo cargar la foto: ${ex.localizedMessage ?: "error"}"
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) scope.launch {
            busy = true
            status = "Subiendo fotoâ€¦"
            try {
                val path = SportProPhotoStorage.upload(context, uid, uri)
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .update("photoStoragePath", path).await()
                photoUrl = SportProPhotoStorage.signedUrl(uid, path)
                status = "Foto actualizada"
            } catch (ex: Exception) {
                status = "Error al subir foto: ${ex.localizedMessage ?: "intenta nuevamente"}"
            } finally {
                busy = false
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Foto de perfil del jugador", style = MaterialTheme.typography.titleMedium)
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Mi foto de perfil",
                modifier = Modifier.size(112.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text("AÃºn no tienes foto de perfil")
        }
        Button(onClick = { pickPhoto.launch("image/*") }, enabled = !busy) {
            Text(if (busy) "Subiendoâ€¦" else "Cambiar foto")
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall)
    }
}
