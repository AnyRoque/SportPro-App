package pe.edu.esan.sportpro.presentation.teams

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.TeamStatus
import pe.edu.esan.sportpro.presentation.components.EmptyState
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.presentation.components.ReasonDialog
import pe.edu.esan.sportpro.presentation.components.StatusChip

private val tabs = listOf(
    TeamStatus.PENDING to "Pendientes",
    TeamStatus.APPROVED to "Aprobados",
    TeamStatus.REJECTED to "Rechazados"
)

/** US-07: solicitudes de equipos para el ADM. */
@Composable
fun TeamApprovalScreen(adminUid: String, viewModel: TeamApprovalViewModel = viewModel()) {
    val s by viewModel.state.collectAsState()
    val context = LocalContext.current
    var rejecting by remember { mutableStateOf<TeamModel?>(null) }

    LaunchedEffect(s.message) {
        s.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabs.indexOfFirst { it.first == s.status }) {
            tabs.forEach { (status, label) ->
                Tab(selected = s.status == status, onClick = { viewModel.selectStatus(status) }, text = { Text(label) })
            }
        }
        when {
            s.loading -> LoadingBox()
            s.teams.isEmpty() -> EmptyState("No hay solicitudes en este estado")
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(s.teams, key = { it.id }) { team ->
                    ApprovalCard(
                        team = team,
                        onApprove = { viewModel.approve(team, adminUid) },
                        onReject = { rejecting = team }
                    )
                }
            }
        }
    }

    rejecting?.let { team ->
        ReasonDialog(
            title = "Rechazar “${team.name}”",
            confirmText = "Confirmar rechazo",
            minLength = 10,
            onConfirm = { reason ->
                viewModel.reject(team, adminUid, reason)
                rejecting = null
            },
            onDismiss = { rejecting = null }
        )
    }
}

@Composable
private fun ApprovalCard(team: TeamModel, onApprove: () -> Unit, onReject: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(team.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                StatusChip(team.status)
            }
            Text("Categoría: ${team.category}")
            Text("DT solicitante: ${team.dtName}")
            Text("Campo: ${team.field} · ${team.district}", style = MaterialTheme.typography.bodySmall)
            if (team.trainingDays.isNotEmpty())
                Text("Horario: ${team.trainingDays.joinToString(", ")} ${team.trainingTime}", style = MaterialTheme.typography.bodySmall)
            if (team.status == TeamStatus.REJECTED)
                Text("Motivo: ${team.rejectReason}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

            if (team.status == TeamStatus.PENDING) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    Button(onClick = onApprove, modifier = Modifier.weight(1f)) { Text("Aprobar") }
                    OutlinedButton(
                        onClick = onReject,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) { Text("Rechazar") }
                }
            }
        }
    }
}
