package pe.edu.esan.sportpro.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.remote.AttendanceRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private fun esMayorDeEdad(fecha: String): Boolean {
    val formato = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    formato.isLenient = false
    return try {
        val nacimiento = Calendar.getInstance()
        nacimiento.time = formato.parse(fecha) ?: return false
        val limite = Calendar.getInstance()
        limite.add(Calendar.YEAR, -18)
        !nacimiento.after(limite)
    } catch (_: Exception) {
        false
    }
}

@Composable
fun PlayerEditSection(uid: String) {
    var telefono by remember(uid) { mutableStateOf("") }
    var correo by remember(uid) { mutableStateOf("") }
    var emergencia by remember(uid) { mutableStateOf("") }
    var parentesco by remember(uid) { mutableStateOf("") }
    var telefonoEmergencia by remember(uid) { mutableStateOf("") }
    var nacimiento by remember(uid) { mutableStateOf("") }
    var jugadorId by remember(uid) { mutableStateOf<String?>(null) }
    var cargando by remember(uid) { mutableStateOf(true) }
    var guardando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    var editando by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val db = remember { FirebaseFirestore.getInstance() }
    val mayor = esMayorDeEdad(nacimiento)

    LaunchedEffect(uid) {
        try {
            val usuario = db.collection("users").document(uid).get().await()
            telefono = usuario.getString("phone").orEmpty()
            correo = usuario.getString("email").orEmpty()
            nacimiento = usuario.getString("birthDate").orEmpty()

            val jugador = AttendanceRepository
                .getPlayerByUserUid(uid).getOrNull()

            jugadorId = jugador?.id
            emergencia = jugador?.emergencyName.orEmpty()
            parentesco = jugador?.emergencyRelation.orEmpty()
            telefonoEmergencia = jugador?.emergencyPhone.orEmpty()
        } catch (e: Exception) {
            mensaje = e.localizedMessage ?: "Error al cargar los datos"
        } finally {
            cargando = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Editar perfil", style = MaterialTheme.typography.titleMedium)

        if (cargando) {
            CircularProgressIndicator()
        } else {
            Button(onClick = { editando = !editando }) {
                Text(if (editando) "Cancelar edicion" else "Editar perfil")
            }

            if (editando) {
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Telefono") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    label = { Text("Correo de contacto") },
                    singleLine = true
                )

                if (mayor && jugadorId != null) {
                    Text("Contacto de emergencia")

                    OutlinedTextField(
                        value = emergencia,
                        onValueChange = { emergencia = it },
                        label = { Text("Nombre del contacto") }
                    )

                    OutlinedTextField(
                        value = parentesco,
                        onValueChange = { parentesco = it },
                        label = { Text("Parentesco") }
                    )

                    OutlinedTextField(
                        value = telefonoEmergencia,
                        onValueChange = { telefonoEmergencia = it },
                        label = { Text("Telefono de emergencia") }
                    )
                } else {
                    Text(
                        "El contacto de emergencia solo puede editarlo " +
                        "el PAD si el jugador es menor de edad o " +
                        "no tiene fecha de nacimiento valida."
                    )
                }

                Button(
                    enabled = !guardando,
                    onClick = {
                        mensaje = when {
                            !telefono.matches(Regex("[0-9]{9}")) ->
                                "El telefono debe tener 9 digitos"

                            !android.util.Patterns.EMAIL_ADDRESS
                                .matcher(correo.trim()).matches() ->
                                "Ingresa un correo valido"

                            mayor && jugadorId != null &&
                                (emergencia.isBlank() ||
                                 parentesco.isBlank() ||
                                 !telefonoEmergencia.matches(Regex("[0-9]{9}"))) ->
                                "Revisa los datos del contacto de emergencia"

                            else -> ""
                        }

                        if (mensaje.isEmpty()) scope.launch {
                            guardando = true
                            try {
                                check(FirebaseAuth.getInstance().currentUser?.uid == uid) {
                                    "Sesion no valida"
                                }

                                val batch = db.batch()
                                val usuarioRef = db.collection("users").document(uid)

                                batch.update(usuarioRef, mapOf(
                                    "phone" to telefono,
                                    "email" to correo.trim()
                                ))

                                jugadorId?.let { id ->
                                    val ref = db.collection("players").document(id)
                                    val cambios = mutableMapOf<String, Any>(
                                        "phone" to telefono,
                                        "email" to correo.trim()
                                    )

                                    if (mayor) {
                                        cambios["emergencyName"] = emergencia.trim()
                                        cambios["emergencyRelation"] = parentesco.trim()
                                        cambios["emergencyPhone"] = telefonoEmergencia
                                    }

                                    batch.update(ref, cambios)
                                }

                                batch.commit().await()
                                mensaje = "Perfil actualizado"
                                editando = false
                            } catch (e: Exception) {
                                mensaje = "Error: ${e.localizedMessage}"
                            } finally {
                                guardando = false
                            }
                        }
                    }
                ) {
                    Text(if (guardando) "Guardando..." else "Guardar cambios")
                }
            }
        }

        if (mensaje.isNotBlank()) {
            Text(mensaje, style = MaterialTheme.typography.bodySmall)
        }
    }
}
