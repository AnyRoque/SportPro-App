package pe.edu.esan.sportpro.presentation.teams

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.data.model.Catalogs
import pe.edu.esan.sportpro.data.model.LinkStatus
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.presentation.components.DropdownField
import pe.edu.esan.sportpro.presentation.components.EmptyState
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.presentation.components.StatusChip

/**
 * Pantalla para explorar equipos aprobados y enviar solicitud de ingreso al DT (US-02 / US-04).
 */
@Composable
fun JoinTeamScreen(
    user: UserModel,
    viewModel: JoinTeamViewModel,
    onBack: () -> Unit
) {
    val approvedTeams by viewModel.approvedTeams.collectAsState()
    val userRequests by viewModel.userRequests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    val query by viewModel.query.collectAsState()

    val context = LocalContext.current
    var selectedTeamToJoin by remember { mutableStateOf<TeamModel?>(null) }

    LaunchedEffect(user.uid) {
        viewModel.loadApprovedTeamsAndUserRequests(user)
    }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    if (isLoading) {
        LoadingBox()
    } else {
        val filteredTeams = approvedTeams.filter {
            query.isBlank() || it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.query.value = it },
                label = { Text("Buscar equipo por nombre o categoría") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (filteredTeams.isEmpty()) {
                EmptyState("No hay equipos disponibles para unirse.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTeams, key = { it.id }) { team ->
                        val existingRequest = userRequests.firstOrNull { it.teamId == team.id }
                        JoinTeamCard(
                            team = team,
                            existingRequestStatus = existingRequest?.linkStatus,
                            onRequestJoin = { selectedTeamToJoin = team }
                        )
                    }
                }
            }
        }
    }

    selectedTeamToJoin?.let { team ->
        RequestJoinDialog(
            team = team,
            onConfirm = { position ->
                viewModel.requestToJoinTeam(user, team, position) {
                    selectedTeamToJoin = null
                }
            },
            onDismiss = { selectedTeamToJoin = null }
        )
    }
}

@Composable
private fun JoinTeamCard(
    team: TeamModel,
    existingRequestStatus: String?,
    onRequestJoin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = team.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                existingRequestStatus?.let { status ->
                    StatusChip(status)
                }
            }

            Text("Categoría: ${team.category} · Distrito: ${team.district}", style = MaterialTheme.typography.bodyMedium)
            Text("Cancha: ${team.field}", style = MaterialTheme.typography.bodySmall)

            if (team.trainingDays.isNotEmpty()) {
                Text(
                    text = "Días de entrenamiento: ${team.trainingDays.joinToString(", ")} ${team.trainingTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (existingRequestStatus == null || existingRequestStatus == LinkStatus.REJECTED) {
                Button(
                    onClick = onRequestJoin,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Solicitar Unirme")
                }
            }
        }
    }
}

@Composable
private fun RequestJoinDialog(
    team: TeamModel,
    onConfirm: (position: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPosition by remember { mutableStateOf(Catalogs.positions.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Solicitud de Ingreso") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enviarás una solicitud al Director Técnico para unirte al equipo ${team.name} (${team.category}).")
                DropdownField(
                    label = "Posición de juego preferida",
                    options = Catalogs.positions,
                    selected = selectedPosition,
                    onSelected = { selectedPosition = it }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedPosition) }) {
                Text("Enviar Solicitud")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
