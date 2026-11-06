package pe.edu.esan.sportpro.presentation.admin

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.AdminModel
import pe.edu.esan.sportpro.data.remote.AdminRepository
import pe.edu.esan.sportpro.presentation.components.DropdownField
import pe.edu.esan.sportpro.presentation.components.FormTextField
import pe.edu.esan.sportpro.util.Validators

/**
 * Formulario para el registro de un "Nuevo Administrador" (US-06).
 * Permite ingresar datos personales, contraseña, asignar club y registrar el correo en la lista de administradores autorizados.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAdminScreen(
    creatorEmail: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedClub by remember { mutableStateOf(AdminRepository.AVAILABLE_CLUBS.first()) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var lastNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    var loading by remember { mutableStateOf(false) }

    fun validateForm(): Boolean {
        var isValid = true

        if (name.isBlank()) {
            nameError = "El nombre es obligatorio"
            isValid = false
        } else {
            nameError = null
        }

        if (lastName.isBlank()) {
            lastNameError = "El apellido es obligatorio"
            isValid = false
        } else {
            lastNameError = null
        }

        if (!Validators.isValidEmail(email)) {
            emailError = "Ingresa un correo electrónico válido"
            isValid = false
        } else {
            emailError = null
        }

        if (password.length < 6) {
            passwordError = "La contraseña debe tener al menos 6 caracteres"
            isValid = false
        } else {
            passwordError = null
        }

        if (phone.isBlank() || phone.length < 7) {
            phoneError = "Ingresa un número de teléfono válido (mínimo 7 dígitos)"
            isValid = false
        } else {
            phoneError = null
        }

        return isValid
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Administrador") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Registrar Nuevo Administrador",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "El usuario se registrará en Firebase Auth y quedará autorizado como Administrador.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FormTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (nameError != null) nameError = null
                },
                label = "Nombres",
                error = nameError
            )
            Spacer(modifier = Modifier.height(12.dp))

            FormTextField(
                value = lastName,
                onValueChange = {
                    lastName = it
                    if (lastNameError != null) lastNameError = null
                },
                label = "Apellidos",
                error = lastNameError
            )
            Spacer(modifier = Modifier.height(12.dp))

            FormTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (emailError != null) emailError = null
                },
                label = "Correo electrónico",
                keyboardType = KeyboardType.Email,
                error = emailError
            )
            Spacer(modifier = Modifier.height(12.dp))

            FormTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (passwordError != null) passwordError = null
                },
                label = "Contraseña",
                isPassword = true,
                keyboardType = KeyboardType.Password,
                error = passwordError
            )
            Spacer(modifier = Modifier.height(12.dp))

            FormTextField(
                value = phone,
                onValueChange = {
                    if (it.all { char -> char.isDigit() || char == '+' || char == ' ' }) {
                        phone = it
                    }
                    if (phoneError != null) phoneError = null
                },
                label = "Teléfono",
                keyboardType = KeyboardType.Number,
                error = phoneError
            )
            Spacer(modifier = Modifier.height(12.dp))

            DropdownField(
                label = "Academia / Club",
                options = AdminRepository.AVAILABLE_CLUBS,
                selected = selectedClub,
                onSelected = { selectedClub = it }
            )
            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    if (!validateForm()) return@Button
                    loading = true
                    scope.launch {
                        val newAdmin = AdminModel(
                            name = name.trim(),
                            lastName = lastName.trim(),
                            email = email.trim().lowercase(),
                            phone = phone.trim(),
                            club = selectedClub
                        )
                        val result = AdminRepository.registerNewAdminWithAuth(
                            admin = newAdmin,
                            password = password,
                            creatorEmail = creatorEmail,
                            context = context
                        )
                        loading = false
                        result.onSuccess {
                            Toast.makeText(
                                context,
                                "Administrador '$email' creado con éxito en Firebase Auth y autorizado.",
                                Toast.LENGTH_LONG
                            ).show()
                            onBack()
                        }.onFailure { e ->
                            val msg = e.message ?: "Error al registrar el administrador"
                            if (msg.contains("ya está registrado")) {
                                emailError = msg
                            }
                            snackbarHostState.showSnackbar(msg)
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar Administrador", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onBack,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
        }
    }
}
