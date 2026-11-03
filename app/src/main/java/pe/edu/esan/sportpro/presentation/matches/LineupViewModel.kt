package pe.edu.esan.sportpro.presentation.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.CallUpItemModel
import pe.edu.esan.sportpro.data.model.CallUpStatus
import pe.edu.esan.sportpro.data.model.LineupConstants
import pe.edu.esan.sportpro.data.model.LineupModel
import pe.edu.esan.sportpro.data.model.MatchModel
import pe.edu.esan.sportpro.data.model.MatchStatus
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.TacticalLayouts
import pe.edu.esan.sportpro.data.model.TacticalPosition
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.MatchRepository

/**
 * ViewModel para la gestión de la Alineación y Formación Táctica (US-18).
 */
class LineupViewModel : ViewModel() {

    private val _match = MutableStateFlow<MatchModel?>(null)
    val match: StateFlow<MatchModel?> = _match.asStateFlow()

    private val _confirmedCallUps = MutableStateFlow<List<CallUpItemModel>>(emptyList())
    val confirmedCallUps: StateFlow<List<CallUpItemModel>> = _confirmedCallUps.asStateFlow()

    private val _positions = MutableStateFlow<List<TacticalPosition>>(emptyList())
    val positions: StateFlow<List<TacticalPosition>> = _positions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isEditable = MutableStateFlow(false)
    val isEditable: StateFlow<Boolean> = _isEditable.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val validationError = MutableStateFlow<String?>(null)
    val formation = MutableStateFlow("4-3-3")

    fun load(matchId: String, user: UserModel) {
        viewModelScope.launch {
            _isLoading.value = true
            val matchRes = MatchRepository.getMatch(matchId)
            matchRes.onSuccess { m ->
                _match.value = m
                _isEditable.value = (user.roleEnum == Role.DT && m.status == MatchStatus.PROGRAMMED)

                val defaultForm = m.formation.ifBlank {
                    if (m.modality == 7) "2-3-1" else if (m.modality == 9) "3-3-2" else "4-3-3"
                }
                formation.value = defaultForm

                // Cargar alineación guardada de Firestore combinada inmediatamente con el diseño táctico
                viewModelScope.launch {
                    MatchRepository.observeLineup(matchId).collect { savedLineup ->
                        val activeForm = savedLineup?.formation?.ifBlank { defaultForm } ?: defaultForm
                        formation.value = activeForm

                        val layoutPositions = TacticalLayouts.generateDefaultPositions(activeForm, m.modality)
                        val savedMap = savedLineup?.positions.orEmpty().associateBy { it.slotIndex }

                        val mergedPositions = layoutPositions.map { layoutPos ->
                            val saved = savedMap[layoutPos.slotIndex]
                            if (saved != null && !saved.playerId.isNullOrBlank()) {
                                layoutPos.copy(
                                    playerId = saved.playerId,
                                    playerName = saved.playerName,
                                    shirtNumber = saved.shirtNumber
                                )
                            } else {
                                layoutPos
                            }
                        }

                        _positions.value = mergedPositions
                    }
                }

                // Observar solo convocatorias con respuesta CONFIRMADO
                viewModelScope.launch {
                    MatchRepository.observeCallUpsByMatch(matchId).collect { items ->
                        _confirmedCallUps.value = items.filter { it.computedStatus == CallUpStatus.CONFIRMED }
                    }
                }

            }.onFailure { e ->
                _error.value = e.message
            }
            _isLoading.value = false
        }
    }

    fun changeFormation(newFormation: String) {
        val m = _match.value ?: return
        formation.value = newFormation
        val newLayout = TacticalLayouts.generateDefaultPositions(newFormation, m.modality)

        // Preservar jugadores asignados si los slots coinciden
        val currentMap = _positions.value.associateBy { it.slotIndex }
        val updated = newLayout.map { newPos ->
            val prev = currentMap[newPos.slotIndex]
            if (prev != null && prev.playerId != null) {
                newPos.copy(
                    playerId = prev.playerId,
                    playerName = prev.playerName,
                    shirtNumber = prev.shirtNumber
                )
            } else {
                newPos
            }
        }
        _positions.value = updated
        validationError.value = null
    }

    fun assignPlayerToPosition(slotIndex: Int, player: CallUpItemModel) {
        validationError.value = null

        // Verificar si el jugador ya está asignado a OTRA posición en la cancha
        val existingSlot = _positions.value.firstOrNull { it.playerId == player.playerId }
        if (existingSlot != null && existingSlot.slotIndex != slotIndex) {
            validationError.value = "Un jugador no puede ocupar dos posiciones"
            return
        }

        // Asignar al slot indicado
        val updated = _positions.value.map { pos ->
            if (pos.slotIndex == slotIndex) {
                pos.copy(
                    playerId = player.playerId,
                    playerName = player.playerName,
                    shirtNumber = null
                )
            } else {
                pos
            }
        }
        _positions.value = updated
    }

    fun unassignPlayerFromPosition(slotIndex: Int) {
        validationError.value = null
        val updated = _positions.value.map { pos ->
            if (pos.slotIndex == slotIndex) {
                pos.copy(playerId = null, playerName = null, shirtNumber = null)
            } else {
                pos
            }
        }
        _positions.value = updated
    }

    fun saveLineup(user: UserModel, onDone: () -> Unit) {
        validationError.value = null
        val m = _match.value ?: return
        val requiredStarters = m.modality
        val assignedStarters = _positions.value.count { !it.playerId.isNullOrBlank() }

        // Validar número exacto de titulares
        if (assignedStarters < requiredStarters) {
            val missing = requiredStarters - assignedStarters
            validationError.value = "Faltan $missing titulares para completar la alineación (requerido: $requiredStarters)"
            return
        }

        // Validar portero asignado
        val gkAssigned = _positions.value.any { pos ->
            !pos.playerId.isNullOrBlank() && (pos.positionName.contains("Portero", ignoreCase = true) || pos.slotIndex == 0)
        }
        if (!gkAssigned) {
            validationError.value = "Debe asignar un portero"
            return
        }

        // Validar límite máximo de suplentes (máximo 7)
        val starterPlayerIds = _positions.value.mapNotNull { it.playerId }.toSet()
        val substitutePlayerIds = _confirmedCallUps.value
            .map { it.playerId }
            .filter { !starterPlayerIds.contains(it) }

        if (substitutePlayerIds.size > LineupConstants.MAX_SUBSTITUTES) {
            validationError.value = "El máximo de suplentes permitido es ${LineupConstants.MAX_SUBSTITUTES} (tienes ${substitutePlayerIds.size})"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            val lineupToSave = LineupModel(
                id = "starting",
                matchId = m.id,
                formation = formation.value,
                modality = m.modality,
                positions = _positions.value,
                substitutePlayerIds = substitutePlayerIds,
                savedBy = user.uid
            )

            val result = MatchRepository.saveLineup(m.id, lineupToSave)
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
        validationError.value = null
    }
}
