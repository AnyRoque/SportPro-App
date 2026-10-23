package pe.edu.esan.sportpro.presentation.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.AttendanceRecordModel
import pe.edu.esan.sportpro.data.model.AttendanceStatus
import pe.edu.esan.sportpro.data.model.PlayerAttendanceItem
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.model.TrainingModel
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.AttendanceRepository
import pe.edu.esan.sportpro.data.remote.TrainingRepository
import pe.edu.esan.sportpro.util.Validators
import java.time.LocalDate

data class AttendanceUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val training: TrainingModel? = null,
    val players: List<PlayerModel> = emptyList(),
    val itemsMap: Map<String, PlayerAttendanceItem> = emptyMap(),
    val isFutureSession: Boolean = false,
    val isEditable: Boolean = true,
    val hasLastRecord: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val validationError: String? = null
) {
    val totalPlayers: Int get() = players.size
    val unmarkedCount: Int get() = itemsMap.values.count { it.status.isBlank() }
    val totalAttended: Int get() = itemsMap.values.count { it.status == AttendanceStatus.ATTENDED }
    val totalLate: Int get() = itemsMap.values.count { it.status == AttendanceStatus.LATE }
    val totalAbsent: Int get() = itemsMap.values.count { it.status == AttendanceStatus.ABSENT }
    val totalJustified: Int get() = itemsMap.values.count { it.status == AttendanceStatus.JUSTIFIED }

    val isComplete: Boolean get() = players.isNotEmpty() && itemsMap.values.none { item ->
        item.status.isBlank() || (item.status == AttendanceStatus.JUSTIFIED && item.justificationReason.isBlank())
    }
}

class AttendanceViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    fun loadData(trainingId: String, teamId: String, dtUid: String) {
        viewModelScope.launch {
            _uiState.value = AttendanceUiState(loading = true)

            // 1. Cargar Sesión de Entrenamiento
            val trainingResult = TrainingRepository.getTraining(trainingId)
            val training = trainingResult.getOrNull()

            if (training == null) {
                _uiState.value = AttendanceUiState(
                    loading = false,
                    errorMessage = "No se pudo cargar la información de la sesión."
                )
                return@launch
            }

            // Validar restricción de sesión futura
            val sessionDate = Validators.parseDate(training.date)
            val today = LocalDate.now()
            val isFuture = sessionDate != null && sessionDate.isAfter(today)

            // 2. Cargar Jugadores Activos del Equipo
            val effectiveTeamId = training.teamId.ifBlank { teamId }
            val playersResult = AttendanceRepository.getActivePlayersForTeam(effectiveTeamId, dtUid)
            val activePlayers = playersResult.getOrDefault(emptyList())

            if (activePlayers.isEmpty()) {
                _uiState.value = AttendanceUiState(
                    loading = false,
                    training = training,
                    isFutureSession = isFuture,
                    errorMessage = "El equipo no cuenta con jugadores activos registrados."
                )
                return@launch
            }

            // 3. Verificar si existe asistencia previa guardada para esta sesión
            val existingRecordResult = AttendanceRepository.getAttendanceRecord(trainingId)
            val existingRecord = existingRecordResult.getOrNull()

            val initialItemsMap = mutableMapOf<String, PlayerAttendanceItem>()

            if (existingRecord != null) {
                // Asistencia previa cargada
                existingRecord.items.forEach { item ->
                    initialItemsMap[item.playerId] = item
                }
                // Completar cualquier jugador nuevo que no estuviera en el registro anterior
                activePlayers.forEach { p ->
                    if (!initialItemsMap.containsKey(p.id)) {
                        initialItemsMap[p.id] = PlayerAttendanceItem(
                            playerId = p.id,
                            playerName = p.fullName,
                            photoUrl = p.photoUrl,
                            shirtNumber = p.shirtNumber,
                            position = p.position,
                            status = "",
                            justificationReason = ""
                        )
                    }
                }
            } else {
                // Registro nuevo sin marcar
                activePlayers.forEach { p ->
                    initialItemsMap[p.id] = PlayerAttendanceItem(
                        playerId = p.id,
                        playerName = p.fullName,
                        photoUrl = p.photoUrl,
                        shirtNumber = p.shirtNumber,
                        position = p.position,
                        status = "",
                        justificationReason = ""
                    )
                }
            }

            // 4. Verificar si se puede editar (dentro de las 48 horas)
            val canEdit = if (existingRecord != null) AttendanceRepository.canEditAttendance(training.date) else true

            // 5. Verificar si hay un registro previo del equipo para copiar
            val lastRecordResult = AttendanceRepository.getLastAttendanceRecordForTeam(effectiveTeamId)
            val hasLastRecord = lastRecordResult.getOrNull() != null

            _uiState.value = AttendanceUiState(
                loading = false,
                training = training,
                players = activePlayers,
                itemsMap = initialItemsMap,
                isFutureSession = isFuture,
                isEditable = canEdit && !isFuture,
                hasLastRecord = hasLastRecord
            )
        }
    }

    fun setPlayerStatus(playerId: String, status: String, justificationReason: String = "") {
        val currentMap = _uiState.value.itemsMap.toMutableMap()
        val item = currentMap[playerId] ?: return

        currentMap[playerId] = item.copy(
            status = status,
            justificationReason = if (status == AttendanceStatus.JUSTIFIED) justificationReason else ""
        )

        _uiState.value = _uiState.value.copy(
            itemsMap = currentMap,
            validationError = null
        )
    }

    fun markAllAsAttended() {
        val currentMap = _uiState.value.itemsMap.toMutableMap()
        currentMap.keys.forEach { pId ->
            val existing = currentMap[pId]
            if (existing != null) {
                currentMap[pId] = existing.copy(
                    status = AttendanceStatus.ATTENDED,
                    justificationReason = ""
                )
            }
        }
        _uiState.value = _uiState.value.copy(
            itemsMap = currentMap,
            validationError = null,
            infoMessage = "Todos los jugadores fueron marcados como 'Asistió'."
        )
    }

    fun copyLastSessionAttendance() {
        val state = _uiState.value
        val teamId = state.training?.teamId ?: return

        viewModelScope.launch {
            val lastResult = AttendanceRepository.getLastAttendanceRecordForTeam(teamId)
            val lastRecord = lastResult.getOrNull()

            if (lastRecord == null || lastRecord.items.isEmpty()) {
                _uiState.value = state.copy(errorMessage = "No hay registros de asistencia anteriores para copiar.")
                return@launch
            }

            val lastMap = lastRecord.items.associateBy { it.playerId }
            val updatedMap = state.itemsMap.toMutableMap()

            updatedMap.keys.forEach { pId ->
                val lastItem = lastMap[pId]
                val currentItem = updatedMap[pId]
                if (currentItem != null && lastItem != null && lastItem.status.isNotBlank()) {
                    updatedMap[pId] = currentItem.copy(
                        status = lastItem.status,
                        justificationReason = lastItem.justificationReason
                    )
                }
            }

            _uiState.value = state.copy(
                itemsMap = updatedMap,
                validationError = null,
                infoMessage = "Asistencia copiada de la última sesión."
            )
        }
    }

    fun saveAttendance(dtUser: UserModel, onSuccess: () -> Unit) {
        val state = _uiState.value
        val training = state.training ?: return

        if (state.isFutureSession) {
            _uiState.value = state.copy(validationError = "No se puede tomar asistencia en sesiones futuras.")
            return
        }

        // Validación de completitud
        val unmarked = state.unmarkedCount
        if (unmarked > 0) {
            _uiState.value = state.copy(validationError = "Faltan $unmarked jugador(es) por calificar.")
            return
        }

        // Validación de motivo de justificados
        val unjustifiedList = state.itemsMap.values.filter {
            it.status == AttendanceStatus.JUSTIFIED && it.justificationReason.isBlank()
        }
        if (unjustifiedList.isNotEmpty()) {
            _uiState.value = state.copy(validationError = "Debes ingresar el motivo de justificación para ${unjustifiedList.first().playerName}.")
            return
        }

        _uiState.value = state.copy(saving = true)

        viewModelScope.launch {
            val record = AttendanceRecordModel(
                id = training.id,
                trainingId = training.id,
                teamId = training.teamId,
                teamName = training.teamName,
                category = training.category,
                dtUid = dtUser.uid,
                dtName = dtUser.fullName,
                date = training.date,
                items = state.itemsMap.values.toList(),
                totalAttended = state.totalAttended,
                totalLate = state.totalLate,
                totalAbsent = state.totalAbsent,
                totalJustified = state.totalJustified,
                editedBy = dtUser.fullName
            )

            val result = AttendanceRepository.saveAttendanceRecord(record)
            _uiState.value = _uiState.value.copy(saving = false)

            result.onSuccess {
                onSuccess()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Error al guardar la asistencia.")
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = null, validationError = null)
    }
}
