package pe.edu.esan.sportpro.presentation.matches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.R
import pe.edu.esan.sportpro.data.model.EventStatus
import pe.edu.esan.sportpro.data.model.EventTeam
import pe.edu.esan.sportpro.data.model.EventType
import pe.edu.esan.sportpro.data.model.MatchEventModel
import pe.edu.esan.sportpro.data.model.MatchModality
import pe.edu.esan.sportpro.data.model.MatchModel
import pe.edu.esan.sportpro.data.model.MatchStatus
import pe.edu.esan.sportpro.data.remote.MatchRepository
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.presentation.components.StatusChip

/**
 * Pantalla de detalle de un partido con marcador, información de puntajes y casilleros de faltas/sanciones por jugador (US-16 / US-19).
 */
@Composable
fun MatchDetailScreen(
    matchId: String,
    onNavigateCallUp: (hasCallUp: Boolean) -> Unit,
    onNavigateLineup: () -> Unit = {},
    onNavigateConsole: () -> Unit = {},
    onNavigateLive: () -> Unit = {},
    onBack: () -> Unit
) {
    var match by remember { mutableStateOf<MatchModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val eventsFlow = remember(matchId) { MatchRepository.observeMatchEvents(matchId) }
    val eventsPair by eventsFlow.collectAsState(initial = Pair(emptyList(), false))
    val eventsList = eventsPair.first.filter { it.status == EventStatus.ACTIVE }

    LaunchedEffect(matchId) {
        val result = MatchRepository.getMatch(matchId)
        result.onSuccess { m -> match = m }
        isLoading = false
    }

    if (isLoading) {
        LoadingBox()
    } else if (match == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.match_not_found))
        }
    } else {
        val m = match!!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusChip(m.status)
                        Text(
                            text = "${m.matchType} · ${m.condition} · ${MatchModality.labelOf(m.modality)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "${m.teamName} vs ${m.rival}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = stringResource(R.string.match_category_label, m.category),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Puntajes y marcador del partido
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(m.teamName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text("${m.ownScore}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Text("VS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(m.rival, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text("${m.rivalScore}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigateCallUp(m.hasCallUp) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                if (m.hasCallUp) stringResource(R.string.callup_panel_title)
                                else stringResource(R.string.callup_form_title)
                            )
                        }

                        if (m.hasCallUp) {
                            OutlinedButton(
                                onClick = onNavigateLineup,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.lineup_title))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateLive,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ver Partido en Vivo")
                        }

                        if (m.hasLineup) {
                            OutlinedButton(
                                onClick = onNavigateConsole,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.console_title))
                            }
                        }
                    }
                }
            }

            // Casilla de Faltas Cometidas y Sanciones por Jugador y Equipo
            PlayerFoulsSummaryCard(events = eventsList)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.match_info_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    HorizontalDivider()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Column {
                            Text(stringResource(R.string.match_date_time_label), style = MaterialTheme.typography.labelMedium)
                            Text("${m.date} a las ${m.time} hs", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Column {
                            Text(stringResource(R.string.match_location_title), style = MaterialTheme.typography.labelMedium)
                            Text(m.location, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Column {
                            Text(stringResource(R.string.match_period_duration_title), style = MaterialTheme.typography.labelMedium)
                            Text(stringResource(R.string.match_period_duration_value, m.periodMinutes, m.totalMatchDuration), style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (m.operatorName.isNotBlank() || m.dtName.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Column {
                                Text(stringResource(R.string.match_operator_label), style = MaterialTheme.typography.labelMedium)
                                Text(m.operatorName.ifBlank { m.dtName }, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    if (m.status == MatchStatus.CANCELED && m.cancelReason.isNotBlank()) {
                        HorizontalDivider()
                        Text(
                            text = stringResource(R.string.match_cancel_reason_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = m.cancelReason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (m.notes.isNotBlank()) {
                        HorizontalDivider()
                        Text(text = stringResource(R.string.match_notes_title), style = MaterialTheme.typography.labelMedium)
                        Text(text = m.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerFoulsSummaryCard(events: List<MatchEventModel>) {
    val foulEvents = events.filter {
        it.type == EventType.FOUL || it.type == EventType.YELLOW_CARD || it.type == EventType.RED_CARD
    }

    if (foulEvents.isEmpty()) return

    // Agrupar por equipo (Propio vs Rival) y luego por jugador
    val teamFoulsGrouped = foulEvents
        .filter { it.playerName.isNotBlank() }
        .groupBy { it.team }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.padding(4.dp))
                Text(
                    text = "Casilla de Faltas y Sanciones por Jugador",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            HorizontalDivider()

            teamFoulsGrouped.forEach { (team, teamEvents) ->
                val teamLabel = if (team == EventTeam.OWN) "Equipo Propio" else "Equipo Rival"
                val playerMap = teamEvents.groupBy { it.playerName }

                Text(
                    text = teamLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (team == EventTeam.OWN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )

                playerMap.forEach { (playerName, pEvents) ->
                    val foulCount = pEvents.count { it.type == EventType.FOUL }
                    val yellowCount = pEvents.count { it.type == EventType.YELLOW_CARD }
                    val redCount = pEvents.count { it.type == EventType.RED_CARD }
                    val isExpelled = redCount > 0 || yellowCount >= 2

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isExpelled) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = playerName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExpelled) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                                )

                                if (isExpelled) {
                                    Surface(color = MaterialTheme.colorScheme.error, shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            text = "🟥 EXPULSADO",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "⚠️ $foulCount Faltas cometidas · 🟨 $yellowCount Amarillas · 🟥 $redCount Rojas",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isExpelled) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.secondary
                            )

                            pEvents.forEach { ev ->
                                val label = when (ev.type) {
                                    EventType.FOUL -> "Falta cometida"
                                    EventType.YELLOW_CARD -> "Tarjeta Amarilla"
                                    EventType.RED_CARD -> "Tarjeta Roja"
                                    else -> EventType.labelOf(ev.type)
                                }
                                Text(
                                    text = "• Minuto ${ev.minute}': $label" + if (ev.observation.isNotBlank()) " (${ev.observation})" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isExpelled) MaterialTheme.colorScheme.onErrorContainer else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
