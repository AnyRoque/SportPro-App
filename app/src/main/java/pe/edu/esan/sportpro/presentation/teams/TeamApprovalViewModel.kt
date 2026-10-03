package pe.edu.esan.sportpro.presentation.teams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.TeamStatus
import pe.edu.esan.sportpro.data.remote.TeamRepository

data class TeamApprovalState(
    val status: String = TeamStatus.PENDING,
    val loading: Boolean = true,
    val teams: List<TeamModel> = emptyList(),
    val message: String? = null
)

/** US-07: el ADM aprueba o rechaza (con motivo) los equipos registrados por los DT. */
class TeamApprovalViewModel : ViewModel() {

    private val _state = MutableStateFlow(TeamApprovalState())
    val state: StateFlow<TeamApprovalState> = _state.asStateFlow()
    private var job: Job? = null

    init { selectStatus(TeamStatus.PENDING) }

    fun selectStatus(status: String) {
        job?.cancel()
        _state.update { it.copy(status = status, loading = true) }
        job = viewModelScope.launch {
            TeamRepository.observeTeamsByStatus(status)
                .catch { e -> _state.update { it.copy(loading = false, message = e.message) } }
                .collect { list -> _state.update { it.copy(loading = false, teams = list) } }
        }
    }

    fun approve(team: TeamModel, adminUid: String) = viewModelScope.launch {
        TeamRepository.approveTeam(team.id, adminUid)
            .onSuccess { _state.update { it.copy(message = "“${team.name}” fue aprobado") } }
            .onFailure { e -> _state.update { it.copy(message = e.message) } }
    }

    fun reject(team: TeamModel, adminUid: String, reason: String) = viewModelScope.launch {
        TeamRepository.rejectTeam(team.id, adminUid, reason)
            .onSuccess { _state.update { it.copy(message = "“${team.name}” fue rechazado") } }
            .onFailure { e -> _state.update { it.copy(message = e.message) } }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}
