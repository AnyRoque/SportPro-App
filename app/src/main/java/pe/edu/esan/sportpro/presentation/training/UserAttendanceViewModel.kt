package pe.edu.esan.sportpro.presentation.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.AttendanceStatus
import pe.edu.esan.sportpro.data.model.PlayerAttendanceRecord
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.AttendanceRepository
import pe.edu.esan.sportpro.util.Validators

data class UserAttendanceUiState(
    val loading: Boolean = true,
    val user: UserModel? = null,
    val children: List<PlayerModel> = emptyList(),
    val selectedChild: PlayerModel? = null,
    val selectedPlayer: PlayerModel? = null,
    val allRecords: List<PlayerAttendanceRecord> = emptyList(),
    val filterMonth: String = "ALL", // "ALL", "01", "02", ..., "12"
    val filterYear: String = "ALL", // "ALL", "2026", "2025"
    val errorMessage: String? = null
) {
    val filteredRecords: List<PlayerAttendanceRecord>
        get() = allRecords.filter { record ->
            val dateObj = Validators.parseDate(record.date)
            val monthMatch = if (filterMonth == "ALL") true else {
                val m = dateObj?.monthValue ?: 0
                val targetM = filterMonth.toIntOrNull() ?: 0
                m == targetM
            }
            val yearMatch = if (filterYear == "ALL") true else {
                val y = dateObj?.year ?: 0
                val targetY = filterYear.toIntOrNull() ?: 0
                y == targetY
            }
            monthMatch && yearMatch
        }

    val totalSessions: Int get() = filteredRecords.size
    val attendedCount: Int get() = filteredRecords.count { it.status == AttendanceStatus.ATTENDED }
    val lateCount: Int get() = filteredRecords.count { it.status == AttendanceStatus.LATE }
    val absentCount: Int get() = filteredRecords.count { it.status == AttendanceStatus.ABSENT }
    val justifiedCount: Int get() = filteredRecords.count { it.status == AttendanceStatus.JUSTIFIED }

    /**
     * Fórmula de compromiso: ((Asistió + Tardanza) / Total de sesiones) * 100
     */
    val attendancePercentage: Double
        get() = if (totalSessions == 0) 0.0 else ((attendedCount + lateCount).toDouble() / totalSessions.toDouble()) * 100.0
}

class UserAttendanceViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UserAttendanceUiState())
    val uiState: StateFlow<UserAttendanceUiState> = _uiState.asStateFlow()

    private var historyJob: Job? = null

    fun loadForUser(user: UserModel) {
        viewModelScope.launch {
            _uiState.value = UserAttendanceUiState(loading = true, user = user)

            if (user.roleEnum == Role.PAD) {
                // Rol Padre de Familia (PAD): consultar hijos vinculados
                val childrenResult = AttendanceRepository.getChildrenByParentUid(user.uid)
                val childrenList = childrenResult.getOrDefault(emptyList())

                if (childrenList.isEmpty()) {
                    _uiState.value = UserAttendanceUiState(
                        loading = false,
                        user = user,
                        errorMessage = "No tienes hijos vinculados a tu cuenta."
                    )
                    return@launch
                }

                val firstChild = childrenList.first()
                _uiState.value = UserAttendanceUiState(
                    loading = false,
                    user = user,
                    children = childrenList,
                    selectedChild = firstChild,
                    selectedPlayer = firstChild
                )
                observeHistoryForPlayer(firstChild.id)

            } else {
                // Rol Jugador (JUG): consultar su propio perfil vinculado
                val playerResult = AttendanceRepository.getPlayerByUserUid(user.uid)
                val player = playerResult.getOrNull()

                if (player == null) {
                    _uiState.value = UserAttendanceUiState(
                        loading = false,
                        user = user,
                        errorMessage = "No se encontró un perfil de jugador vinculado a tu cuenta."
                    )
                    return@launch
                }

                _uiState.value = UserAttendanceUiState(
                    loading = false,
                    user = user,
                    selectedPlayer = player
                )
                observeHistoryForPlayer(player.id)
            }
        }
    }

    fun selectChild(child: PlayerModel) {
        _uiState.value = _uiState.value.copy(
            selectedChild = child,
            selectedPlayer = child,
            loading = true
        )
        observeHistoryForPlayer(child.id)
    }

    fun setFilterMonth(month: String) {
        _uiState.value = _uiState.value.copy(filterMonth = month)
    }

    fun setFilterYear(year: String) {
        _uiState.value = _uiState.value.copy(filterYear = year)
    }

    private fun observeHistoryForPlayer(playerId: String) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            AttendanceRepository.observePlayerAttendanceHistory(playerId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(loading = false, errorMessage = e.message)
                }
                .collect { list ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        allRecords = list
                    )
                }
        }
    }
}
