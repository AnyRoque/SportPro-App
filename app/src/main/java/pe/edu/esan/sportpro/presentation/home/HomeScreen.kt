package pe.edu.esan.sportpro.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.presentation.navigation.Routes

private data class Shortcut(val title: String, val subtitle: String, val route: String, val icon: ImageVector)

/** Inicio según rol: accesos directos a las funciones autorizadas. */
@Composable
fun HomeScreen(user: UserModel, onNavigate: (String) -> Unit) {
    val shortcuts = when (user.roleEnum) {
        Role.ADM -> listOf(Shortcut("Validar equipos", "Aprueba o rechaza equipos registrados", Routes.TEAM_APPROVAL, Icons.Default.CheckCircle))
        Role.DT -> listOf(Shortcut("Mis equipos", "Registra equipos y gestiona tu plantel", Routes.MY_TEAMS, Icons.AutoMirrored.Filled.List))
        Role.PAD -> listOf(Shortcut("Mis hijos", "Registra a tus hijos y vincúlalos a su equipo", Routes.MY_CHILDREN, Icons.Default.Face))
        Role.JUG -> emptyList()
        null -> emptyList()
    } + Shortcut("Mi perfil", "Tus datos de cuenta", Routes.PROFILE, Icons.Default.Person)

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Hola, ${user.name}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(user.roleEnum?.label ?: "", color = MaterialTheme.colorScheme.primary)
        shortcuts.forEach { sc ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigate(sc.route) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(sc.icon, null, modifier = Modifier.size(32.dp))
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(sc.title, style = MaterialTheme.typography.titleMedium)
                        Text(sc.subtitle, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (user.roleEnum == Role.JUG) {
            Text("Pronto: convocatorias, asistencia y estadísticas.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
        }
    }
}
