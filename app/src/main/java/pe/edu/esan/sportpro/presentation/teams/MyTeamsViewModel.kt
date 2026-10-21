package pe.edu.esan.sportpro.presentation.teams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.remote.TeamRepository

data class TeamListState(
    val loading: Boolean = true,
    val teams: List<TeamModel> = emptyList(),
    val error: String? = null
)

/** US-03: lista "Mis equipos" del DT, actualizada en tiempo real. */
class MyTeamsViewModel : ViewModel() {

    private val _state = MutableStateFlow(TeamListState())
    val state: StateFlow<TeamListState> = _state.asStateFlow()
    private var job: Job? = null

    fun start(dtUid: String) {
        if (job != null) return
        job = viewModelScope.launch {
            TeamRepository.observeTeamsByDt(dtUid)
                .catch { e -> _state.value = TeamListState(loading = false, error = e.message) }
                .collect { list -> _state.value = TeamListState(loading = false, teams = list) }
        }
    }
}
