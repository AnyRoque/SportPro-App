package pe.edu.esan.sportpro.presentation.players

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.edu.esan.sportpro.data.model.Catalogs
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.presentation.components.DropdownField
import pe.edu.esan.sportpro.presentation.components.EmptyState
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.presentation.components.StatusChip

/** US-04: plantel (búsqueda, filtro por posición) y solicitudes de vínculo (US-02). */
@Composable
fun RosterScreen(
    teamId: String,
    dtUid: String,
    onAddPlayer: () -> Unit,
    onOpenPlayer: (PlayerModel) -> Unit,
    viewModel: RosterViewModel = viewModel()
) {
    val s by viewModel.state.collectAsState()
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var showUserSearchDialog by remember { mutableStateOf(false) }

    LaunchedEffect(teamId) { viewModel.start(teamId, dtUid) }
    LaunchedEffect(s.message) {
        s.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); viewModel.clearMessage() }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            s.team?.let {
                Text("${it.name} · ${it.category}", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
            }
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Plantel (${s.roster.size})") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Solicitudes (${s.requests.size})") })
            }
            if (s.loading) {
                LoadingBox()
            } else if (tab == 0) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = s.query, onValueChange = viewModel::onQuery,
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        placeholder = { Text("Buscar") }, singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    DropdownField("Posición", listOf("Todas") + Catalogs.positions, s.positionFilter,
                        viewModel::onPositionFilter, modifier = Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = { showUserSearchDialog = true }) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Vincular por correo")
                    }
                }

                if (s.roster.isEmpty()) EmptyState("No hay jugadores en el plantel")
                else LazyColumn(
                    contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(s.roster, key = { it.id }) { p -> PlayerRow(p) { onOpenPlayer(p) } }
                }
            } else {
                if (s.requests.isEmpty()) EmptyState("No hay solicitudes pendientes")
                else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(s.requests, key = { it.id }) { p ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(p.fullName, style = MaterialTheme.typography.titleMedium)
                                Text("Nac.: ${p.birthDate} · ${p.category}", style = MaterialTheme.typography.bodySmall)
                                Text("Registrado por su ${p.parentRelation.lowercase()}", style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                    Button(onClick = { viewModel.accept(p) }, modifier = Modifier.weight(1f)) { Text("Aceptar") }
                                    OutlinedButton(onClick = { viewModel.reject(p) }, modifier = Modifier.weight(1f)) { Text("Rechazar") }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (tab == 0) {
            FloatingActionButton(onClick = onAddPlayer, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                Icon(Icons.Default.Add, "Agregar jugador")
            }
        }
    }

    if (showUserSearchDialog) {
        SearchUserDialog(
            onConfirm = { email ->
                viewModel.addRegisteredUserByEmail(email, dtUid)
                showUserSearchDialog = false
            },
            onDismiss = { showUserSearchDialog = false }
        )
    }
}

@Composable
private fun SearchUserDialog(
    onConfirm: (email: String) -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar usuario registrado al equipo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ingresa el correo electrónico del usuario (Jugador o Padre) registrado en la plataforma:")
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(email.trim()) },
                enabled = email.trim().isNotBlank()
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun PlayerRow(p: PlayerModel, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(p.shirtNumber?.toString() ?: "–", fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(p.fullName, style = MaterialTheme.typography.titleSmall)
                Text(p.position.ifBlank { "Sin posición" }, style = MaterialTheme.typography.bodySmall)
            }
            if (!p.active) StatusChip("INACTIVO")
        }
    }
}
