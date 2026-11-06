package pe.edu.esan.sportpro.presentation.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.EventStatus
import pe.edu.esan.sportpro.data.model.EventType
import pe.edu.esan.sportpro.data.model.MatchEventModel
import pe.edu.esan.sportpro.data.model.MatchModel
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.AttendanceRepository
import pe.edu.esan.sportpro.data.remote.MatchRepository

data class LiveMatchUiState(
    val match: MatchModel? = null,
    val events: List<MatchEventModel> = emptyList(),
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val accessDenied: Boolean = false,
    val accessDeniedReason: String = "",
    val elapsedSeconds: Long = 0L,
    val currentMinute: Int = 1,
    val animatedEvent: MatchEventModel? = null
)

/**
 * ViewModel para la Visualización del Marcador y Cronología en Vivo Sin Recarga (US-21).
 */
class LiveMatchViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LiveMatchUiState())
    val uiState: StateFlow<LiveMatchUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var previousEventIds = setOf<String>()

    fun loadLiveMatch(matchId: String, user: UserModel) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, accessDenied = false)

            // 1. Observar cambios en tiempo real del Partido (snapshot listener)
            viewModelScope.launch {
                MatchRepository.observeMatch(matchId).collect { (m, matchFromCache) ->
                    // Validar control de acceso restringido a miembros del equipo (Protección de Menores - US-21)
                    val isMember = checkTeamMembership(user, m)
                    if (!isMember) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            accessDenied = true,
                            accessDeniedReason = "Acceso restringido: Solo el Director Técnico, Jugadores y Padres vinculados al equipo pueden ver el partido en vivo para proteger la privacidad de los menores."
                        )
                        return@collect
                    }

                    _uiState.value = _uiState.value.copy(
                        match = m,
                        isLoading = false,
                        isOffline = matchFromCache || _uiState.value.isOffline
                    )
                }
            }

            // 2. Observar eventos del partido en tiempo real (snapshot listener)
            viewModelScope.launch {
                MatchRepository.observeMatchEvents(matchId).collect { (eventList, eventsFromCache) ->
                    // Detectar nuevos eventos de Gol o Tarjeta Roja para disparar animación destacada (US-21)
                    val activeEvents = eventList.filter { it.status == EventStatus.ACTIVE }
                    val newGoalOrRed = activeEvents.firstOrNull { ev ->
                        !previousEventIds.contains(ev.id) &&
                                (ev.type == EventType.GOAL || ev.type == EventType.RED_CARD)
                    }

                    previousEventIds = activeEvents.map { it.id }.toSet()

                    _uiState.value = _uiState.value.copy(
                        events = eventList,
                        isOffline = eventsFromCache || _uiState.value.isOffline
                    )

                    if (newGoalOrRed != null) {
                        triggerHighlightAnimation(newGoalOrRed)
                    }
                }
            }

            // 3. Ticker del Cronómetro en tiempo real
            startTimerTicker()
        }
    }

    private suspend fun checkTeamMembership(user: UserModel, match: MatchModel): Boolean {
        if (user.roleEnum == Role.DT) {
            return user.uid == match.dtUid || user.uid == match.operatorUid
        } else if (user.roleEnum == Role.JUG) {
            val pRes = AttendanceRepository.getPlayerByUserUid(user.uid)
            val p = pRes.getOrNull()
            return p != null && p.teamId == match.teamId
        } else if (user.roleEnum == Role.PAD) {
            val cRes = AttendanceRepository.getChildrenByParentUid(user.uid)
            val children = cRes.getOrNull().orEmpty()
            return children.any { it.teamId == match.teamId }
        }
        return false
    }

    private fun startTimerTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val m = _uiState.value.match
                if (m != null) {
                    val sec = m.calculatedElapsedSeconds
                    val min = m.calculatedCurrentMinute
                    _uiState.value = _uiState.value.copy(
                        elapsedSeconds = sec,
                        currentMinute = min
                    )
                }
                delay(1000L)
            }
        }
    }

    private fun triggerHighlightAnimation(event: MatchEventModel) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(animatedEvent = event)
            delay(4000L) // Duración de la animación destacada de Gol/Roja
            _uiState.value = _uiState.value.copy(animatedEvent = null)
        }
    }

    fun dismissAnimation() {
        _uiState.value = _uiState.value.copy(animatedEvent = null)
    }
}
