package pe.edu.esan.sportpro.presentation.players

import android.widget.Toast
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.edu.esan.sportpro.data.model.Catalogs
import pe.edu.esan.sportpro.presentation.components.DateField
import pe.edu.esan.sportpro.presentation.components.DropdownField
import pe.edu.esan.sportpro.presentation.components.FormTextField
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.presentation.components.SectionTitle

/** US-04: formulario del perfil del jugador por secciones. */
@Composable
fun PlayerFormScreen(
    teamId: String,
    playerId: String?,
    dtUid: String,
    onDone: () -> Unit,
    viewModel: PlayerFormViewModel = viewModel()
) {
    val s by viewModel.state.collectAsState()
    val context = LocalContext.current
    val e = s.errors

    LaunchedEffect(teamId, playerId) { viewModel.load(teamId, playerId, dtUid) }
    LaunchedEffect(s.saved) {
        if (s.saved) {
            Toast.makeText(context, "Perfil guardado", Toast.LENGTH_SHORT).show()
            onDone()
        }
    }
    if (s.loading) { LoadingBox(); return }

    fun digits(max: Int, v: String) = v.filter(Char::isDigit).take(max)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        SectionTitle("Datos personales")
        FormTextField(s.name, { v -> viewModel.onChange { it.copy(name = v) } }, "Nombres *", error = e["name"])
        FormTextField(s.lastName, { v -> viewModel.onChange { it.copy(lastName = v) } }, "Apellidos *", error = e["lastName"])
        DateField("Fecha de nacimiento *", s.birthDate, { v -> viewModel.onChange { it.copy(birthDate = v) } }, error = e["birthDate"])
        FormTextField(s.dni, { v -> viewModel.onChange { it.copy(dni = digits(8, v)) } }, "DNI",
            error = e["dni"], keyboardType = KeyboardType.Number, locked = s.isEdit)

        SectionTitle("Datos deportivos")
        DropdownField("Categoría *", Catalogs.categories, s.category, { v -> viewModel.onChange { it.copy(category = v) } }, error = e["category"])
        DropdownField("Posición *", Catalogs.positions, s.position, { v -> viewModel.onChange { it.copy(position = v) } }, error = e["position"])
        FormTextField(s.shirtNumber, { v -> viewModel.onChange { it.copy(shirtNumber = digits(2, v)) } }, "Número de camiseta (1-99)",
            error = e["shirtNumber"], keyboardType = KeyboardType.Number)

        SectionTitle("Datos físicos")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormTextField(s.heightCm, { v -> viewModel.onChange { it.copy(heightCm = digits(3, v)) } }, "Altura (cm)",
                error = e["heightCm"], keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
            FormTextField(s.weightKg, { v -> viewModel.onChange { it.copy(weightKg = digits(3, v)) } }, "Peso (kg)",
                error = e["weightKg"], keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
        }
        DropdownField("Pie hábil", Catalogs.feet, s.foot, { v -> viewModel.onChange { it.copy(foot = v) } })

        SectionTitle("Contacto")
        FormTextField(s.phone, { v -> viewModel.onChange { it.copy(phone = digits(9, v)) } }, "Teléfono",
            error = e["phone"], keyboardType = KeyboardType.Phone)
        FormTextField(s.email, { v -> viewModel.onChange { it.copy(email = v) } }, "Correo electrónico",
            error = e["email"], keyboardType = KeyboardType.Email)

        SectionTitle("Contacto de emergencia")
        FormTextField(s.emergencyName, { v -> viewModel.onChange { it.copy(emergencyName = v) } }, "Nombre *", error = e["emergencyName"])
        DropdownField("Parentesco *", Catalogs.relations + "Otro", s.emergencyRelation,
            { v -> viewModel.onChange { it.copy(emergencyRelation = v) } }, error = e["emergencyRelation"])
        FormTextField(s.emergencyPhone, { v -> viewModel.onChange { it.copy(emergencyPhone = digits(9, v)) } }, "Teléfono *",
            error = e["emergencyPhone"], keyboardType = KeyboardType.Phone)

        Text("Los datos físicos y de contacto solo son visibles para el DT, el administrador, el jugador y su padre/apoderado.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(vertical = 8.dp))
        s.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(onClick = { viewModel.save(dtUid) }, enabled = !s.saving, modifier = Modifier.fillMaxWidth()) {
            if (s.saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(if (s.isEdit) "Guardar cambios" else "Registrar jugador")
        }
    }
}
