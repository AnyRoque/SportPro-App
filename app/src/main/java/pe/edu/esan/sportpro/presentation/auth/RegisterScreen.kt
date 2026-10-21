package pe.edu.esan.sportpro.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.presentation.components.DateField
import pe.edu.esan.sportpro.presentation.components.FormTextField
import pe.edu.esan.sportpro.util.RegisterForm

/** US-01: registro público. Solo roles DT, JUG y PAD (sin ADM). */
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onGoLogin: () -> Unit
) {
    val ui by viewModel.ui.collectAsState()
    var form by remember { mutableStateOf(RegisterForm()) }
    val err = ui.fieldErrors

    LaunchedEffect(Unit) { viewModel.clearMessages() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Crear cuenta", style = MaterialTheme.typography.headlineSmall)
        Text("Selecciona tu rol", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        Role.selfRegister.forEach { role ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = form.role == role,
                    onClick = { form = form.copy(role = role); viewModel.clearFieldError("role") }
                )
                Text(role.label)
            }
        }
        err["role"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        FormTextField(form.name, { form = form.copy(name = it) }, "Nombres", error = err["name"])
        FormTextField(form.lastName, { form = form.copy(lastName = it) }, "Apellidos", error = err["lastName"])
        FormTextField(form.email, { form = form.copy(email = it) }, "Correo electrónico",
            error = err["email"], keyboardType = KeyboardType.Email)
        FormTextField(form.phone, { if (it.length <= 9) form = form.copy(phone = it.filter(Char::isDigit)) },
            "Teléfono (9 dígitos)", error = err["phone"], keyboardType = KeyboardType.Phone)

        // Solo para jugadores: los menores deben ser registrados por su PAD
        if (form.role == Role.JUG) {
            DateField("Fecha de nacimiento", form.birthDate, { form = form.copy(birthDate = it) }, error = err["birthDate"])
        }

        FormTextField(form.password, { form = form.copy(password = it) }, "Contraseña",
            error = err["password"], isPassword = true, keyboardType = KeyboardType.Password)
        FormTextField(form.confirmPassword, { form = form.copy(confirmPassword = it) }, "Confirmar contraseña",
            error = err["confirmPassword"], isPassword = true, keyboardType = KeyboardType.Password)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = form.acceptedTerms, onCheckedChange = { form = form.copy(acceptedTerms = it) })
            Text("Acepto los términos de uso y la política de privacidad", style = MaterialTheme.typography.bodySmall)
        }
        err["terms"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        if (form.role == Role.PAD) {
            Text("Después de registrarte podrás agregar los datos de tus hijos.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        ui.errorMessage?.let { if (err.isEmpty()) Text(it, color = MaterialTheme.colorScheme.error) }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { viewModel.register(form) },
            enabled = !ui.loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (ui.loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Registrarme")
        }
        TextButton(onClick = onGoLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}
