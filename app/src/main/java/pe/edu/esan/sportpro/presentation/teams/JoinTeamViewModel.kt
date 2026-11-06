package pe.edu.esan.sportpro.presentation.teams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.PlayerRepository
import pe.edu.esan.sportpro.data.remote.TeamRepository

/**
 * ViewModel para que los jugadores o padres exploren equipos y envíen solicitudes de ingreso al DT.
 */
class JoinTeamViewModel : ViewModel() {

    private val _approvedTeams = MutableStateFlow<List<TeamModel>>(emptyList())
    val approvedTeams: StateFlow<List<TeamModel>> = _approvedTeams.asStateFlow()

    private val _userRequests = MutableStateFlow<List<PlayerModel>>(emptyList())
    val userRequests: StateFlow<List<PlayerModel>> = _userRequests.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val query = MutableStateFlow("")

    fun loadApprovedTeamsAndUserRequests(user: UserModel) {
        viewModelScope.launch {
            _isLoading.value = true
            val teamsRes = TeamRepository.getApprovedTeams()
            teamsRes.onSuccess { teams ->
                _approvedTeams.value = teams
            }.onFailure { e ->
                _message.value = e.message
            }
            _isLoading.value = false
        }

        viewModelScope.launch {
            val flow = if (user.roleEnum == Role.PAD) {
                PlayerRepository.observeChildrenByParent(user.uid)
            } else {
                PlayerRepository.observePlayersByUser(user.uid)
            }
            flow.collect { requests ->
                _userRequests.value = requests
            }
        }
    }

    fun requestToJoinTeam(user: UserModel, team: TeamModel, position: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = PlayerRepository.requestToJoinTeam(user, team, position)
            _isLoading.value = false
            result.onSuccess {
                _message.value = "Solicitud enviada con éxito al DT de ${team.name}"
                onDone()
            }.onFailure { e ->
                _message.value = e.message
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
