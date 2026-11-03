package pe.edu.esan.sportpro.presentation.matches

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.edu.esan.sportpro.R
import pe.edu.esan.sportpro.data.model.CallUpItemModel
import pe.edu.esan.sportpro.data.model.TacticalFormations
import pe.edu.esan.sportpro.data.model.TacticalPosition
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.presentation.components.DropdownField
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.presentation.components.SectionTitle

/**
 * Pantalla gráfica de Alineación y Formación Táctica en la cancha (US-18).
 */
@Composable
fun LineupScreen(
    user: UserModel,
    viewModel: LineupViewModel,
    matchId: String,
    onBack: () -> Unit
) {
    val match by viewModel.match.collectAsState()
    val confirmedCallUps by viewModel.confirmedCallUps.collectAsState()
    val positions by viewModel.positions.collectAsState()
    val formation by viewModel.formation.collectAsState()
    val isEditable by viewModel.isEditable.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val validationErr by viewModel.validationError.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedSlotToAssign by remember { mutableStateOf<TacticalPosition?>(null) }

    LaunchedEffect(matchId) {
        viewModel.load(matchId, user)
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(validationErr) {
        validationErr?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    if (isLoading || match == null) {
        LoadingBox()
        return
    }

    val m = match!!
    val assignedPlayerIds = positions.mapNotNull { it.playerId }.toSet()
    val substitutes = confirmedCallUps.filter { !assignedPlayerIds.contains(it.playerId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "${m.teamName} vs ${m.rival}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Selector de Formación táctica
        val availableFormations = TacticalFormations.forModality(m.modality)
        if (isEditable) {
            DropdownField(
                label = stringResource(R.string.lineup_formation_label),
                options = availableFormations,
                selected = formation,
                onSelected = { viewModel.changeFormation(it) }
            )
        } else {
            Text(
                text = "Formación Táctica: $formation (Modo Solo Lectura)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        validationErr?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }

        SectionTitle("Campo Táctico - Titulares (${positions.count { it.playerId != null }}/${m.modality})")

        // Campo Gráfico de Fútbol (Cancha)
        SoccerPitch(
            positions = positions,
            isEditable = isEditable,
            onSlotClick = { slot ->
                if (isEditable) selectedSlotToAssign = slot
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Suplentes (Banca)
        SectionTitle(stringResource(R.string.lineup_substitutes_title) + " (${substitutes.size})")

        if (substitutes.isEmpty()) {
            Text(
                text = stringResource(R.string.lineup_no_substitutes),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    substitutes.forEach { sub ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(
                                    text = " ${sub.playerName}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                text = sub.position.ifBlank { "Suplente" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        if (isEditable) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.saveLineup(user, onBack) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.lineup_save_button))
            }
        }

        SnackbarHost(hostState = snackbarHostState)
    }

    // Diálogo de asignación de jugador a un slot de posición táctica
    selectedSlotToAssign?.let { slot ->
        AssignPlayerDialog(
            slot = slot,
            confirmedCallUps = confirmedCallUps,
            assignedPlayerIds = assignedPlayerIds,
            onSelectPlayer = { selectedPlayer ->
                viewModel.assignPlayerToPosition(slot.slotIndex, selectedPlayer)
                selectedSlotToAssign = null
            },
            onRemove = {
                viewModel.unassignPlayerFromPosition(slot.slotIndex)
                selectedSlotToAssign = null
            },
            onDismiss = { selectedSlotToAssign = null }
        )
    }
}

/**
 * Campo Gráfico de Fútbol interactivo en Compose.
 */
@Composable
private fun SoccerPitch(
    positions: List<TacticalPosition>,
    isEditable: Boolean,
    onSlotClick: (TacticalPosition) -> Unit
) {
    val pitchGreen = Color(0xFF2E7D32)
    val lineWhite = Color(0xDDFFFFFF)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = pitchGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()

            // Dibujar líneas reglamentarias del campo de juego
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 3.dp.toPx())

                // Borde
                drawRect(
                    color = lineWhite,
                    topLeft = Offset(12.dp.toPx(), 12.dp.toPx()),
                    size = Size(widthPx - 24.dp.toPx(), heightPx - 24.dp.toPx()),
                    style = stroke
                )

                // Línea Central
                drawLine(
                    color = lineWhite,
                    start = Offset(12.dp.toPx(), heightPx / 2f),
                    end = Offset(widthPx - 12.dp.toPx(), heightPx / 2f),
                    strokeWidth = 3.dp.toPx()
                )

                // Círculo Central
                drawCircle(
                    color = lineWhite,
                    radius = 45.dp.toPx(),
                    center = Offset(widthPx / 2f, heightPx / 2f),
                    style = stroke
                )

                // Área Chica / Penal Superior (Rival)
                drawRect(
                    color = lineWhite,
                    topLeft = Offset(widthPx * 0.25f, 12.dp.toPx()),
                    size = Size(widthPx * 0.5f, heightPx * 0.16f),
                    style = stroke
                )

                // Área Chica / Penal Inferior (Propia)
                drawRect(
                    color = lineWhite,
                    topLeft = Offset(widthPx * 0.25f, heightPx * 0.84f - 12.dp.toPx()),
                    size = Size(widthPx * 0.5f, heightPx * 0.16f),
                    style = stroke
                )
            }

            // Renderizar los slots tácticos con fichas interactivas
            positions.forEach { pos ->
                val xPos = (pos.xRatio * maxWidth.value).dp - 24.dp
                val yPos = (pos.yRatio * maxHeight.value).dp - 24.dp

                Box(
                    modifier = Modifier
                        .offset(x = xPos, y = yPos)
                        .clickable(enabled = isEditable) { onSlotClick(pos) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = if (pos.playerId != null) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier
                                .size(36.dp)
                                .border(1.5.dp, Color.White, CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (pos.playerId != null) {
                                    Text(
                                        text = pos.playerName?.take(2)?.uppercase() ?: "P",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = pitchGreen, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = pos.playerName ?: pos.positionName,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignPlayerDialog(
    slot: TacticalPosition,
    confirmedCallUps: List<CallUpItemModel>,
    assignedPlayerIds: Set<String>,
    onSelectPlayer: (CallUpItemModel) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lineup_assign_dialog_title, slot.positionName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                if (confirmedCallUps.isEmpty()) {
                    Text("No hay jugadores confirmados aún para este partido. Los jugadores deben confirmar su asistencia en la convocatoria para ser alineados.")
                } else {
                    confirmedCallUps.forEach { player ->
                        val isAssignedElsewhere = assignedPlayerIds.contains(player.playerId) && player.playerId != slot.playerId
                        val isCurrent = player.playerId == slot.playerId

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isAssignedElsewhere) {
                                        errorMsg = "Un jugador no puede ocupar dos posiciones"
                                    } else {
                                        onSelectPlayer(player)
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                                else if (isAssignedElsewhere) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = player.playerName,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isAssignedElsewhere) Color.Gray else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (player.position.isNotBlank()) {
                                        Text(
                                            text = "Posición habitual: ${player.position}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                Text(
                                    text = if (isCurrent) "Titular actual" else if (isAssignedElsewhere) "Ocupado" else "Disponible",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isAssignedElsewhere) Color.Gray else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (slot.playerId != null) {
                OutlinedButton(onClick = onRemove) {
                    Text("Quitar de la posición")
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
