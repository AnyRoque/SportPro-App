package pe.edu.esan.sportpro.presentation.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.MatchCondition
import pe.edu.esan.sportpro.data.model.MatchModality
import pe.edu.esan.sportpro.data.model.MatchModel
import pe.edu.esan.sportpro.data.model.MatchType
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.MatchRepository
import pe.edu.esan.sportpro.util.Validators
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * ViewModel para la gestión y programación de partidos (US-16).
 */
class MatchViewModel : ViewModel() {

    private val _matches = MutableStateFlow<List<MatchModel>>(emptyList())
    val matches: StateFlow<List<MatchModel>> = _matches.asStateFlow()

    private val _approvedTeams = MutableStateFlow<List<TeamModel>>(emptyList())
    val approvedTeams: StateFlow<List<TeamModel>> = _approvedTeams.asStateFlow()

    private val _allApprovedTeams = MutableStateFlow<List<TeamModel>>(emptyList())
    val allApprovedTeams: StateFlow<List<TeamModel>> = _allApprovedTeams.asStateFlow()

    private val _teamMembers = MutableStateFlow<List<UserModel>>(emptyList())
    val teamMembers: StateFlow<List<UserModel>> = _teamMembers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Errores por campo
    val teamError = MutableStateFlow<String?>(null)
    val rivalError = MutableStateFlow<String?>(null)
    val dateError = MutableStateFlow<String?>(null)
    val timeError = MutableStateFlow<String?>(null)
    val locationError = MutableStateFlow<String?>(null)

    // Campos del formulario
    var selectedTeam = MutableStateFlow<TeamModel?>(null)
    var rival = MutableStateFlow("")
    var matchType = MutableStateFlow(MatchType.FRIENDLY)
    var condition = MutableStateFlow(MatchCondition.LOCAL)
    var modality = MutableStateFlow(MatchModality.F7)
    var periodMinutes = MutableStateFlow("30")
    var date = MutableStateFlow("")
    var time = MutableStateFlow("10:00")
    var location = MutableStateFlow("")
    var notes = MutableStateFlow("")
    var selectedOperator = MutableStateFlow<UserModel?>(null)

    fun loadMatchesForUser(user: UserModel) {
        viewModelScope.launch {
            _isLoading.value = true
            MatchRepository.observeMatchesForUser(user).collect { list ->
                _matches.value = list.sortedByDescending { it.date }
                _isLoading.value = false
            }
        }
    }

    fun loadApprovedTeams(user: UserModel) {
        viewModelScope.launch {
            val result = MatchRepository.getApprovedTeamsByDt(user.uid)
            result.onSuccess { teams ->
                _approvedTeams.value = teams
                if (selectedTeam.value == null && teams.isNotEmpty()) {
                    onTeamSelected(teams.first(), user)
                }
            }.onFailure { e ->
                _error.value = e.message
            }

            val allRes = MatchRepository.getAllApprovedTeams()
            allRes.onSuccess { allTeams ->
                _allApprovedTeams.value = allTeams
            }
        }
    }

    fun onTeamSelected(team: TeamModel, dtUser: UserModel) {
        selectedTeam.value = team
        teamError.value = null
        viewModelScope.launch {
            val membersRes = MatchRepository.getTeamMembers(team.id, dtUser)
            membersRes.onSuccess { members ->
                _teamMembers.value = members
                if (selectedOperator.value == null || members.none { it.uid == selectedOperator.value?.uid }) {
                    selectedOperator.value = members.firstOrNull() ?: dtUser
                }
            }
        }
    }

    fun loadMatchForEdit(matchId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = MatchRepository.getMatch(matchId)
            result.onSuccess { m ->
                rival.value = m.rival
                matchType.value = m.matchType
                condition.value = m.condition
                modality.value = m.modality
                periodMinutes.value = m.periodMinutes.toString()
                date.value = m.date
                time.value = m.time
                location.value = m.location
                notes.value = m.notes
                selectedOperator.value = UserModel(uid = m.operatorUid, name = m.operatorName)
            }.onFailure { e ->
                _error.value = e.message
            }
            _isLoading.value = false
        }
    }

    private fun clearFieldErrors() {
        teamError.value = null
        rivalError.value = null
        dateError.value = null
        timeError.value = null
        locationError.value = null
        _error.value = null
    }

    private fun validateFields(): Boolean {
        clearFieldErrors()
        var valid = true

        val team = selectedTeam.value
        if (team == null) {
            teamError.value = "Selecciona un equipo"
            valid = false
        }

        if (rival.value.trim().isBlank()) {
            rivalError.value = "Ingresa el nombre del equipo rival"
            valid = false
        }

        val parsedDate = Validators.parseDate(date.value)
        if (parsedDate == null) {
            dateError.value = "Selecciona una fecha válida (dd/mm/aaaa)"
            valid = false
        }

        val parsedTime = try {
            LocalTime.parse(time.value.trim(), DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {
            null
        }

        if (parsedTime == null) {
            timeError.value = "Ingresa una hora válida (HH:mm)"
            valid = false
        }

        if (parsedDate != null && parsedTime != null) {
            val dateTime = LocalDateTime.of(parsedDate, parsedTime)
            if (!dateTime.isAfter(LocalDateTime.now())) {
                dateError.value = "La fecha y hora deben ser posteriores a la actual"
                timeError.value = "La fecha y hora deben ser posteriores a la actual"
                valid = false
            }
        }

        if (location.value.trim().isBlank()) {
            locationError.value = "Ingresa la ubicación o cancha"
            valid = false
        }

        return valid
    }

    fun saveMatch(user: UserModel, matchId: String? = null, onDone: () -> Unit) {
        if (!validateFields()) return

        val team = selectedTeam.value ?: return
        val operator = selectedOperator.value ?: user
        val parsedPeriodMinutes = periodMinutes.value.toIntOrNull() ?: 30

        viewModelScope.launch {
            _isLoading.value = true

            val matchToSave = MatchModel(
                id = matchId ?: "",
                dtUid = user.uid,
                dtName = user.fullName,
                teamId = team.id,
                teamName = team.name,
                category = team.category,
                rival = rival.value.trim(),
                matchType = matchType.value,
                condition = condition.value,
                modality = modality.value,
                periodMinutes = parsedPeriodMinutes,
                date = date.value.trim(),
                time = time.value.trim(),
                location = location.value.trim(),
                notes = notes.value.trim(),
                operatorUid = operator.uid,
                operatorName = operator.fullName.ifBlank { operator.name }
            )

            val result = if (matchId.isNullOrBlank()) {
                MatchRepository.createMatch(matchToSave)
            } else {
                MatchRepository.updateMatch(matchToSave)
            }

            _isLoading.value = false
            result.onSuccess {
                onDone()
            }.onFailure { e ->
                _error.value = e.message
            }
        }
    }

    fun cancelMatch(matchId: String, reason: String, dtUid: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = MatchRepository.cancelMatch(matchId, reason, dtUid)
            _isLoading.value = false
            result.onSuccess {
                onDone()
            }.onFailure { e ->
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
