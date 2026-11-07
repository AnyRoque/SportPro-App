package pe.edu.esan.sportpro.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.remote.AttendanceRepository

@Composable
fun PlayerSportsSection(userUid: String) {
    var player by remember(userUid) {
        mutableStateOf<PlayerModel?>(null)
    }
    var loading by remember(userUid) { mutableStateOf(true) }
    var error by remember(userUid) { mutableStateOf<String?>(null) }

    LaunchedEffect(userUid) {
        loading = true
        val result = AttendanceRepository.getPlayerByUserUid(userUid)
        player = result.getOrNull()
        error = result.exceptionOrNull()?.message
        loading = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Datos deportivos", style = MaterialTheme.typography.titleMedium)

        when {
            loading -> CircularProgressIndicator()

            error != null -> Text(
                error ?: "No se pudo cargar el perfil",
                color = MaterialTheme.colorScheme.error
            )

            player == null -> Text("No hay datos deportivos vinculados")

            else -> {
                val p = player!!
                SportsField("Equipo", p.teamName)
                SportsField("Categoria", p.category)
                SportsField("Posicion", p.position)
                SportsField("Numero de camiseta", p.shirtNumber?.toString())
                SportsField("Altura", p.heightCm?.let { "$it cm" })
                SportsField("Peso", p.weightKg?.let { "$it kg" })
                SportsField("Pie habil", p.foot)

                HorizontalDivider()
                Text("Contacto de emergencia")
                Text("Nombre: ${p.emergencyName.ifBlank { "Sin registrar" }}")
                Text("Parentesco: ${p.emergencyRelation.ifBlank { "Sin registrar" }}")
                Text("Telefono: ${p.emergencyPhone.ifBlank { "Sin registrar" }}")
            }
        }
    }
}

@Composable
private fun SportsField(label: String, value: String?) {
    Text("$label: ${value?.takeIf { it.isNotBlank() } ?: "Sin registrar"}  🔒")
}
