package pe.edu.esan.sportpro.presentation.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.edu.esan.sportpro.data.model.AnnouncementType
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.presentation.components.DateField
import pe.edu.esan.sportpro.presentation.components.DropdownField
import pe.edu.esan.sportpro.presentation.components.FormTextField
import pe.edu.esan.sportpro.presentation.components.SectionTitle

private val roleLabels = mapOf("DT" to "Directores Técnicos", "JUG" to "Jugadores", "PAD" to "Padres")

/** US-22: formulario de anuncio con tipo, destinatarios y vigencia. */
@Composable
fun AnnouncementFormScreen(
    user: UserModel,
    editId: String?,
    onDone: () -> Unit,
    viewModel: AnnouncementFormViewModel = viewModel()
) {
    val s by viewModel.state.collectAsState()
    val e = s.errors
    LaunchedEffect(editId) { viewModel.start(user, editId) }
    LaunchedEffect(s.saved) { if (s.saved) onDone() }

    val isAdmin = user.roleEnum == Role.ADM
    val teamOptions = (if (isAdmin) listOf("Toda la academia") else emptyList()) + s.teams.map { it.name }
    val selectedTeam = s.teams.firstOrNull { it.id == s.teamId }?.name ?: if (isAdmin) "Toda la academia" else ""

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FormTextField(s.title, { v -> viewModel.onChange { it.copy(title = v.take(80)) } }, "Título * (${s.title.length}/80)", error = e["title"])
        FormTextField(s.description, { v -> viewModel.onChange { it.copy(description = v.take(500)) } },
            "Descripción * (${s.description.length}/500)", error = e["description"], singleLine = false)
        DropdownField("Tipo *", AnnouncementType.ALL, s.type, { v -> viewModel.onChange { it.copy(type = v) } })

        SectionTitle("Destinatarios")
        DropdownField(
            label = if (isAdmin) "Equipo o academia *" else "Equipo *",
            options = teamOptions, selected = selectedTeam,
            onSelected = { name ->
                val id = s.teams.firstOrNull { it.name == name }?.id ?: ""
                viewModel.onChange { it.copy(teamId = id) }
            },
            error = e["teamId"] ?: if (!isAdmin && s.teams.isEmpty()) "No tienes equipos aprobados" else null
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            roleLabels.forEach { (role, label) ->
                FilterChip(
                    selected = role in s.roles,
                    onClick = { viewModel.onChange { it.copy(roles = if (role in it.roles) it.roles - role else it.roles + role) } },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        e["roles"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        DateField("Vigente hasta *", s.validUntil, { v -> viewModel.onChange { it.copy(validUntil = v) } }, error = e["validUntil"])

        Text("Al publicar se enviará una notificación a los destinatarios.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        s.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(onClick = { viewModel.save(user) }, enabled = !s.saving, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            if (s.saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(if (s.id.isBlank()) "Publicar anuncio" else "Guardar cambios")
        }
    }
}
