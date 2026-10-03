package pe.edu.esan.sportpro.presentation.children

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.edu.esan.sportpro.data.model.Catalogs
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.presentation.components.DateField
import pe.edu.esan.sportpro.presentation.components.DropdownField
import pe.edu.esan.sportpro.presentation.components.FormTextField
import pe.edu.esan.sportpro.presentation.components.SectionTitle

/** US-02: registro de datos del hijo y vínculo a su equipo. */
@Composable
fun ChildFormScreen(
    parent: UserModel,
    onDone: () -> Unit,
    viewModel: ChildFormViewModel = viewModel()
) {
    val s by viewModel.state.collectAsState()
    val e = s.errors
    // Solo equipos aprobados de la categoría seleccionada
    val teamsForCategory = s.teams.filter { s.category.isBlank() || it.category == s.category }
    val selectedTeamName = s.teams.firstOrNull { it.id == s.teamId }?.name ?: ""

    if (s.finished) {
        AlertDialog(
            onDismissRequest = onDone,
            title = { Text("Solicitud enviada al Director Técnico") },
            text = { Text("Cuando el DT acepte el vínculo podrás ver la información de tu hijo en “Mis hijos”.") },
            confirmButton = { Button(onClick = onDone) { Text("Ir a Mis hijos") } }
        )
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text("Registra los datos de tu hijo para vincularlo a su equipo.", style = MaterialTheme.typography.bodyMedium)
        if (s.savedCount > 0) Text("Hijos registrados en esta sesión: ${s.savedCount}", color = MaterialTheme.colorScheme.primary)

        SectionTitle("Datos del hijo")
        FormTextField(s.name, { v -> viewModel.onChange { it.copy(name = v) } }, "Nombres *", error = e["name"])
        FormTextField(s.lastName, { v -> viewModel.onChange { it.copy(lastName = v) } }, "Apellidos *", error = e["lastName"])
        DateField("Fecha de nacimiento *", s.birthDate, viewModel::onBirthDate, error = e["birthDate"])
        FormTextField(s.dni, { v -> viewModel.onChange { it.copy(dni = v.filter(Char::isDigit).take(8)) } }, "DNI *",
            error = e["dni"], keyboardType = KeyboardType.Number)
        DropdownField("Sexo *", Catalogs.sexes, s.sex, { v -> viewModel.onChange { it.copy(sex = v) } }, error = e["sex"])
        DropdownField("Parentesco *", Catalogs.relations, s.relation, { v -> viewModel.onChange { it.copy(relation = v) } }, error = e["relation"])

        SectionTitle("Equipo")
        DropdownField("Categoría * (sugerida por edad)", Catalogs.categories, s.category,
            { v -> viewModel.onChange { it.copy(category = v, teamId = "") } }, error = e["category"])
        DropdownField(
            label = "Equipo / academia *",
            options = teamsForCategory.map { it.name },
            selected = selectedTeamName,
            onSelected = { name ->
                val id = teamsForCategory.firstOrNull { it.name == name }?.id ?: ""
                viewModel.onChange { it.copy(teamId = id) }
            },
            error = e["teamId"] ?: if (teamsForCategory.isEmpty()) "No hay equipos aprobados en esta categoría" else null
        )

        s.message?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp)) }

        Button(onClick = { viewModel.save(parent, addAnother = false) }, enabled = !s.saving, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Guardar")
        }
        OutlinedButton(onClick = { viewModel.save(parent, addAnother = true) }, enabled = !s.saving, modifier = Modifier.fillMaxWidth()) {
            Text("Guardar y agregar otro hijo")
        }
    }
}
