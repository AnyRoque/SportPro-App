package pe.edu.esan.sportpro.presentation.training

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.TrainingModel
import pe.edu.esan.sportpro.data.model.TrainingStatus
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.presentation.components.EmptyState
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.presentation.components.ReasonDialog
import pe.edu.esan.sportpro.presentation.components.StatusChip

/**
 * Pantalla principal de Planificación de Entrenamientos (US-11, US-13).
 * Soporta Vista Lista y Vista Semanal, filtrado por estado y toma de asistencia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingListScreen(
    user: UserModel,
    viewModel: TrainingViewModel,
    onNavigateForm: (trainingId: String?, duplicateFromId: String?) -> Unit,
    onNavigateDetail: (trainingId: String) -> Unit,
    onNavigateAttendance: (trainingId: String, teamId: String) -> Unit
) {
    val state by viewModel.listUi.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTrainingToCancel by remember { mutableStateOf<TrainingModel?>(null) }

    LaunchedEffect(user.uid) {
        viewModel.initList(user)
    }

    LaunchedEffect(state.errorMessage, state.infoMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (user.roleEnum == Role.DT) {
                FloatingActionButton(
                    onClick = { onNavigateForm(null, null) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Planificar Entrenamiento")
                }
            }
        }
    ) { paddingValues ->
        if (state.loading) {
            LoadingBox()
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Fila de Alternar Vistas: Lista vs Semanal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.setViewMode("LIST") },
                    colors = if (state.viewMode == "LIST") ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Vista Lista")
                }

                Button(
                    onClick = { viewModel.setViewMode("WEEKLY") },
                    colors = if (state.viewMode == "WEEKLY") ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Vista Semanal")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fila de Filtros por Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = state.filterStatus == "ALL",
                    onClick = { viewModel.setFilterStatus("ALL") },
                    label = { Text("Todas") }
                )
                FilterChip(
                    selected = state.filterStatus == TrainingStatus.PROGRAMMED,
                    onClick = { viewModel.setFilterStatus(TrainingStatus.PROGRAMMED) },
                    label = { Text("Programadas") }
                )
                FilterChip(
                    selected = state.filterStatus == TrainingStatus.COMPLETED,
                    onClick = { viewModel.setFilterStatus(TrainingStatus.COMPLETED) },
                    label = { Text("Realizadas") }
                )
                FilterChip(
                    selected = state.filterStatus == TrainingStatus.CANCELED,
                    onClick = { viewModel.setFilterStatus(TrainingStatus.CANCELED) },
                    label = { Text("Canceladas") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val filteredList = state.trainings.filter { t ->
                when (state.filterStatus) {
                    "ALL" -> true
                    else -> t.status == state.filterStatus
                }
            }

            if (filteredList.isEmpty()) {
                EmptyState(
                    message = if (user.roleEnum == Role.DT)
                        "No hay sesiones de entrenamiento registradas. Presiona '+' para planificar la primera."
                    else "No hay sesiones de entrenamiento programadas para tus equipos.",
                    modifier = Modifier.weight(1f)
                )
            } else if (state.viewMode == "WEEKLY") {
                // Vista Semanal agrupada por semanas/días
                val groupedByWeek = filteredList.groupBy { it.date }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    groupedByWeek.forEach { (date, sessions) ->
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text(
                                        text = "Fecha: $date",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                        items(sessions, key = { it.id }) { session ->
                            TrainingCard(
                                training = session,
                                isDt = user.roleEnum == Role.DT,
                                onClick = { onNavigateDetail(session.id) },
                                onEdit = { onNavigateForm(session.id, null) },
                                onDuplicate = { onNavigateForm(null, session.id) },
                                onAttendance = { onNavigateAttendance(session.id, session.teamId) },
                                onCancel = { selectedTrainingToCancel = session }
                            )
                        }
                    }
                }
            } else {
                // Vista Lista
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { it.id }) { session ->
                        TrainingCard(
                            training = session,
                            isDt = user.roleEnum == Role.DT,
                            onClick = { onNavigateDetail(session.id) },
                            onEdit = { onNavigateForm(session.id, null) },
                            onDuplicate = { onNavigateForm(null, session.id) },
                            onAttendance = { onNavigateAttendance(session.id, session.teamId) },
                            onCancel = { selectedTrainingToCancel = session }
                        )
                    }
                }
            }
        }
    }

    selectedTrainingToCancel?.let { session ->
        ReasonDialog(
            title = "Cancelar Sesión de Entrenamiento",
            confirmText = "Confirmar Cancelación",
            minLength = 5,
            onConfirm = { reason ->
                viewModel.cancelTraining(session.id, reason, user.uid)
                selectedTrainingToCancel = null
            },
            onDismiss = { selectedTrainingToCancel = null }
        )
    }
}

@Composable
fun TrainingCard(
    training: TrainingModel,
    isDt: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onAttendance: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${training.teamName} · ${training.category}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                StatusChip(status = training.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${training.date} · ${training.startTime} hs (${training.durationMinutes} min)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = training.location,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Objetivo (${training.objectiveType}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = training.objectiveDescription,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2
                    )
                    if (training.exercises.isNotEmpty()) {
                        Text(
                            text = "Ejercicios planificados: ${training.exercises.size} (${training.totalExerciseDuration} min acumulados)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (training.status == TrainingStatus.CANCELED && training.cancelReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Motivo de cancelación: ${training.cancelReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Acciones exclusivas del DT
            if (isDt) {
                Spacer(modifier = Modifier.height(12.dp))

                // Botón de Asistencia para el DT
                if (training.status != TrainingStatus.CANCELED) {
                    Button(
                        onClick = onAttendance,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (training.status == TrainingStatus.COMPLETED) "Ver / Editar Asistencia" else "Tomar Asistencia",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDuplicate,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Duplicar", style = MaterialTheme.typography.labelMedium)
                    }

                    if (training.status == TrainingStatus.PROGRAMMED) {
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Editar", style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = onCancel,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
