package pe.edu.esan.sportpro.presentation.profile

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.data.model.UserModel

/** Perfil de la cuenta y cierre de sesiÃƒÆ’Ã‚Â³n (US-01). */
@Composable
fun ProfileScreen(user: UserModel, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(user.fullName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Field("Rol", user.roleEnum?.label ?: user.role)
                Field("Correo", user.email)
                Field("TelÃƒÆ’Ã‚Â©fono", user.phone)
                if (user.birthDate.isNotBlank()) Field("Fecha de nacimiento", user.birthDate)
            }
        }
        if (user.roleEnum == pe.edu.esan.sportpro.data.model.Role.JUG) {
    PlayerPhotoSection(user.uid)
    PlayerSportsSection(user.uid)
    PlayerEditSection(user.uid)
}
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesiÃƒÆ’Ã‚Â³n") }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Row {
        Text("$label: ", fontWeight = FontWeight.SemiBold)
        Text(value)
    }
}

