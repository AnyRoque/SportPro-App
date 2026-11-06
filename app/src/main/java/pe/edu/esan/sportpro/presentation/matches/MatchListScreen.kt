package pe.edu.esan.sportpro.presentation.matches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.R
import pe.edu.esan.sportpro.data.model.MatchModality
import pe.edu.esan.sportpro.data.model.MatchModel
import pe.edu.esan.sportpro.data.model.MatchStatus
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.presentation.components.EmptyState
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.presentation.components.ReasonDialog
import pe.edu.esan.sportpro.presentation.components.StatusChip

/**
 * Pantalla que lista los partidos clasificados por estado (Programado, En Curso, Finalizado, Cancelado) (US-16).
 */
@Composable
fun MatchListScreen(
    user: UserModel,
    viewModel: MatchViewModel,
    onNavigateForm: (matchId: String?) -> Unit,
    onNavigateDetail: (matchId: String) -> Unit,
    onNavigateCallUp: (matchId: String, hasCallUp: Boolean) -> Unit,
    onNavigateRegisterChild: () -> Unit = {},
    onNavigateJoinTeam: () -> Unit = {}
) {
    val matches by viewModel.matches.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDt = user.roleEnum == Role.DT

    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var cancelMatchId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user.uid) {
        viewModel.loadMatchesForUser(user)
    }

    Scaffold(
        floatingActionButton = {
            if (isDt) {
                FloatingActionButton(
                    onClick = { onNavigateForm(null) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.match_form_new_title))
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            LoadingBox()
        } else {
            val filteredMatches = when (selectedStatusFilter) {
                MatchStatus.PROGRAMMED -> matches.filter { it.status == MatchStatus.PROGRAMMED }
                MatchStatus.IN_PROGRESS -> matches.filter { it.status == MatchStatus.IN_PROGRESS }
                MatchStatus.FINISHED -> matches.filter { it.status == MatchStatus.FINISHED }
                MatchStatus.CANCELED -> matches.filter { it.status == MatchStatus.CANCELED }
                else -> matches
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Filtro/Clasificación por Estado
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedStatusFilter == "ALL",
                            onClick = { selectedStatusFilter = "ALL" },
                            label = { Text("Todos (${matches.size})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedStatusFilter == MatchStatus.PROGRAMMED,
                            onClick = { selectedStatusFilter = MatchStatus.PROGRAMMED },
                            label = { Text("Programados (${matches.count { it.status == MatchStatus.PROGRAMMED }})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedStatusFilter == MatchStatus.IN_PROGRESS,
                            onClick = { selectedStatusFilter = MatchStatus.IN_PROGRESS },
                            label = { Text("En Curso (${matches.count { it.status == MatchStatus.IN_PROGRESS }})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedStatusFilter == MatchStatus.FINISHED,
                            onClick = { selectedStatusFilter = MatchStatus.FINISHED },
                            label = { Text("Finalizados (${matches.count { it.status == MatchStatus.FINISHED }})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedStatusFilter == MatchStatus.CANCELED,
                            onClick = { selectedStatusFilter = MatchStatus.CANCELED },
                            label = { Text("Cancelados (${matches.count { it.status == MatchStatus.CANCELED }})") }
                        )
                    }
                }

                if (filteredMatches.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        EmptyState(message = stringResource(R.string.match_empty_list))

                        Spacer(modifier = Modifier.height(16.dp))

                        if (user.roleEnum == Role.PAD) {
                            Text(
                                text = stringResource(R.string.match_new_pad_prompt),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onNavigateRegisterChild) {
                                Text(stringResource(R.string.match_new_pad_action))
                            }
                        } else if (user.roleEnum == Role.JUG) {
                            Text(
                                text = stringResource(R.string.match_new_jug_prompt),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onNavigateJoinTeam) {
                                Text("Solicitar unirme a un equipo")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredMatches, key = { it.id }) { match ->
                            MatchCard(
                                match = match,
                                isDt = isDt,
                                onClick = { onNavigateDetail(match.id) },
                                onEdit = { onNavigateForm(match.id) },
                                onCancel = { cancelMatchId = match.id },
                                onCallUp = { onNavigateCallUp(match.id, match.hasCallUp) }
                            )
                        }
                    }
                }
            }
        }
    }

    cancelMatchId?.let { id ->
        ReasonDialog(
            title = stringResource(R.string.match_cancel_reason_title),
            confirmText = stringResource(R.string.match_cancel_confirm),
            minLength = 5,
            onConfirm = { reason ->
                viewModel.cancelMatch(id, reason, user.uid) {
                    cancelMatchId = null
                }
            },
            onDismiss = { cancelMatchId = null }
        )
    }
}

@Composable
private fun MatchCard(
    match: MatchModel,
    isDt: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onCallUp: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(match.status)
                Text(
                    text = "${match.matchType} · ${match.condition} · ${MatchModality.labelOf(match.modality)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${match.teamName} vs ${match.rival}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.match_category_label, match.category),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (match.status == MatchStatus.FINISHED || match.status == MatchStatus.IN_PROGRESS) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.match_score_label, match.ownScore, match.rivalScore),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(
                    text = "${match.date} a las ${match.time} hs",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(
                    text = match.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onCallUp) {
                    Text(
                        if (match.hasCallUp) stringResource(R.string.callup_nav_title)
                        else "Convocatoria"
                    )
                }

                if (isDt && match.status == MatchStatus.PROGRAMMED) {
                    Row {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(stringResource(R.string.match_cancel_button))
                        }
                        OutlinedButton(onClick = onEdit) {
                            Text(stringResource(R.string.match_edit_button))
                        }
                    }
                }
            }
        }
    }
}
