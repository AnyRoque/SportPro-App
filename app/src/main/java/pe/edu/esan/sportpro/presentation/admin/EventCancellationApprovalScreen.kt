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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.EventCancellationRequestModel
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.MatchRepository
import pe.edu.esan.sportpro.presentation.components.EmptyState

/**
 * Módulo de la Consola del Administrador para Revisar y Resolver Solicitudes de Anulación de Eventos enviadas por DTs (US-20).
 */
@Composable
fun EventCancellationApprovalScreen(
    adminUser: UserModel
) {
    val requestsFlow = remember { MatchRepository.observePendingCancellationRequests() }
    val pendingRequests by requestsFlow.collectAsState(initial = emptyList())

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedRequestToResolve by remember { mutableStateOf<EventCancellationRequestModel?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Solicitudes de Anulación de Eventos (${pendingRequests.size})",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (pendingRequests.isEmpty()) {
            EmptyState("No hay solicitudes pendientes de anulación de eventos de partido.", modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pendingRequests, key = { it.id }) { req ->
                    CancellationRequestCard(
                        request = req,
                        onResolve = { selectedRequestToResolve = req }
                    )
                }
            }
        }
    }

    selectedRequestToResolve?.let { req ->
        ResolveCancellationDialog(
            request = req,
            onConfirmResolve = { approve, comment ->
                scope.launch {
                    val res = MatchRepository.resolveCancellationRequest(req.id, approve, comment, adminUser)
                    res.onSuccess {
                        Toast.makeText(context, if (approve) "Evento anulado correctamente." else "Solicitud rechazada.", Toast.LENGTH_SHORT).show()
                        selectedRequestToResolve = null
                    }.onFailure { e ->
                        Toast.makeText(context, e.message ?: "Error al resolver solicitud", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDismiss = { selectedRequestToResolve = null }
        )
    }
}

@Composable
private fun CancellationRequestCard(
    request: EventCancellationRequestModel,
    onResolve: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request.matchTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "PENDIENTE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Text("Evento: ${request.eventSummary}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text("Solicitante: DT ${request.requestingDtName} (${request.requestingTeamName})", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Justificación del Reclamo:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = request.justification,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onResolve,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Revisar y Resolver")
            }
        }
    }
}

@Composable
private fun ResolveCancellationDialog(
    request: EventCancellationRequestModel,
    onConfirmResolve: (approve: Boolean, adminComment: String) -> Unit,
    onDismiss: () -> Unit
) {
    var adminComment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resolver Solicitud de Anulación") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Partido: ${request.matchTitle}")
                Text("Evento a anular: ${request.eventSummary}")
                Text("Justificación del DT: ${request.justification}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = adminComment,
                    onValueChange = { adminComment = it },
                    label = { Text("Comentario de resolución del ADM (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onConfirmResolve(false, adminComment) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Rechazar")
                }
                Button(
                    onClick = { onConfirmResolve(true, adminComment) }
                ) {
                    Text("Aceptar y Anular")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
