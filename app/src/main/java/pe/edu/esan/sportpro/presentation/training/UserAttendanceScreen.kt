package pe.edu.esan.sportpro.presentation.training

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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.data.model.AttendanceStatus
import pe.edu.esan.sportpro.data.model.PlayerAttendanceRecord
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.presentation.components.DropdownField
import pe.edu.esan.sportpro.presentation.components.EmptyState
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.presentation.components.StatusChip
import pe.edu.esan.sportpro.ui.theme.StatusApproved
import pe.edu.esan.sportpro.ui.theme.StatusPending
import pe.edu.esan.sportpro.ui.theme.StatusRejected
import java.util.Locale

private val monthMap = mapOf(
    "ALL" to "Todos los meses",
    "01" to "Enero",
    "02" to "Febrero",
    "03" to "Marzo",
    "04" to "Abril",
    "05" to "Mayo",
    "06" to "Junio",
    "07" to "Julio",
    "08" to "Agosto",
    "09" to "Septiembre",
    "10" to "Octubre",
    "11" to "Noviembre",
    "12" to "Diciembre"
)

private val yearOptions = listOf("ALL" to "Todas las temporadas", "2026" to "2026", "2025" to "2025")

/**
 * Consulta de Historial de Asistencia para Jugador (JUG) y Padre de Familia (PAD) (US-14).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAttendanceScreen(
    user: UserModel,
    viewModel: UserAttendanceViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(user.uid) {
        viewModel.loadForUser(user)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Asistencia") },
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

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Selector de Hijo para Padres (PAD) si tiene más de uno
            if (user.roleEnum == Role.PAD && state.children.isNotEmpty()) {
                val childNames = state.children.map { "${it.fullName} (${it.category})" }
                val selectedLabel = state.selectedChild?.let { "${it.fullName} (${it.category})" } ?: childNames.first()

                DropdownField(
                    label = "Seleccionar hijo(a)",
                    options = childNames,
                    selected = selectedLabel,
                    onSelected = { label ->
                        val child = state.children.find { "${it.fullName} (${it.category})" == label }
                        if (child != null) viewModel.selectChild(child)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Tarjeta de Resumen Estadístico y Porcentaje de Compromiso
            val formattedPercentage = String.format(Locale.getDefault(), "%.1f", state.attendancePercentage)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Compromiso de Asistencia",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            state.selectedPlayer?.let {
                                Text(
                                    text = "${it.fullName} · ${it.category}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "$formattedPercentage%",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Desglose Estadístico
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricItem(label = "Sesiones", value = state.totalSessions.toString(), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        MetricItem(label = "Asistió", value = state.attendedCount.toString(), color = StatusApproved)
                        MetricItem(label = "Tardanza", value = state.lateCount.toString(), color = StatusPending)
                        MetricItem(label = "Falta", value = state.absentCount.toString(), color = StatusRejected)
                        MetricItem(label = "Justificado", value = state.justifiedCount.toString(), color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fila de Filtros (Mes y Temporada)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DropdownField(
                    label = "Mes",
                    options = monthMap.values.toList(),
                    selected = monthMap[state.filterMonth] ?: "Todos los meses",
                    onSelected = { label ->
                        val code = monthMap.entries.find { it.value == label }?.key ?: "ALL"
                        viewModel.setFilterMonth(code)
                    },
                    modifier = Modifier.weight(1f)
                )

                DropdownField(
                    label = "Temporada",
                    options = yearOptions.map { it.second },
                    selected = yearOptions.find { it.first == state.filterYear }?.second ?: "Todas las temporadas",
                    onSelected = { label ->
                        val code = yearOptions.find { it.second == label }?.first ?: "ALL"
                        viewModel.setFilterYear(code)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lista de Sesiones / Registros
            if (state.filteredRecords.isEmpty()) {
                EmptyState(
                    message = "Aún no hay sesiones registradas",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.filteredRecords, key = { it.id }) { record ->
                        PlayerAttendanceRecordCard(record = record)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun PlayerAttendanceRecordCard(record: PlayerAttendanceRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Fecha: ${record.date}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                StatusChip(status = record.status)
            }

            if (record.teamName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.teamName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (record.status == AttendanceStatus.JUSTIFIED && record.justificationReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Motivo de justificación: ${record.justificationReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
