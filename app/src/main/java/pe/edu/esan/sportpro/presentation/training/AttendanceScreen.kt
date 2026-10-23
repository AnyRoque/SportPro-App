package pe.edu.esan.sportpro.presentation.training

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.data.model.AttendanceStatus
import pe.edu.esan.sportpro.data.model.PlayerAttendanceItem
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.presentation.components.SectionTitle
import pe.edu.esan.sportpro.ui.theme.StatusApproved
import pe.edu.esan.sportpro.ui.theme.StatusPending
import pe.edu.esan.sportpro.ui.theme.StatusRejected

/**
 * Pantalla para la Toma y Registro de Asistencia a Entrenamientos (US-13).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    user: UserModel,
    viewModel: AttendanceViewModel,
    trainingId: String,
    teamId: String,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(trainingId, teamId) {
        viewModel.loadData(trainingId = trainingId, teamId = teamId, dtUid = user.uid)
    }

    LaunchedEffect(state.errorMessage, state.infoMessage, state.validationError) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.infoMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
        state.validationError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Registro de Asistencia") },
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
        if (state.loading) {
            LoadingBox()
            return@Scaffold
        }

        val training = state.training

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Cabecera con datos de la sesión
            if (training != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${training.teamName} (${training.category})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${training.date} · ${training.startTime} hs (${training.durationMinutes} min)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = training.location, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Avisos de restricción
            if (state.isFutureSession) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sesión futura: La asistencia solo puede registrarse el día del entrenamiento o posteriormente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            } else if (!state.isEditable) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Modo Lectura: Han transcurrido más de 48 horas desde la realización de esta sesión.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Resumen de Estado y Calificación
            Surface(
                color = if (state.unmarkedCount > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Resumen de Asistencia",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (state.unmarkedCount > 0) {
                            Text(
                                text = "Faltan ${state.unmarkedCount} por calificar",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "100% Calificado",
                                style = MaterialTheme.typography.labelMedium,
                                color = StatusApproved,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text("Asistieron: ${state.totalAttended}", style = MaterialTheme.typography.bodySmall, color = StatusApproved, fontWeight = FontWeight.Bold)
                        Text("Tardanzas: ${state.totalLate}", style = MaterialTheme.typography.bodySmall, color = StatusPending, fontWeight = FontWeight.Bold)
                        Text("Faltas: ${state.totalAbsent}", style = MaterialTheme.typography.bodySmall, color = StatusRejected, fontWeight = FontWeight.Bold)
                        Text("Justificados: ${state.totalJustified}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Botones de Ayuda Rápida
            if (state.isEditable) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.markAllAsAttended() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Marcar todos Asistió", style = MaterialTheme.typography.labelSmall)
                    }

                    if (state.hasLastRecord) {
                        OutlinedButton(
                            onClick = { viewModel.copyLastSessionAttendance() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copiar previa", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lista de Jugadores Activos
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.players, key = { it.id }) { player ->
                    val item = state.itemsMap[player.id] ?: PlayerAttendanceItem(
                        playerId = player.id,
                        playerName = player.fullName,
                        shirtNumber = player.shirtNumber,
                        position = player.position
                    )

                    PlayerAttendanceCard(
                        player = player,
                        item = item,
                        isEditable = state.isEditable,
                        onStatusSelected = { status ->
                            viewModel.setPlayerStatus(player.id, status, item.justificationReason)
                        },
                        onJustificationChanged = { reason ->
                            viewModel.setPlayerStatus(player.id, item.status, reason)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón de Guardar Asistencia
            if (state.isEditable) {
                Button(
                    onClick = {
                        viewModel.saveAttendance(user, onSuccess = {
                            Toast.makeText(context, "Asistencia registrada exitosamente.", Toast.LENGTH_LONG).show()
                            onBack()
                        })
                    },
                    enabled = !state.saving && state.isComplete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.saving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Guardar Asistencia", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerAttendanceCard(
    player: PlayerModel,
    item: PlayerAttendanceItem,
    isEditable: Boolean,
    onStatusSelected: (String) -> Unit,
    onJustificationChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.status.isBlank()) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = player.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Dorsal: ${player.shirtNumber ?: "-"} · Posición: ${player.position.ifBlank { "N/A" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chips / Botones de Selección de Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AttendanceChip(
                    label = "Asistió",
                    status = AttendanceStatus.ATTENDED,
                    selectedStatus = item.status,
                    enabled = isEditable,
                    onSelect = onStatusSelected,
                    modifier = Modifier.weight(1f)
                )
                AttendanceChip(
                    label = "Tardanza",
                    status = AttendanceStatus.LATE,
                    selectedStatus = item.status,
                    enabled = isEditable,
                    onSelect = onStatusSelected,
                    modifier = Modifier.weight(1f)
                )
                AttendanceChip(
                    label = "Falta",
                    status = AttendanceStatus.ABSENT,
                    selectedStatus = item.status,
                    enabled = isEditable,
                    onSelect = onStatusSelected,
                    modifier = Modifier.weight(1f)
                )
                AttendanceChip(
                    label = "Justificado",
                    status = AttendanceStatus.JUSTIFIED,
                    selectedStatus = item.status,
                    enabled = isEditable,
                    onSelect = onStatusSelected,
                    modifier = Modifier.weight(1f)
                )
            }

            // Campo de motivo si es Justificado
            if (item.status == AttendanceStatus.JUSTIFIED) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = item.justificationReason,
                    onValueChange = onJustificationChanged,
                    enabled = isEditable,
                    label = { Text("Motivo de la justificación (Obligatorio)") },
                    isError = item.justificationReason.isBlank(),
                    supportingText = {
                        if (item.justificationReason.isBlank()) {
                            Text("Ingresa el motivo de la inasistencia")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun AttendanceChip(
    label: String,
    status: String,
    selectedStatus: String,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = selectedStatus == status
    val color = when (status) {
        AttendanceStatus.ATTENDED -> StatusApproved
        AttendanceStatus.LATE -> StatusPending
        AttendanceStatus.ABSENT -> StatusRejected
        else -> MaterialTheme.colorScheme.primary
    }

    FilterChip(
        selected = isSelected,
        onClick = { if (enabled) onSelect(status) },
        enabled = enabled,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.25f),
            selectedLabelColor = color
        ),
        modifier = modifier
    )
}
