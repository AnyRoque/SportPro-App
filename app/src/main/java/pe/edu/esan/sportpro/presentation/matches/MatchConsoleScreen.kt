package pe.edu.esan.sportpro.presentation.matches

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.edu.esan.sportpro.R
import pe.edu.esan.sportpro.data.model.EventStatus
import pe.edu.esan.sportpro.data.model.EventTeam
import pe.edu.esan.sportpro.data.model.EventType
import pe.edu.esan.sportpro.data.model.MatchEventModel
import pe.edu.esan.sportpro.data.model.MatchModel
import pe.edu.esan.sportpro.data.model.MatchStatus
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.presentation.components.DropdownField
import pe.edu.esan.sportpro.presentation.components.EmptyState
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.presentation.components.SectionTitle
import pe.edu.esan.sportpro.ui.theme.StatusApproved
import pe.edu.esan.sportpro.ui.theme.StatusPending
import pe.edu.esan.sportpro.ui.theme.StatusRejected
import java.util.Locale

/**
 * Pantalla Consola de Registro de Eventos en Tiempo Real con Edición, Anulación y Tiempo Extra (US-19 / US-20).
 */
@Composable
fun MatchConsoleScreen(
    user: UserModel,
    viewModel: MatchConsoleViewModel,
    matchId: String,
    onNavigateLineup: () -> Unit,
    onBack: () -> Unit
) {
    val match by viewModel.match.collectAsState()
    val events by viewModel.events.collectAsState()
    val pitchPlayers by viewModel.pitchPlayers.collectAsState()
    val elapsedSec by viewModel.elapsedSeconds.collectAsState()
    val calculatedMin by viewModel.currentMinute.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val infoMsg by viewModel.infoMessage.collectAsState()
    val noLineupErr by viewModel.noLineupError.collectAsState()
    val accessDeniedErr by viewModel.accessDenied.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedEventTypeToRegister by remember { mutableStateOf<String?>(null) }
    var eventToEdit by remember { mutableStateOf<MatchEventModel?>(null) }
    var eventToCancel by remember { mutableStateOf<MatchEventModel?>(null) }
    var eventToDelete by remember { mutableStateOf<MatchEventModel?>(null) }
    var eventToRequestAdminCancel by remember { mutableStateOf<MatchEventModel?>(null) }
    var eventToShowHistory by remember { mutableStateOf<MatchEventModel?>(null) }

    val canEditEvents = viewModel.canEditEvents()

    LaunchedEffect(matchId) {
        viewModel.loadConsole(matchId, user)
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(infoMsg) {
        infoMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearInfoMessage()
        }
    }

    if (isLoading) {
        LoadingBox()
        return
    }

    // Validación 1: Acceso Denegado
    if (accessDeniedErr) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.console_access_denied),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack) {
                Text("Volver")
            }
        }
        return
    }

    // Validación 2: Falta Alineación Táctica Guardada
    if (noLineupErr) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.console_no_lineup_error),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateLineup) {
                Text("Ir a Alineación Táctica")
            }
        }
        return
    }

    val m = match ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Marcador Oficial y Cronómetro de Partido (Hora, Minuto y Segundo)
        MatchTimerHeader(
            user = user,
            viewModel = viewModel,
            match = m,
            elapsedSeconds = elapsedSec,
            calculatedMinute = calculatedMin,
            onStartMatch = { viewModel.startMatch(user) },
            onPauseTimer = { viewModel.pauseTimer() },
            onResumeTimer = { viewModel.resumeTimer() },
            onEndFirstHalf = { viewModel.endFirstHalf(user) },
            onStartSecondHalf = { viewModel.startSecondHalf(user) },
            onFinishMatch = { viewModel.finishMatch(user) }
        )

        // Botones de Evento Rápido con Iconos y Texto Completo
        if (m.status == MatchStatus.IN_PROGRESS) {
            SectionTitle(stringResource(R.string.console_events_title))
            QuickEventButtons(
                onEventSelected = { type -> selectedEventTypeToRegister = type }
            )
        }

        // Línea de Tiempo del Partido (Cronología en Vivo)
        SectionTitle(stringResource(R.string.console_timeline_title) + " (${events.count { it.status == EventStatus.ACTIVE }})")

        if (events.isEmpty()) {
            EmptyState("Aún no hay eventos registrados en este partido.", modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    TimelineEventCard(
                        user = user,
                        match = m,
                        event = event,
                        canEdit = canEditEvents,
                        onEdit = { eventToEdit = event },
                        onCancel = { eventToCancel = event },
                        onDelete = { eventToDelete = event },
                        onRequestAdminCancel = { eventToRequestAdminCancel = event },
                        onShowHistory = { eventToShowHistory = event }
                    )
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState)
    }

    // Diálogo 1: Registro de Evento Nuevo (US-19)
    selectedEventTypeToRegister?.let { eventType ->
        RegisterEventDialog(
            eventType = eventType,
            currentMinute = calculatedMin,
            pitchPlayers = pitchPlayers,
            rivalName = m.rival,
            onConfirm = { team, minute, manualMin, playerId, playerName, isUnidentified, saleId, saleName, entraId, entraName, obs ->
                viewModel.registerEvent(
                    user = user,
                    type = eventType,
                    team = team,
                    minute = minute,
                    manualMinute = manualMin,
                    playerId = playerId,
                    playerName = playerName,
                    isUnidentified = isUnidentified,
                    jugadorSaleId = saleId,
                    jugadorSaleName = saleName,
                    jugadorEntraId = entraId,
                    jugadorEntraName = entraName,
                    observation = obs,
                    onDone = { selectedEventTypeToRegister = null }
                )
            },
            onDismiss = { selectedEventTypeToRegister = null }
        )
    }

    // Diálogo 2: Edición de Evento Propio con Motivo Obligatorio (US-20)
    eventToEdit?.let { event ->
        EditEventDialog(
            event = event,
            pitchPlayers = pitchPlayers,
            rivalName = m.rival,
            onConfirm = { updatedEvent, motivo ->
                viewModel.editEvent(user, updatedEvent, motivo) {
                    eventToEdit = null
                }
            },
            onDismiss = { eventToEdit = null }
        )
    }

    // Diálogo 3: Anulación Directa de Evento Propio con Motivo Obligatorio (US-20)
    eventToCancel?.let { event ->
        CancelEventDialog(
            event = event,
            onConfirm = { motivo ->
                viewModel.cancelEvent(user, event.id, motivo) {
                    eventToCancel = null
                }
            },
            onDismiss = { eventToCancel = null }
        )
    }

    // Diálogo 4: Eliminación Definitiva de Evento Anulado Propio
    eventToDelete?.let { event ->
        DeleteEventConfirmDialog(
            event = event,
            onConfirm = {
                viewModel.deleteEvent(user, event.id) {
                    eventToDelete = null
                }
            },
            onDismiss = { eventToDelete = null }
        )
    }

    // Diálogo 5: Solicitud de Anulación de Evento Rival enviada al Administrador (US-20)
    eventToRequestAdminCancel?.let { event ->
        RequestCancellationDialog(
            event = event,
            onConfirm = { justification ->
                viewModel.requestEventCancellationToAdmin(user, event, justification) {
                    eventToRequestAdminCancel = null
                }
            },
            onDismiss = { eventToRequestAdminCancel = null }
        )
    }

    // Diálogo 6: Historial de Cambios / Trazabilidad de Auditoría (US-20)
    eventToShowHistory?.let { event ->
        EventHistoryDialog(
            viewModel = viewModel,
            event = event,
            onDismiss = { eventToShowHistory = null }
        )
    }
}

@Composable
private fun MatchTimerHeader(
    user: UserModel,
    viewModel: MatchConsoleViewModel,
    match: MatchModel,
    elapsedSeconds: Long,
    calculatedMinute: Int,
    onStartMatch: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onEndFirstHalf: () -> Unit,
    onStartSecondHalf: () -> Unit,
    onFinishMatch: () -> Unit
) {
    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60

    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

    val periodText = when (match.currentPeriod) {
        1 -> "1° Tiempo"
        2 -> "2° Tiempo"
        4 -> "Tiempo Extra"
        3 -> "Finalizado"
        else -> "Por Iniciar"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${match.teamName}  vs  ${match.rival}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${match.ownScore}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = periodText,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = timeFormatted,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = "Minuto $calculatedMinute' · ${seconds}s",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "${match.rivalScore}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botones de Control del Cronómetro (Funcionan para los DTs de ambos equipos)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (match.status == MatchStatus.PROGRAMMED) {
                    Button(
                        onClick = onStartMatch,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.console_start_match), fontWeight = FontWeight.Bold)
                    }
                } else if (match.status == MatchStatus.IN_PROGRESS) {
                    if (match.currentPeriod == 1) {
                        if (match.timerIsRunning) {
                            OutlinedButton(onClick = onPauseTimer, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.console_pause))
                            }
                            Button(onClick = onEndFirstHalf, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.console_end_first_half))
                            }
                        } else {
                            // Una vez finalizado el 1° Tiempo, cambia a "Iniciar 2° Tiempo"
                            Button(
                                onClick = onStartSecondHalf,
                                colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.console_start_second_half), fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (match.currentPeriod == 2) {
                        if (match.timerIsRunning) {
                            OutlinedButton(onClick = onPauseTimer, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.console_pause))
                            }
                            Button(
                                onClick = onFinishMatch,
                                colors = ButtonDefaults.buttonColors(containerColor = StatusRejected),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.console_finish_match))
                            }
                        } else if (match.ownScore == match.rivalScore) {
                            // En caso de empate al cumplir los 90 min / 2° tiempo -> Ofrecer Iniciar Tiempo Extra
                            Button(
                                onClick = { viewModel.startExtraTime(user) },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Iniciar Tiempo Extra", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onFinishMatch,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.console_finish_match))
                            }
                        } else {
                            Button(onClick = onResumeTimer, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.console_resume))
                            }
                            Button(
                                onClick = onFinishMatch,
                                colors = ButtonDefaults.buttonColors(containerColor = StatusRejected),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.console_finish_match))
                            }
                        }
                    } else if (match.currentPeriod == 4) {
                        // Tiempo Extra en marcha
                        if (match.timerIsRunning) {
                            OutlinedButton(onClick = onPauseTimer, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.console_pause))
                            }
                        } else {
                            Button(onClick = onResumeTimer, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.console_resume))
                            }
                        }
                        Button(
                            onClick = onFinishMatch,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRejected),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.console_finish_match))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickEventButtons(
    onEventSelected: (type: String) -> Unit
) {
    val quickEvents = listOf(
        EventType.GOAL to "⚽ Gol Marcado",
        EventType.YELLOW_CARD to "🟨 Tarjeta Amarilla",
        EventType.RED_CARD to "🟥 Tarjeta Roja",
        EventType.SUBSTITUTION to "🔄 Cambio de Jugador",
        EventType.FOUL to "⚠️ Falta Cometida",
        EventType.SHOT_ON_TARGET to "🎯 Tiro al Arco",
        EventType.SHOT_OFF_TARGET to "↗️ Tiro Desviado",
        EventType.CORNER to "🚩 Tiro de Esquina",
        EventType.OFFSIDE to "🚩 Fuera de Juego"
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            quickEvents.take(3).forEach { (type, label) ->
                Button(
                    onClick = { onEventSelected(type) },
                    modifier = Modifier.weight(1f),
                    contentPadding = ButtonDefaults.ContentPadding
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            quickEvents.drop(3).take(3).forEach { (type, label) ->
                OutlinedButton(
                    onClick = { onEventSelected(type) },
                    modifier = Modifier.weight(1f),
                    contentPadding = ButtonDefaults.ContentPadding
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            quickEvents.drop(6).take(3).forEach { (type, label) ->
                OutlinedButton(
                    onClick = { onEventSelected(type) },
                    modifier = Modifier.weight(1f),
                    contentPadding = ButtonDefaults.ContentPadding
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun TimelineEventCard(
    user: UserModel,
    match: MatchModel,
    event: MatchEventModel,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onRequestAdminCancel: () -> Unit,
    onShowHistory: () -> Unit
) {
    val isCanceled = event.status == EventStatus.CANCELED
    val textDecoration = if (isCanceled) TextDecoration.LineThrough else TextDecoration.None

    // Verificar si el evento fue registrado por el propio equipo o usuario
    val isOwnEvent = (event.registeredBy == user.uid || (event.team == EventTeam.OWN && user.uid == match.dtUid))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCanceled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else if (event.type == EventType.GOAL) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isCanceled) Color.Gray else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${event.minute}'",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = EventType.labelOf(event.type),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textDecoration = textDecoration,
                            color = if (isCanceled) Color.Gray else MaterialTheme.colorScheme.onSurface
                        )
                        if (isCanceled) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(color = MaterialTheme.colorScheme.error, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "ANULADO",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        } else if (event.minutoCorregido) {
                            Text(
                                text = " (${stringResource(R.string.console_corrected_minute)})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    if (event.type == EventType.SUBSTITUTION) {
                        Text(
                            text = "Sustitución: Sale ${event.jugadorSaleName} ➔ Entra ${event.jugadorEntraName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = textDecoration
                        )
                    } else if (event.playerName.isNotBlank()) {
                        Text(
                            text = "Jugador involucrado: ${event.playerName}",
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = textDecoration
                        )
                    }

                    if (event.observation.isNotBlank()) {
                        Text(
                            text = "Nota: ${event.observation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (event.pendingSync) {
                        Text(
                            text = "⏳ " + stringResource(R.string.console_pending_sync),
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusPending,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    color = if (event.team == EventTeam.OWN) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (event.team == EventTeam.OWN) "Propio" else "Rival",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (event.team == EventTeam.OWN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Opciones de Edición, Anulación, Eliminación e Historial (US-20)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShowHistory, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Ver Historial", modifier = Modifier.size(18.dp))
                }

                if (canEdit) {
                    if (isOwnEvent) {
                        if (!isCanceled) {
                            // Evento propio activo -> Editar o Anular
                            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                            }
                            TextButton(onClick = onCancel) {
                                Text("Anular", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            // Evento propio anulado -> Opcion de ELIMINAR permanentemente
                            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    } else if (!isCanceled) {
                        // Evento del equipo rival -> No se puede anular directamente, solicitar a ADM
                        TextButton(onClick = onRequestAdminCancel) {
                            Text("Solicitar Anulación a ADM", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegisterEventDialog(
    eventType: String,
    currentMinute: Int,
    pitchPlayers: List<PlayerPitchState>,
    rivalName: String,
    onConfirm: (
        team: String,
        minute: Int,
        manualMin: Boolean,
        playerId: String?,
        playerName: String,
        isUnidentified: Boolean,
        saleId: String?,
        saleName: String,
        entraId: String?,
        entraName: String,
        observation: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTeam by remember { mutableStateOf(EventTeam.OWN) }
    var minuteText by remember { mutableStateOf(currentMinute.toString()) }
    var isUnidentified by remember { mutableStateOf(false) }

    val onPitchList = pitchPlayers.filter { it.isOnPitch && !it.isExpelled }
    val onBenchList = pitchPlayers.filter { !it.isOnPitch && !it.isExpelled }

    var selectedPlayer by remember { mutableStateOf(onPitchList.firstOrNull()) }
    var selectedSalePlayer by remember { mutableStateOf(onPitchList.firstOrNull()) }
    var selectedEntraPlayer by remember { mutableStateOf(onBenchList.firstOrNull()) }

    var rivalPlayerText by remember { mutableStateOf("") }
    var observation by remember { mutableStateOf("") }

    val calculatedMinVal = currentMinute
    val inputMinVal = minuteText.toIntOrNull() ?: currentMinute
    val isMinuteCorrected = inputMinVal != calculatedMinVal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar " + EventType.labelOf(eventType)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Equipo involucrado:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedTeam == EventTeam.OWN,
                        onClick = { selectedTeam = EventTeam.OWN },
                        label = { Text("Propio") }
                    )
                    FilterChip(
                        selected = selectedTeam == EventTeam.RIVAL,
                        onClick = { selectedTeam = EventTeam.RIVAL },
                        label = { Text("Rival ($rivalName)") }
                    )
                }

                OutlinedTextField(
                    value = minuteText,
                    onValueChange = { minuteText = it.filter { c -> c.isDigit() } },
                    label = { Text("Minuto del evento") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (eventType == EventType.SUBSTITUTION) {
                    Text("Jugador que sale (En campo):", style = MaterialTheme.typography.labelMedium)
                    val saleNames = onPitchList.map { "${it.playerName} (${it.positionName})" }
                    val selectedSaleLabel = selectedSalePlayer?.let { "${it.playerName} (${it.positionName})" } ?: saleNames.firstOrNull() ?: "Sin jugadores en campo"

                    if (onPitchList.isNotEmpty()) {
                        DropdownField(
                            label = "Jugador Sale",
                            options = saleNames,
                            selected = selectedSaleLabel,
                            onSelected = { label ->
                                selectedSalePlayer = onPitchList.find { "${it.playerName} (${it.positionName})" == label }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text("Jugador que entra (Suplente):", style = MaterialTheme.typography.labelMedium)
                    val entraNames = onBenchList.map { "${it.playerName} (${it.positionName})" }
                    val selectedEntraLabel = selectedEntraPlayer?.let { "${it.playerName} (${it.positionName})" } ?: entraNames.firstOrNull() ?: "Sin suplentes"

                    if (onBenchList.isNotEmpty()) {
                        DropdownField(
                            label = "Jugador Entra",
                            options = entraNames,
                            selected = selectedEntraLabel,
                            onSelected = { label ->
                                selectedEntraPlayer = onBenchList.find { "${it.playerName} (${it.positionName})" == label }
                            }
                        )
                    } else {
                        Text("No hay suplentes disponibles en la banca.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                } else if (selectedTeam == EventTeam.OWN) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isUnidentified,
                            onCheckedChange = { isUnidentified = it }
                        )
                        Text("Jugador no identificado", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (!isUnidentified) {
                        val playerNames = onPitchList.map { "${it.playerName} (${it.positionName})" }
                        val selectedPlayerLabel = selectedPlayer?.let { "${it.playerName} (${it.positionName})" } ?: playerNames.firstOrNull() ?: ""

                        if (onPitchList.isNotEmpty()) {
                            DropdownField(
                                label = "Jugador de la alineación",
                                options = playerNames,
                                selected = selectedPlayerLabel,
                                onSelected = { label ->
                                    selectedPlayer = onPitchList.find { "${it.playerName} (${it.positionName})" == label }
                                }
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = rivalPlayerText,
                        onValueChange = { rivalPlayerText = it },
                        label = { Text("Jugador / N° Dorsal rival (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = observation,
                    onValueChange = { observation = it },
                    label = { Text("Observación (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalMin = minuteText.toIntOrNull() ?: currentMinute
                    if (eventType == EventType.SUBSTITUTION) {
                        onConfirm(
                            selectedTeam,
                            finalMin,
                            isMinuteCorrected,
                            null,
                            "",
                            false,
                            selectedSalePlayer?.playerId,
                            selectedSalePlayer?.playerName ?: "",
                            selectedEntraPlayer?.playerId,
                            selectedEntraPlayer?.playerName ?: "",
                            observation
                        )
                    } else if (selectedTeam == EventTeam.OWN) {
                        onConfirm(
                            selectedTeam,
                            finalMin,
                            isMinuteCorrected,
                            if (isUnidentified) null else selectedPlayer?.playerId,
                            if (isUnidentified) "Jugador no identificado" else selectedPlayer?.playerName ?: "",
                            isUnidentified,
                            null,
                            "",
                            null,
                            "",
                            observation
                        )
                    } else {
                        onConfirm(
                            selectedTeam,
                            finalMin,
                            isMinuteCorrected,
                            null,
                            rivalPlayerText.ifBlank { "Jugador Rival" },
                            rivalPlayerText.isBlank(),
                            null,
                            "",
                            null,
                            "",
                            observation
                        )
                    }
                }
            ) {
                Text("Confirmar Evento")
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
private fun EditEventDialog(
    event: MatchEventModel,
    pitchPlayers: List<PlayerPitchState>,
    rivalName: String,
    onConfirm: (updatedEvent: MatchEventModel, motivo: String) -> Unit,
    onDismiss: () -> Unit
) {
    var minuteText by remember { mutableStateOf(event.minute.toString()) }
    var observation by remember { mutableStateOf(event.observation) }
    var motivo by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Evento (${EventType.labelOf(event.type)})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = minuteText,
                    onValueChange = { minuteText = it.filter { c -> c.isDigit() } },
                    label = { Text("Minuto del evento") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = observation,
                    onValueChange = { observation = it },
                    label = { Text("Observación") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo de la modificación (Obligatorio)") },
                    isError = motivo.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (motivo.isBlank()) {
                        errorMsg = "Debes ingresar el motivo de la modificación"
                    } else {
                        val min = minuteText.toIntOrNull() ?: event.minute
                        val updated = event.copy(
                            minute = min,
                            observation = observation.trim(),
                            minutoCorregido = true
                        )
                        onConfirm(updated, motivo)
                    }
                }
            ) {
                Text("Guardar Edición")
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
private fun CancelEventDialog(
    event: MatchEventModel,
    onConfirm: (motivo: String) -> Unit,
    onDismiss: () -> Unit
) {
    var motivo by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Anular Evento (${EventType.labelOf(event.type)})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("¿Estás seguro de anular este evento? El evento quedará marcado como 'ANULADO' y se recalcularán el marcador y estadísticas.")

                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo de la anulación (Obligatorio)") },
                    isError = motivo.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (motivo.isBlank()) {
                        errorMsg = "Debes ingresar el motivo de la anulación"
                    } else {
                        onConfirm(motivo)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Confirmar Anulación")
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
private fun DeleteEventConfirmDialog(
    event: MatchEventModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar Evento Anulado") },
        text = {
            Text("¿Estás seguro de eliminar permanentemente este evento anulado (${EventType.labelOf(event.type)} - Min ${event.minute}')? Esta acción eliminará el registro de la línea de tiempo definitivamente.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Eliminar Definitivamente")
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
private fun RequestCancellationDialog(
    event: MatchEventModel,
    onConfirm: (justification: String) -> Unit,
    onDismiss: () -> Unit
) {
    var justification by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Solicitar Anulación al ADM") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Este evento fue registrado por el equipo rival (${EventType.labelOf(event.type)} - ${event.playerName}).")
                Text("Para anular o corregir esta sanción/evento registrado por el rival, debes ingresar el motivo / justificación detallada para el Administrador:")

                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = justification,
                    onValueChange = { justification = it },
                    label = { Text("Justificación del reclamo (Obligatorio)") },
                    isError = justification.isBlank(),
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (justification.isBlank()) {
                        errorMsg = "Debes ingresar una justificación para el Administrador"
                    } else {
                        onConfirm(justification)
                    }
                }
            ) {
                Text("Enviar Solicitud a ADM")
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
private fun EventHistoryDialog(
    viewModel: MatchConsoleViewModel,
    event: MatchEventModel,
    onDismiss: () -> Unit
) {
    val historyFlow = remember(event.id) {
        viewModel.observeEventHistory(event.id)
    }
    val auditList by historyFlow.collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Historial de Cambios / Bitácora") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Evento: ${EventType.labelOf(event.type)} (${event.minute}')",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )

                if (auditList.isEmpty()) {
                    Text("No hay modificaciones registradas para este evento (valor original).")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(auditList, key = { it.id }) { audit ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = audit.tipoCambio,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = audit.usuarioNombre,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Motivo: ${audit.motivo}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text("Anterior: ${audit.valorOriginal}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text("Nuevo: ${audit.valorNuevo}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
