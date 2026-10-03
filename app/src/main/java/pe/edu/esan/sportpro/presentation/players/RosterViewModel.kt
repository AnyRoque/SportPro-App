package pe.edu.esan.sportpro.presentation.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.LinkStatus
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.remote.PlayerRepository
import pe.edu.esan.sportpro.data.remote.TeamRepository

data class RosterState(
    val loading: Boolean = true,
    val team: TeamModel? = null,
    val all: List<PlayerModel> = emptyList(),
    val query: String = "",
    val positionFilter: String = "Todas",
    val message: String? = null
) {
    /** Plantel: jugadores con vínculo aceptado, filtrados por búsqueda y posición. */
    val roster: List<PlayerModel>
        get() = all.filter { it.linkStatus == LinkStatus.ACCEPTED }
            .filter { query.isBlank() || it.fullName.contains(query.trim(), ignoreCase = true) }
            .filter { positionFilter == "Todas" || it.position == positionFilter }

    /** Solicitudes de vínculo enviadas por padres (US-02). */
    val requests: List<PlayerModel>
        get() = all.filter { it.linkStatus == LinkStatus.PENDING }
}

/** US-04: plantel del equipo y solicitudes de vínculo, en tiempo real. */
class RosterViewModel : ViewModel() {

    private val _state = MutableStateFlow(RosterState())
    val state: StateFlow<RosterState> = _state.asStateFlow()
    private var job: Job? = null

    fun start(teamId: String, dtUid: String) {
        if (job != null) return
        viewModelScope.launch {
            TeamRepository.getTeam(teamId).onSuccess { t -> _state.update { it.copy(team = t) } }
        }
        job = viewModelScope.launch {
            PlayerRepository.observePlayersByTeam(teamId, dtUid)
                .catch { e -> _state.update { it.copy(loading = false, message = e.message) } }
                .collect { list -> _state.update { it.copy(loading = false, all = list) } }
        }
    }

    fun onQuery(q: String) = _state.update { it.copy(query = q) }
    fun onPositionFilter(p: String) = _state.update { it.copy(positionFilter = p) }

    fun accept(player: PlayerModel) = viewModelScope.launch {
        PlayerRepository.acceptLink(player.id)
            .onSuccess { _state.update { it.copy(message = "${player.fullName} se unió al plantel") } }
            .onFailure { e -> _state.update { it.copy(message = e.message) } }
    }

    fun reject(player: PlayerModel) = viewModelScope.launch {
        PlayerRepository.rejectLink(player.id)
            .onSuccess { _state.update { it.copy(message = "Solicitud rechazada") } }
            .onFailure { e -> _state.update { it.copy(message = e.message) } }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}
