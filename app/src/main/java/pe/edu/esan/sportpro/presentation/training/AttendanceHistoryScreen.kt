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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.data.model.AttendanceRecordModel
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.AttendanceRepository
import pe.edu.esan.sportpro.presentation.components.EmptyState
import pe.edu.esan.sportpro.ui.theme.StatusApproved
import pe.edu.esan.sportpro.ui.theme.StatusPending
import pe.edu.esan.sportpro.ui.theme.StatusRejected

/**
 * Consulta del Historial de Asistencias Pasadas ordenadas por Sesión de Entrenamiento (US-13).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(
    user: UserModel,
    teamId: String,
    onNavigateAttendance: (trainingId: String) -> Unit,
    onBack: () -> Unit
) {
    val historyFlow = remember(teamId) {
        AttendanceRepository.observeAttendanceHistoryByTeam(teamId)
    }
    val records by historyFlow.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Asistencias") },
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
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (records.isEmpty()) {
                EmptyState(
                    message = "No hay registros de asistencias pasadas para este equipo.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(records, key = { it.id }) { record ->
                        AttendanceRecordCard(
                            record = record,
                            isDt = user.roleEnum == Role.DT,
                            onEdit = { onNavigateAttendance(record.trainingId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceRecordCard(
    record: AttendanceRecordModel,
    isDt: Boolean,
    onEdit: () -> Unit
) {
    val canEdit = isDt && AttendanceRepository.canEditAttendance(record.date)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fecha: ${record.date}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (canEdit) {
                    Text(
                        text = "Editable (48h)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${record.teamName} (${record.category})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Resumen de la Asistencia
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("Asistieron: ${record.totalAttended}", style = MaterialTheme.typography.bodySmall, color = StatusApproved, fontWeight = FontWeight.Bold)
                Text("Tardanzas: ${record.totalLate}", style = MaterialTheme.typography.bodySmall, color = StatusPending, fontWeight = FontWeight.Bold)
                Text("Faltas: ${record.totalAbsent}", style = MaterialTheme.typography.bodySmall, color = StatusRejected, fontWeight = FontWeight.Bold)
                Text("Justificados: ${record.totalJustified}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }

            if (canEdit) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver / Editar Asistencia")
                }
            }
        }
    }
}
