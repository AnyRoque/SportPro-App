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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.R
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.presentation.components.DateField
import pe.edu.esan.sportpro.presentation.components.FormTextField
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.presentation.components.SectionTitle

/**
 * Pantalla para seleccionar jugadores y definir la fecha límite de respuesta de convocatoria (US-17).
 */
@Composable
fun CallUpFormScreen(
    user: UserModel,
    viewModel: CallUpViewModel,
    matchId: String,
    isAdding: Boolean = false,
    onBack: () -> Unit
) {
    val match by viewModel.match.collectAsState()
    val activePlayers by viewModel.activePlayers.collectAsState()
    val callUpItems by viewModel.callUpItems.collectAsState()
    val selectedPlayerIds by viewModel.selectedPlayerIds.collectAsState()
    val deadlineDate by viewModel.deadlineDate.collectAsState()
    val deadlineTime by viewModel.deadlineTime.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val deadlineErr by viewModel.deadlineError.collectAsState()
    val minPlayersErr by viewModel.minPlayersError.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(matchId) {
        viewModel.loadMatchAndPlayers(matchId, user.uid)
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
    val existingIds = callUpItems.map { it.playerId }.toSet()
    val availablePlayers = if (isAdding) activePlayers.filter { !existingIds.contains(it.id) } else activePlayers

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle(
            text = if (isAdding) stringResource(R.string.callup_add_players_title)
            else stringResource(R.string.callup_form_title)
        )

        Text(
            text = "${m.teamName} vs ${m.rival} (${m.date} ${m.time} hs)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (!isAdding) {
            Text(
                text = stringResource(R.string.callup_min_required, m.modality),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            DateField(
                label = stringResource(R.string.callup_deadline_date),
                value = deadlineDate,
                onValueChange = { viewModel.deadlineDate.value = it },
                error = deadlineErr
            )

            FormTextField(
                value = deadlineTime,
                onValueChange = { viewModel.deadlineTime.value = it },
                label = stringResource(R.string.callup_deadline_time),
                error = deadlineErr
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.callup_select_players) + " (${selectedPlayerIds.size})",
                style = MaterialTheme.typography.labelLarge
            )
            OutlinedButton(onClick = { viewModel.selectAllPlayers() }) {
                Text(stringResource(R.string.callup_select_all))
            }
        }

        minPlayersErr?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availablePlayers, key = { it.id }) { player ->
                val isSelected = selectedPlayerIds.contains(player.id)
                PlayerSelectItemCard(
                    player = player,
                    isSelected = isSelected,
                    onToggle = { viewModel.togglePlayerSelection(player.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (isAdding) {
                    viewModel.addPlayersToCallUp { onBack() }
                } else {
                    viewModel.createCallUp { onBack() }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isAdding) stringResource(R.string.callup_button_add_players)
                else stringResource(R.string.callup_button_submit)
            )
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
private fun PlayerSelectItemCard(
    player: PlayerModel,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = player.fullName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Posición: ${player.position.ifBlank { "Sin definir" }}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
        }
    }
}
