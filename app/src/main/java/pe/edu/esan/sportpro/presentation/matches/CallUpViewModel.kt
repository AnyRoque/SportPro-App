package pe.edu.esan.sportpro.presentation.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.CallUpItemModel
import pe.edu.esan.sportpro.data.model.CallUpStatus
import pe.edu.esan.sportpro.data.model.MatchModel
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.AttendanceRepository
import pe.edu.esan.sportpro.data.remote.MatchRepository
import pe.edu.esan.sportpro.data.remote.PlayerRepository
import pe.edu.esan.sportpro.util.Validators
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * ViewModel para la gestión de Convocatoria y Disponibilidad (US-17).
 */
class CallUpViewModel : ViewModel() {

    private val _match = MutableStateFlow<MatchModel?>(null)
    val match: StateFlow<MatchModel?> = _match.asStateFlow()

    private val _activePlayers = MutableStateFlow<List<PlayerModel>>(emptyList())
    val activePlayers: StateFlow<List<PlayerModel>> = _activePlayers.asStateFlow()

    private val _callUpItems = MutableStateFlow<List<CallUpItemModel>>(emptyList())
    val callUpItems: StateFlow<List<CallUpItemModel>> = _callUpItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val deadlineError = MutableStateFlow<String?>(null)
    val minPlayersError = MutableStateFlow<String?>(null)

    val selectedPlayerIds = MutableStateFlow<Set<String>>(emptySet())
    val deadlineDate = MutableStateFlow("")
    val deadlineTime = MutableStateFlow("20:00")

    val userPlayerId = MutableStateFlow<String?>(null)
    val userChildrenIds = MutableStateFlow<Set<String>>(emptySet())

    fun loadUserProfiles(user: UserModel) {
        viewModelScope.launch {
            if (user.roleEnum == Role.JUG) {
                val pRes = AttendanceRepository.getPlayerByUserUid(user.uid)
                pRes.onSuccess { userPlayerId.value = it.id }
            } else if (user.roleEnum == Role.PAD) {
                val cRes = AttendanceRepository.getChildrenByParentUid(user.uid)
                cRes.onSuccess { list -> userChildrenIds.value = list.map { it.id }.toSet() }
            }
        }
    }

    fun loadMatchAndPlayers(matchId: String, currentUid: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val matchRes = MatchRepository.getMatch(matchId)
            matchRes.onSuccess { m ->
                _match.value = m
                if (deadlineDate.value.isBlank()) {
                    deadlineDate.value = m.date
                }

                val dtUidToQuery = m.dtUid.ifBlank { currentUid }
                viewModelScope.launch {
                    PlayerRepository.observePlayersByTeam(m.teamId, dtUidToQuery).collect { playersList ->
                        val activeList = playersList.filter { it.active }
                        _activePlayers.value = activeList
                    }
                }
            }.onFailure { e ->
                _error.value = e.message
            }
            _isLoading.value = false
        }

        // Observar convocatoria en tiempo real en corrutina independiente
        viewModelScope.launch {
            MatchRepository.observeCallUpsByMatch(matchId).collect { items ->
                val uniqueItems = items.distinctBy { it.playerId.ifBlank { it.playerName } }
                _callUpItems.value = uniqueItems
            }
        }
    }

    fun togglePlayerSelection(playerId: String) {
        val current = selectedPlayerIds.value.toMutableSet()
        if (current.contains(playerId)) {
            current.remove(playerId)
        } else {
            current.add(playerId)
        }
        selectedPlayerIds.value = current
        minPlayersError.value = null
    }

    fun selectAllPlayers() {
        selectedPlayerIds.value = _activePlayers.value.map { it.id }.toSet()
        minPlayersError.value = null
    }

    private fun validateCallUp(): Boolean {
        deadlineError.value = null
        minPlayersError.value = null
        _error.value = null

        val m = _match.value ?: return false
        val minRequired = m.modality

        if (selectedPlayerIds.value.size < minRequired) {
            minPlayersError.value = "Debes seleccionar al menos $minRequired jugadores (modalidad ${m.modality})"
            return false
        }

        val dDate = Validators.parseDate(deadlineDate.value)
        if (dDate == null) {
            deadlineError.value = "Selecciona una fecha válida (dd/mm/aaaa)"
            return false
        }

        val dTime = try {
            LocalTime.parse(deadlineTime.value.trim(), DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {
            null
        }

        if (dTime == null) {
            deadlineError.value = "Ingresa una hora válida (HH:mm)"
            return false
        }

        val deadlineDateTime = LocalDateTime.of(dDate, dTime)

        // Validar que sea posterior a la actual
        if (!deadlineDateTime.isAfter(LocalDateTime.now())) {
            deadlineError.value = "La fecha y hora límite deben ser posteriores a la actual"
            return false
        }

        // Validar que sea anterior al partido
        val mDate = Validators.parseDate(m.date)
        val mTime = try {
            LocalTime.parse(m.time.trim(), DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {
            LocalTime.MIDNIGHT
        }

        if (mDate != null) {
            val matchDateTime = LocalDateTime.of(mDate, mTime)
            if (!deadlineDateTime.isBefore(matchDateTime)) {
                deadlineError.value = "La fecha y hora límite deben ser anteriores al partido ($m.date $m.time hs)"
                return false
            }
        }

        return true
    }

    fun createCallUp(onDone: () -> Unit) {
        if (!validateCallUp()) return
        val m = _match.value ?: return
        val selectedPlayersList = _activePlayers.value.filter { selectedPlayerIds.value.contains(it.id) }

        viewModelScope.launch {
            _isLoading.value = true
            val result = MatchRepository.createCallUp(
                match = m,
                selectedPlayers = selectedPlayersList,
                deadlineDate = deadlineDate.value.trim(),
                deadlineTime = deadlineTime.value.trim()
            )
            _isLoading.value = false
            result.onSuccess {
                onDone()
            }.onFailure { e ->
                _error.value = e.message
            }
        }
    }

    fun addPlayersToCallUp(onDone: () -> Unit) {
        val m = _match.value ?: return
        val existingIds = _callUpItems.value.map { it.playerId }.toSet()
        val additionalPlayers = _activePlayers.value
            .filter { selectedPlayerIds.value.contains(it.id) && !existingIds.contains(it.id) }

        if (additionalPlayers.isEmpty()) {
            _error.value = "Selecciona al menos un jugador nuevo para agregar"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = MatchRepository.addPlayersToCallUp(m, additionalPlayers)
            _isLoading.value = false
            result.onSuccess {
                selectedPlayerIds.value = emptySet()
                onDone()
            }.onFailure { e ->
                _error.value = e.message
            }
        }
    }

    fun respondCallUp(
        matchId: String,
        playerId: String,
        status: String,
        reason: String = "",
        comment: String = "",
        onDone: () -> Unit
    ) {
        // Actualización optimista inmediata en la interfaz gráfica
        _callUpItems.value = _callUpItems.value.map { item ->
            if (item.playerId == playerId) {
                item.copy(
                    responseStatus = status,
                    rejectReason = reason.trim(),
                    rejectComment = comment.trim()
                )
            } else {
                item
            }
        }

        viewModelScope.launch {
            val result = MatchRepository.respondCallUp(matchId, playerId, status, reason, comment)
            result.onSuccess {
                onDone()
            }.onFailure { e ->
                _error.value = e.message
            }
        }
    }

    fun resendReminders(onDone: () -> Unit) {
        val m = _match.value ?: return
        val pendingList = _callUpItems.value.filter { it.computedStatus == CallUpStatus.PENDING }

        if (pendingList.isEmpty()) {
            _error.value = "No hay jugadores con convocatoria pendiente"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = MatchRepository.resendCallUpReminders(m, pendingList)
            _isLoading.value = false
            result.onSuccess {
                onDone()
            }.onFailure { e ->
                _error.value = e.message
            }
        }
    }

    fun updateFormation(matchId: String, formation: String) {
        viewModelScope.launch {
            MatchRepository.updateFormation(matchId, formation)
        }
    }

    fun toggleStarterStatus(matchId: String, playerId: String, currentStarter: Boolean) {
        _callUpItems.value = _callUpItems.value.map { item ->
            if (item.playerId == playerId) {
                item.copy(isStarter = !currentStarter)
            } else {
                item
            }
        }
        viewModelScope.launch {
            MatchRepository.updatePlayerStarterStatus(matchId, playerId, !currentStarter)
        }
    }

    fun clearError() {
        _error.value = null
    }
}
