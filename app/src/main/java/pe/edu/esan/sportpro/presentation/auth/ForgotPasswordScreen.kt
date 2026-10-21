package pe.edu.esan.sportpro.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.presentation.components.FormTextField

/** US-01: recuperar contraseña por enlace al correo. */
@Composable
fun ForgotPasswordScreen(viewModel: AuthViewModel, onBack: () -> Unit) {
    val ui by viewModel.ui.collectAsState()
    var email by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.clearMessages() }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, androidx.compose.ui.Alignment.CenterVertically)
    ) {
        Text("Recuperar contraseña", style = MaterialTheme.typography.headlineSmall)
        Text("Ingresa tu correo y te enviaremos un enlace para crear una nueva contraseña.")
        FormTextField(email, { email = it }, "Correo electrónico", keyboardType = KeyboardType.Email)
        ui.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        ui.infoMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Button(onClick = { viewModel.sendPasswordReset(email) }, enabled = !ui.loading, modifier = Modifier.fillMaxWidth()) {
            Text("Enviar enlace")
        }
        TextButton(onClick = onBack) { Text("Volver a iniciar sesión") }
    }
}
