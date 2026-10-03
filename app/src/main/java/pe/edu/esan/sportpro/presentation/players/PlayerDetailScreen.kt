package pe.edu.esan.sportpro.presentation.players

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.remote.PlayerRepository
import pe.edu.esan.sportpro.presentation.components.ConfirmDialog
import pe.edu.esan.sportpro.presentation.components.EmptyState
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.presentation.components.StatusChip

/** US-04: perfil completo del jugador con opciones Editar y Dar de baja. */
@Composable
fun PlayerDetailScreen(
    playerId: String,
    onEdit: (PlayerModel) -> Unit,
    canEdit: Boolean = true
) {
    var player by remember { mutableStateOf<PlayerModel?>(null) }
    var loading by remember { mutableStateOf(true) }
    var confirmToggle by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(playerId, reload) {
        player = PlayerRepository.getPlayer(playerId).getOrNull()
        loading = false
    }

    if (loading) { LoadingBox(); return }
    val p = player ?: run { EmptyState("Jugador no encontrado"); return }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.fullName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${p.teamName} · ${p.category}")
            }
            StatusChip(if (p.active) "ACTIVO" else "INACTIVO")
        }
        InfoCard("Datos personales", listOf(
            "Fecha de nacimiento" to p.birthDate, "DNI" to p.dni.ifBlank { "—" }
        ))
        InfoCard("Datos deportivos", listOf(
            "Posición" to p.position, "Número" to (p.shirtNumber?.toString() ?: "—")
        ))
        InfoCard("Datos físicos", listOf(
            "Altura" to (p.heightCm?.let { "$it cm" } ?: "—"),
            "Peso" to (p.weightKg?.let { "$it kg" } ?: "—"),
            "Pie hábil" to p.foot.ifBlank { "—" }
        ))
        InfoCard("Contacto", listOf("Teléfono" to p.phone.ifBlank { "—" }, "Correo" to p.email.ifBlank { "—" }))
        InfoCard("Contacto de emergencia", listOf(
            "Nombre" to p.emergencyName, "Parentesco" to p.emergencyRelation, "Teléfono" to p.emergencyPhone
        ))

        if (canEdit) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onEdit(p) }, modifier = Modifier.weight(1f)) { Text("Editar") }
                OutlinedButton(onClick = { confirmToggle = true }, modifier = Modifier.weight(1f)) {
                    Text(if (p.active) "Dar de baja" else "Reactivar")
                }
            }
        }
    }

    if (confirmToggle) {
        ConfirmDialog(
            title = if (p.active) "Dar de baja" else "Reactivar",
            message = if (p.active) "${p.fullName} pasará a Inactivo. Su historial no se eliminará."
                      else "${p.fullName} volverá a estar activo en el plantel.",
            onConfirm = {
                confirmToggle = false
                scope.launch {
                    PlayerRepository.setActive(p.id, !p.active)
                    reload++
                }
            },
            onDismiss = { confirmToggle = false }
        )
    }
}

@Composable
private fun InfoCard(title: String, rows: List<Pair<String, String>>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            rows.forEach { (label, value) ->
                Row {
                    Text("$label: ", fontWeight = FontWeight.SemiBold)
                    Text(value)
                }
            }
        }
    }
}
