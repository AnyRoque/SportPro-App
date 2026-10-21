package pe.edu.esan.sportpro.presentation.teams

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.TeamStatus
import pe.edu.esan.sportpro.presentation.components.EmptyState
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.presentation.components.StatusChip

/** US-03: "Mis equipos" con su estado (Pendiente / Aprobado / Rechazado). */
@Composable
fun MyTeamsScreen(
    dtUid: String,
    onNewTeam: () -> Unit,
    onOpenRoster: (TeamModel) -> Unit,
    onFixTeam: (TeamModel) -> Unit,
    viewModel: MyTeamsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(dtUid) { viewModel.start(dtUid) }

    Box(Modifier.fillMaxSize()) {
        when {
            state.loading -> LoadingBox()
            state.error != null -> EmptyState("Error: ${state.error}")
            state.teams.isEmpty() -> EmptyState("Aún no registras equipos.\nPresiona “Nuevo equipo” para empezar.")
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.teams, key = { it.id }) { team ->
                    TeamCard(team, onOpenRoster, onFixTeam)
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onNewTeam,
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text("Nuevo equipo") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }
}

@Composable
private fun TeamCard(team: TeamModel, onOpenRoster: (TeamModel) -> Unit, onFixTeam: (TeamModel) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = team.status == TeamStatus.APPROVED) { onOpenRoster(team) }
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(team.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                StatusChip(team.status)
            }
            Text("${team.category} · ${team.district}", style = MaterialTheme.typography.bodyMedium)
            Text("Campo: ${team.field}", style = MaterialTheme.typography.bodySmall)
            if (team.trainingDays.isNotEmpty())
                Text("Entrena: ${team.trainingDays.joinToString(", ")} ${team.trainingTime}", style = MaterialTheme.typography.bodySmall)

            when (team.status) {
                TeamStatus.PENDING -> Text("En revisión por el administrador. Aún no puedes agregar jugadores.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                TeamStatus.APPROVED -> Text("Toca para ver el plantel", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
                TeamStatus.REJECTED -> {
                    Text("Motivo: ${team.rejectReason}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = { onFixTeam(team) }) { Text("Corregir y reenviar") }
                }
            }
        }
    }
}
