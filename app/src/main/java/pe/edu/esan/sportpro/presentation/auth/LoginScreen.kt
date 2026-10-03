package pe.edu.esan.sportpro.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.presentation.components.FormTextField

/** US-01: inicio de sesión con correo y contraseña. */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onGoRegister: () -> Unit,
    onForgotPassword: () -> Unit
) {
    val ui by viewModel.ui.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.clearMessages() }

    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxSize()
            .statusBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SportPro", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text("Gestión de equipos y academias de fútbol", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(32.dp))
        Text("Iniciar sesión", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        FormTextField(value = email, onValueChange = { email = it }, label = "Correo electrónico",
            keyboardType = KeyboardType.Email)
        FormTextField(value = password, onValueChange = { password = it }, label = "Contraseña",
            isPassword = true, keyboardType = KeyboardType.Password)

        ui.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
        }

        TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.End)) {
            Text("¿Olvidaste tu contraseña?")
        }

        Button(
            onClick = { viewModel.login(email, password) },
            enabled = email.isNotBlank() && password.isNotBlank() && !ui.loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (ui.loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Ingresar")
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onGoRegister) { Text("¿No tienes cuenta? Crear cuenta") }
    }
}
