package pe.edu.esan.sportpro.presentation.teams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import pe.edu.esan.sportpro.data.model.Catalogs
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.presentation.components.DropdownField
import pe.edu.esan.sportpro.presentation.components.FormTextField
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.presentation.components.SectionTitle

/** US-03: formulario "Nuevo equipo" / "Corregir y reenviar". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeamFormScreen(
    dt: UserModel,
    teamId: String?,
    onDone: () -> Unit,
    viewModel: TeamFormViewModel = viewModel()
) {
    val s by viewModel.state.collectAsState()
    LaunchedEffect(teamId) { viewModel.load(teamId) }

    if (s.loading) {
        LoadingBox(); return
    }

    if (s.saved) {
        AlertDialog(
            onDismissRequest = onDone,
            title = { Text("Equipo enviado para aprobación") },
            text = { Text("El administrador revisará tu solicitud. Recibirás el resultado en “Mis equipos”.") },
            confirmButton = { Button(onClick = onDone) { Text("Entendido") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SectionTitle("Datos del equipo")
        FormTextField(s.name, { v -> viewModel.onChange { it.copy(name = v) } }, "Nombre del equipo / academia *", error = s.errors["name"])
        DropdownField("Categoría *", Catalogs.categories, s.category,
            { v -> viewModel.onChange { it.copy(category = v) } }, error = s.errors["category"])
        FormTextField(s.field, { v -> viewModel.onChange { it.copy(field = v) } }, "Campo de entrenamiento *", error = s.errors["field"])
        FormTextField(s.district, { v -> viewModel.onChange { it.copy(district = v) } }, "Distrito *", error = s.errors["district"])

        SectionTitle("Horario de entrenamiento")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Catalogs.weekDays.forEach { day ->
                FilterChip(
                    selected = day in s.days,
                    onClick = { viewModel.onChange { it.copy(days = if (day in it.days) it.days - day else it.days + day) } },
                    label = { Text(day) }
                )
            }
        }
        FormTextField(s.time, { v -> viewModel.onChange { it.copy(time = v) } }, "Horario (ej. 16:00 - 18:00)")

        Text("Tú quedarás asignado como Director Técnico del equipo. El equipo quedará pendiente hasta que el administrador lo apruebe.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(vertical = 8.dp))

        s.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(onClick = { viewModel.save(dt) }, enabled = !s.saving, modifier = Modifier.fillMaxWidth()) {
            if (s.saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(if (s.editingId == null) "Registrar equipo" else "Corregir y reenviar")
        }
    }
}
