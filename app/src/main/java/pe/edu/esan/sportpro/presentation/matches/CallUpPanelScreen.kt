package pe.edu.esan.sportpro.presentation.matches

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.R
import pe.edu.esan.sportpro.data.model.CallUpItemModel
import pe.edu.esan.sportpro.data.model.CallUpStatus
import pe.edu.esan.sportpro.data.model.MatchStatus
import pe.edu.esan.sportpro.data.model.RejectReason
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.TacticalFormations
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.presentation.components.DropdownField
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.ui.theme.StatusApproved
import pe.edu.esan.sportpro.ui.theme.StatusPending
import pe.edu.esan.sportpro.ui.theme.StatusRejected

/**
 * Panel de Convocatoria en tiempo real con opción de Asistir o No Asistir independiente para cada jugador o apoderado (US-17).
 */
@Composable
fun CallUpPanelScreen(
    user: UserModel,
    viewModel: CallUpViewModel,
    matchId: String,
    onNavigateAddPlayers: () -> Unit,
    onNavigateLineup: () -> Unit = {},
    onBack: () -> Unit
) {
    val match by viewModel.match.collectAsState()
    val callUpItems by viewModel.callUpItems.collectAsState()
    val userPlayerId by viewModel.userPlayerId.collectAsState()
    val userChildrenIds by viewModel.userChildrenIds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val isDt = user.roleEnum == Role.DT
    val isParent = user.roleEnum == Role.PAD
    val snackbarHostState = remember { SnackbarHostState() }

    var rejectPlayerId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(matchId, user.uid) {
        viewModel.loadMatchAndPlayers(matchId, user.uid)
        viewModel.loadUserProfiles(user)
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (isLoading || match == null) {
        LoadingBox()
        return
    }

    val m = match!!
    val isMatchProgrammed = m.status == MatchStatus.PROGRAMMED

    // Validación ESTRICTA e independiente de propiedad del ítem de convocatoria
    fun isUserItem(item: CallUpItemModel): Boolean {
        if (isDt) return false
        if (user.uid.isNotBlank() && (user.uid == item.userUid || user.uid == item.parentUid)) return true
        if (userPlayerId != null && item.playerId == userPlayerId) return true
        if (userChildrenIds.contains(item.playerId)) return true
        return false
    }

    // Priorizar en la lista los ítems pendientes por responder del usuario o apoderado mientras el partido esté programado
    val sortedCallUpItems = remember(callUpItems, user.uid, userPlayerId, userChildrenIds, isMatchProgrammed) {
        callUpItems.sortedByDescending { item ->
            isUserItem(item) && item.responseStatus == CallUpStatus.PENDING && (isMatchProgrammed || !item.isExpired)
        }
    }

    val userPendingItem = sortedCallUpItems.firstOrNull { item ->
        isUserItem(item) && item.responseStatus == CallUpStatus.PENDING && (isMatchProgrammed || !item.isExpired)
    }

    // Contadores en tiempo real
    val confirmedCount = callUpItems.count { it.computedStatus == CallUpStatus.CONFIRMED }
    val rejectedCount = callUpItems.count { it.computedStatus == CallUpStatus.REJECTED }
    val pendingCount = callUpItems.count { it.computedStatus == CallUpStatus.PENDING }
    val noResponseCount = callUpItems.count { it.computedStatus == CallUpStatus.NO_RESPONSE }
    val startersCount = callUpItems.count { it.isStarter }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${m.teamName} vs ${m.rival}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Límite de respuesta: ${m.callUpDeadlineDate} ${m.callUpDeadlineTime} hs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Button(onClick = onNavigateLineup) {
                Text(stringResource(R.string.lineup_title))
            }
        }

        // Aviso destacado si el jugador o apoderado tiene una respuesta pendiente
        if (userPendingItem != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(
                        text = if (isParent) "Tienes una convocatoria pendiente para confirmar o rechazar la asistencia de tu hijo(a)."
                        else "Tienes una convocatoria pendiente. Confirma o rechaza tu asistencia al partido.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        // Panel de contadores para DT y miembros
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CountCard(label = stringResource(R.string.callup_count_confirmed), count = confirmedCount, color = MaterialTheme.colorScheme.primary)
            CountCard(label = stringResource(R.string.callup_count_rejected), count = rejectedCount, color = MaterialTheme.colorScheme.error)
            CountCard(label = stringResource(R.string.callup_count_pending), count = pendingCount, color = MaterialTheme.colorScheme.tertiary)
            CountCard(label = stringResource(R.string.callup_count_no_response), count = noResponseCount, color = MaterialTheme.colorScheme.outline)
        }

        // Selección de Formación Táctica y Alineación
        val formations = TacticalFormations.forModality(m.modality)
        val currentFormation = m.formation.ifBlank { formations.first() }

        if (isDt) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DropdownField(
                    label = "Formación Táctica (Fútbol ${m.modality})",
                    options = formations,
                    selected = currentFormation,
                    onSelected = { newFormation ->
                        viewModel.updateFormation(m.id, newFormation)
                    },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Titulares: $startersCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.resendReminders {} },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(stringResource(R.string.callup_button_resend_reminders))
                }
                Button(
                    onClick = onNavigateAddPlayers,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(stringResource(R.string.callup_button_add_players))
                }
            }
        } else if (currentFormation.isNotBlank()) {
            Text(
                text = "Formación táctica: $currentFormation | Titulares: $startersCount",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sortedCallUpItems, key = { it.id }) { item ->
                // OPCIÓN DE ASISTIR O NO ASISTIR (Exclusiva e independiente para el jugador o su apoderado)
                val canRespond = isUserItem(item) && item.responseStatus == CallUpStatus.PENDING && (isMatchProgrammed || !item.isExpired)

                CallUpItemCard(
                    item = item,
                    isDt = isDt,
                    isParent = isParent,
                    canRespond = canRespond,
                    onToggleStarter = {
                        viewModel.toggleStarterStatus(m.id, item.playerId, item.isStarter)
                    },
                    onConfirm = {
                        viewModel.respondCallUp(matchId, item.playerId, CallUpStatus.CONFIRMED) {}
                    },
                    onReject = {
                        rejectPlayerId = item.playerId
                    }
                )
            }
        }

        SnackbarHost(hostState = snackbarHostState)
    }

    rejectPlayerId?.let { playerId ->
        RejectDialog(
            onConfirm = { reason, comment ->
                viewModel.respondCallUp(matchId, playerId, CallUpStatus.REJECTED, reason, comment) {
                    rejectPlayerId = null
                }
            },
            onDismiss = { rejectPlayerId = null }
        )
    }
}

@Composable
private fun CountCard(label: String, count: Int, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        modifier = Modifier.padding(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
private fun CallUpStatusBadge(status: String) {
    val (label, color) = when (status) {
        CallUpStatus.CONFIRMED -> "DENTRO DE LA CONVOCATORIA" to StatusApproved
        CallUpStatus.REJECTED -> "FUERA DE LA CONVOCATORIA" to StatusRejected
        CallUpStatus.NO_RESPONSE -> "SIN RESPUESTA (VENCIDO)" to Color.Gray
        else -> "PENDIENTE DE RESPUESTA" to StatusPending
    }
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
        Text(
            text = label,
            color = color,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CallUpItemCard(
    item: CallUpItemModel,
    isDt: Boolean,
    isParent: Boolean,
    canRespond: Boolean,
    onToggleStarter: () -> Unit,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = item.playerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (item.isStarter) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = " (TITULAR)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(text = "Posición: ${item.position.ifBlank { "Sin definir" }}", style = MaterialTheme.typography.bodySmall)
                }

                CallUpStatusBadge(item.computedStatus)
            }

            if (item.computedStatus == CallUpStatus.REJECTED && item.rejectReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Motivo de inasistencia: ${item.rejectReason}" + if (item.rejectComment.isNotBlank()) " (${item.rejectComment})" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (isDt) {
                Spacer(modifier = Modifier.height(4.dp))
                FilterChip(
                    selected = item.isStarter,
                    onClick = onToggleStarter,
                    label = { Text(if (item.isStarter) "Alineación: Titular" else "Alineación: Suplente") }
                )
            }

            // OPCIONES EXCLUSIVAS PARA EL JUGADOR O SU APODERADO
            if (canRespond) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onReject, modifier = Modifier.padding(end = 8.dp)) {
                        Text(if (isParent) "No Asistirá" else "No Asistir")
                    }
                    Button(onClick = onConfirm) {
                        Text(if (isParent) "Sí, Asistirá" else "Sí, Asistiré")
                    }
                }
            } else if (item.isExpired && item.responseStatus == CallUpStatus.PENDING) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.callup_expired_notice),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun RejectDialog(
    onConfirm: (reason: String, comment: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedReason by remember { mutableStateOf(RejectReason.INJURY) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.callup_reject_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.callup_reject_reason_label), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    RejectReason.ALL.forEach { reason ->
                        FilterChip(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            label = { Text(reason) }
                        )
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text(stringResource(R.string.callup_reject_comment_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedReason, comment) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Confirmar Inasistencia")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
