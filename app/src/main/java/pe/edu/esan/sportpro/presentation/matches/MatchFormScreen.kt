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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.R
import pe.edu.esan.sportpro.data.model.MatchCondition
import pe.edu.esan.sportpro.data.model.MatchModality
import pe.edu.esan.sportpro.data.model.MatchType
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.presentation.components.DateField
import pe.edu.esan.sportpro.presentation.components.DropdownField
import pe.edu.esan.sportpro.presentation.components.FormTextField
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.presentation.components.SectionTitle

/**
 * Formulario para programar o editar un partido (US-16).
 */
@Composable
fun MatchFormScreen(
    user: UserModel,
    viewModel: MatchViewModel,
    matchId: String? = null,
    onBack: () -> Unit
) {
    val approvedTeams by viewModel.approvedTeams.collectAsState()
    val teamMembers by viewModel.teamMembers.collectAsState()
    val selectedTeam by viewModel.selectedTeam.collectAsState()
    val selectedOperator by viewModel.selectedOperator.collectAsState()
    val rival by viewModel.rival.collectAsState()
    val matchType by viewModel.matchType.collectAsState()
    val condition by viewModel.condition.collectAsState()
    val modality by viewModel.modality.collectAsState()
    val periodMinutes by viewModel.periodMinutes.collectAsState()
    val date by viewModel.date.collectAsState()
    val time by viewModel.time.collectAsState()
    val location by viewModel.location.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Errores por campo
    val teamErr by viewModel.teamError.collectAsState()
    val rivalErr by viewModel.rivalError.collectAsState()
    val dateErr by viewModel.dateError.collectAsState()
    val timeErr by viewModel.timeError.collectAsState()
    val locationErr by viewModel.locationError.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(user.uid) {
        viewModel.loadApprovedTeams(user)
        if (!matchId.isNullOrBlank()) {
            viewModel.loadMatchForEdit(matchId)
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (isLoading) {
        LoadingBox()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle(stringResource(R.string.match_form_new_title))

        // Selección de equipo (solo si es nuevo)
        if (matchId.isNullOrBlank()) {
            val teamNames = approvedTeams.map { it.name }
            DropdownField(
                label = stringResource(R.string.match_team_label),
                options = teamNames,
                selected = selectedTeam?.name.orEmpty(),
                onSelected = { name ->
                    val team = approvedTeams.firstOrNull { it.name == name }
                    if (team != null) viewModel.onTeamSelected(team, user)
                },
                error = teamErr
            )
        } else {
            FormTextField(
                value = selectedTeam?.name.orEmpty(),
                onValueChange = {},
                label = stringResource(R.string.match_team_label),
                locked = true
            )
        }

        // Rival
        val allApprovedTeams by viewModel.allApprovedTeams.collectAsState()
        val rivalOptions = allApprovedTeams.map { it.name }.filter { it != selectedTeam?.name }
        if (rivalOptions.isNotEmpty()) {
            DropdownField(
                label = stringResource(R.string.match_rival_label) + " (Seleccionar de lista)",
                options = rivalOptions,
                selected = if (rivalOptions.contains(rival)) rival else "",
                onSelected = { viewModel.rival.value = it }
            )
        }
        FormTextField(
            value = rival,
            onValueChange = { viewModel.rival.value = it },
            label = stringResource(R.string.match_rival_label) + " (Nombre libre)",
            error = rivalErr
        )

        // Tipo de Partido (Amistoso / Torneo / Liga)
        Text(
            text = stringResource(R.string.match_type_label),
            style = MaterialTheme.typography.labelLarge
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MatchType.ALL.forEach { type ->
                FilterChip(
                    selected = matchType == type,
                    onClick = { viewModel.matchType.value = type },
                    label = { Text(type) }
                )
            }
        }

        // Condición (Local / Visitante)
        Text(
            text = stringResource(R.string.match_condition_label),
            style = MaterialTheme.typography.labelLarge
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MatchCondition.ALL.forEach { cond ->
                FilterChip(
                    selected = condition == cond,
                    onClick = { viewModel.condition.value = cond },
                    label = { Text(cond) }
                )
            }
        }

        // Modalidad
        Text(
            text = stringResource(R.string.match_modality_label),
            style = MaterialTheme.typography.labelLarge
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MatchModality.ALL.forEach { mod ->
                FilterChip(
                    selected = modality == mod,
                    onClick = { viewModel.modality.value = mod },
                    label = { Text(MatchModality.labelOf(mod)) }
                )
            }
        }

        // Fecha
        DateField(
            label = stringResource(R.string.match_date_label),
            value = date,
            onValueChange = { viewModel.date.value = it },
            error = dateErr
        )

        // Hora
        FormTextField(
            value = time,
            onValueChange = { viewModel.time.value = it },
            label = stringResource(R.string.match_time_label),
            error = timeErr
        )

        // Duración por tiempo
        DropdownField(
            label = stringResource(R.string.match_period_minutes_label),
            options = listOf("20", "25", "30", "35", "40", "45"),
            selected = periodMinutes,
            onSelected = { viewModel.periodMinutes.value = it }
        )

        // Ubicación
        FormTextField(
            value = location,
            onValueChange = { viewModel.location.value = it },
            label = stringResource(R.string.match_location_label),
            error = locationErr
        )

        // Operador de Registro
        val memberNames = teamMembers.map { it.fullName.ifBlank { it.name } }
        DropdownField(
            label = stringResource(R.string.match_operator_label),
            options = memberNames.ifEmpty { listOf(user.fullName) },
            selected = selectedOperator?.fullName?.ifBlank { selectedOperator?.name }.orEmpty().ifBlank { user.fullName },
            onSelected = { selectedName ->
                viewModel.selectedOperator.value = teamMembers.firstOrNull {
                    (it.fullName.ifBlank { it.name }) == selectedName
                } ?: user
            }
        )

        // Observaciones
        FormTextField(
            value = notes,
            onValueChange = { viewModel.notes.value = it },
            label = stringResource(R.string.match_notes_label),
            singleLine = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.saveMatch(user, matchId, onBack) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.match_save_button))
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}
