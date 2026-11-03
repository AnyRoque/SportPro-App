package pe.edu.esan.sportpro.presentation.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.CallUpItemModel
import pe.edu.esan.sportpro.data.model.CallUpStatus
import pe.edu.esan.sportpro.data.model.EventStatus
import pe.edu.esan.sportpro.data.model.EventType
import pe.edu.esan.sportpro.data.model.LineupModel
import pe.edu.esan.sportpro.data.model.MatchEventModel
import pe.edu.esan.sportpro.data.model.MatchModel
import pe.edu.esan.sportpro.data.model.MatchStatus
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.AttendanceRepository
import pe.edu.esan.sportpro.data.remote.MatchRepository
import pe.edu.esan.sportpro.data.remote.PlayerRepository

data class PlayerPitchState(
    val playerId: String,
    val playerName: String,
    val shirtNumber: Int? = null,
    val positionName: String = "",
    val isOnPitch: Boolean = true,
    val isExpelled: Boolean = false,
    val yellowCardCount: Int = 0
)

/**
 * ViewModel para la Consola de Registro de Eventos en Tiempo Real (US-19 / US-20).
 */
class MatchConsoleViewModel : ViewModel() {

    private val _match = MutableStateFlow<MatchModel?>(null)
    val match: StateFlow<MatchModel?> = _match.asStateFlow()

    private val _lineup = MutableStateFlow<LineupModel?>(null)
    val lineup: StateFlow<LineupModel?> = _lineup.asStateFlow()

    private val _events = MutableStateFlow<List<MatchEventModel>>(emptyList())
    val events: StateFlow<List<MatchEventModel>> = _events.asStateFlow()

    private val _confirmedCallUps = MutableStateFlow<List<CallUpItemModel>>(emptyList())
    val confirmedCallUps: StateFlow<List<CallUpItemModel>> = _confirmedCallUps.asStateFlow()

    private val _pitchPlayers = MutableStateFlow<List<PlayerPitchState>>(emptyList())
    val pitchPlayers: StateFlow<List<PlayerPitchState>> = _pitchPlayers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    val noLineupError = MutableStateFlow(false)
    val accessDenied = MutableStateFlow(false)

    val elapsedSeconds = MutableStateFlow(0L)
    val currentMinute = MutableStateFlow(1)

    private var timerJob: Job? = null

    fun loadConsole(matchId: String, user: UserModel) {
        viewModelScope.launch {
            _isLoading.value = true
            noLineupError.value = false
            accessDenied.value = false

            val matchRes = MatchRepository.getMatch(matchId)
            matchRes.onSuccess { m ->
                _match.value = m

                // 1. Validar Control de Acceso (Solo DTs de los equipos involucrados u Operador asignado)
                val isAuthorized = (user.uid == m.dtUid || user.uid == m.operatorUid || (user.roleEnum == Role.DT && m.memberUids.contains(user.uid)))
                if (!isAuthorized) {
                    accessDenied.value = true
                    _isLoading.value = false
                    return@onSuccess
                }

                // 2. Validar que exista alineación guardada
                if (!m.hasLineup) {
                    noLineupError.value = true
                    _isLoading.value = false
                    return@onSuccess
                }

                // Cargar alineación inicial
                viewModelScope.launch {
                    MatchRepository.observeLineup(matchId).collect { savedLineup ->
                        if (savedLineup == null || savedLineup.positions.isEmpty()) {
                            noLineupError.value = true
                        } else {
                            _lineup.value = savedLineup
                            noLineupError.value = false
                            rebuildPitchState()
                        }
                    }
                }

                // Cargar convocatorias confirmadas
                viewModelScope.launch {
                    MatchRepository.observeCallUpsByMatch(matchId).collect { items ->
                        _confirmedCallUps.value = items.filter { it.computedStatus == CallUpStatus.CONFIRMED }
                        rebuildPitchState()
                    }
                }

                // Cargar eventos en tiempo real
                viewModelScope.launch {
                    MatchRepository.observeMatchEvents(matchId).collect { (eventList, _) ->
                        _events.value = eventList
                        rebuildPitchState()
                    }
                }

                // Iniciar motor de cronómetro en segundo plano
                startTimerTicker(user)

            }.onFailure { e ->
                _error.value = e.message
            }
            _isLoading.value = false
        }
    }

    private fun startTimerTicker(user: UserModel) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val m = _match.value
                if (m != null) {
                    val sec = m.calculatedElapsedSeconds
                    elapsedSeconds.value = sec
                    val min = m.calculatedCurrentMinute
                    currentMinute.value = min

                    // 1. Inicio automático a la hora programada (si el partido no se ha iniciado de forma manual)
                    if (m.status == MatchStatus.PROGRAMMED && m.hasLineup) {
                        try {
                            val pDate = pe.edu.esan.sportpro.util.Validators.parseDate(m.date)
                            val pTime = try {
                                java.time.LocalTime.parse(m.time.trim(), java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                            } catch (_: Exception) {
                                null
                            }
                            if (pDate != null && pTime != null) {
                                val scheduledDateTime = java.time.LocalDateTime.of(pDate, pTime)
                                if (!java.time.LocalDateTime.now().isBefore(scheduledDateTime)) {
                                    startMatch(user)
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    // 2. Finalización automática del 1° Tiempo al cumplir los minutos por tiempo
                    val period1LimitSeconds = (m.periodMinutes * 60).toLong()
                    if (m.status == MatchStatus.IN_PROGRESS && m.currentPeriod == 1 && m.timerIsRunning && sec >= period1LimitSeconds) {
                        endFirstHalf(user)
                    }

                    // 3. Finalización automática del 2° Tiempo al cumplir los minutos totales
                    val period2LimitSeconds = (m.periodMinutes * 2 * 60).toLong()
                    if (m.status == MatchStatus.IN_PROGRESS && m.currentPeriod == 2 && m.timerIsRunning && sec >= period2LimitSeconds) {
                        if (m.ownScore == m.rivalScore) {
                            pauseTimer()
                            _infoMessage.value = "¡Fin de los 90 minutos en EMPATE (${m.ownScore}-${m.rivalScore})! Opción de Tiempo Extra habilitada."
                        } else {
                            finishMatch(user)
                        }
                    }
                }
                delay(1000L)
            }
        }
    }

    private fun rebuildPitchState() {
        val l = _lineup.value ?: return
        val activeEvents = _events.value.filter { it.status == EventStatus.ACTIVE }

        // Mapear titulares iniciales
        val starters = l.positions.filter { !it.playerId.isNullOrBlank() }
        val starterIds = starters.mapNotNull { it.playerId }.toSet()

        val playerStatesMap = mutableMapOf<String, PlayerPitchState>()

        // 1. Inicializar titulares en campo
        starters.forEach { pos ->
            val pId = pos.playerId!!
            playerStatesMap[pId] = PlayerPitchState(
                playerId = pId,
                playerName = pos.playerName ?: "Jugador",
                shirtNumber = pos.shirtNumber,
                positionName = pos.positionName,
                isOnPitch = true,
                isExpelled = false,
                yellowCardCount = 0
            )
        }

        // 2. Inicializar suplentes en banca
        _confirmedCallUps.value.forEach { callUp ->
            if (!starterIds.contains(callUp.playerId)) {
                playerStatesMap[callUp.playerId] = PlayerPitchState(
                    playerId = callUp.playerId,
                    playerName = callUp.playerName,
                    positionName = callUp.position.ifBlank { "Suplente" },
                    isOnPitch = false,
                    isExpelled = false,
                    yellowCardCount = 0
                )
            }
        }

        // 3. Procesar eventos en orden para calcular sustituciones y tarjetas
        activeEvents.forEach { ev ->
            when (ev.type) {
                EventType.SUBSTITUTION -> {
                    // Jugador que sale va a la banca
                    ev.jugadorSaleId?.let { saleId ->
                        playerStatesMap[saleId]?.let { prev ->
                            playerStatesMap[saleId] = prev.copy(isOnPitch = false)
                        }
                    }
                    // Jugador que entra ingresa al campo
                    ev.jugadorEntraId?.let { entraId ->
                        playerStatesMap[entraId]?.let { prev ->
                            playerStatesMap[entraId] = prev.copy(isOnPitch = true)
                        }
                    }
                }
                EventType.YELLOW_CARD -> {
                    ev.playerId?.let { pId ->
                        playerStatesMap[pId]?.let { prev ->
                            val newYellows = prev.yellowCardCount + 1
                            val isExpelled = newYellows >= 2 || prev.isExpelled
                            playerStatesMap[pId] = prev.copy(
                                yellowCardCount = newYellows,
                                isExpelled = isExpelled,
                                isOnPitch = if (isExpelled) false else prev.isOnPitch
                            )
                        }
                    }
                }
                EventType.RED_CARD -> {
                    ev.playerId?.let { pId ->
                        playerStatesMap[pId]?.let { prev ->
                            playerStatesMap[pId] = prev.copy(
                                isExpelled = true,
                                isOnPitch = false
                            )
                        }
                    }
                }
            }
        }

        _pitchPlayers.value = playerStatesMap.values.toList()
    }

    // Controles del Partido
    fun startMatch(user: UserModel) {
        val m = _match.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = MatchRepository.startMatchTimer(m.id, user)
            _isLoading.value = false
            res.onFailure { _error.value = it.message }
        }
    }

    fun pauseTimer() {
        val m = _match.value ?: return
        viewModelScope.launch {
            val res = MatchRepository.pauseMatchTimer(m.id, elapsedSeconds.value)
            res.onFailure { _error.value = it.message }
        }
    }

    fun resumeTimer() {
        val m = _match.value ?: return
        viewModelScope.launch {
            val res = MatchRepository.resumeMatchTimer(m.id)
            res.onFailure { _error.value = it.message }
        }
    }

    fun endFirstHalf(user: UserModel) {
        val m = _match.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = MatchRepository.endFirstHalf(m.id, elapsedSeconds.value, currentMinute.value, user)
            _isLoading.value = false
            res.onFailure { _error.value = it.message }
        }
    }

    fun startSecondHalf(user: UserModel) {
        val m = _match.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = MatchRepository.startSecondHalf(m.id, m.periodMinutes, user)
            _isLoading.value = false
            res.onFailure { _error.value = it.message }
        }
    }

    fun startExtraTime(user: UserModel) {
        val m = _match.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = MatchRepository.startExtraTime(m.id, m.periodMinutes, user)
            _isLoading.value = false
            res.onFailure { _error.value = it.message }
        }
    }

    fun finishMatch(user: UserModel) {
        val m = _match.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = MatchRepository.finishMatch(m.id, elapsedSeconds.value, currentMinute.value, user)
            _isLoading.value = false
            res.onFailure { _error.value = it.message }
        }
    }

    // Registro de Evento
    fun registerEvent(
        user: UserModel,
        type: String,
        team: String,
        minute: Int,
        manualMinute: Boolean,
        playerId: String?,
        playerName: String,
        isUnidentified: Boolean,
        jugadorSaleId: String?,
        jugadorSaleName: String,
        jugadorEntraId: String?,
        jugadorEntraName: String,
        observation: String,
        onDone: () -> Unit
    ) {
        val m = _match.value ?: return

        // Validaciones
        if (type == EventType.SUBSTITUTION) {
            if (jugadorSaleId.isNullOrBlank() || jugadorEntraId.isNullOrBlank()) {
                _error.value = "Para un cambio debes seleccionar el jugador que sale y el que entra"
                return
            }
            if (jugadorSaleId == jugadorEntraId) {
                _error.value = "El jugador que entra no puede ser el mismo que sale"
                return
            }
        }

        // Verificar si el jugador ya está expulsado
        if (playerId != null && type != EventType.SUBSTITUTION) {
            val pState = _pitchPlayers.value.firstOrNull { it.playerId == playerId }
            if (pState != null && pState.isExpelled) {
                _error.value = "El jugador $playerName está expulsado y no puede recibir más eventos"
                return
            }
        }

        viewModelScope.launch {
            val eventToSave = MatchEventModel(
                matchId = m.id,
                type = type,
                minute = minute,
                minutoCorregido = manualMinute,
                team = team,
                playerId = if (isUnidentified) null else playerId,
                playerName = if (isUnidentified) "Jugador no identificado" else playerName,
                jugadorNoIdentificado = isUnidentified,
                jugadorSaleId = jugadorSaleId,
                jugadorSaleName = jugadorSaleName,
                jugadorEntraId = jugadorEntraId,
                jugadorEntraName = jugadorEntraName,
                observation = observation.trim(),
                registeredBy = user.uid,
                registeredByName = user.fullName,
                status = EventStatus.ACTIVE
            )

            val res = MatchRepository.addMatchEvent(eventToSave)
            res.onSuccess {
                // Verificar si era 2ª tarjeta amarilla para auto-generar Tarjeta Roja
                if (type == EventType.YELLOW_CARD && playerId != null) {
                    val pState = _pitchPlayers.value.firstOrNull { it.playerId == playerId }
                    if (pState != null && pState.yellowCardCount >= 1) {
                        // Es la 2ª Tarjeta Amarilla -> Generar Roja automáticamente
                        val autoRedEvent = MatchEventModel(
                            matchId = m.id,
                            type = EventType.RED_CARD,
                            minute = minute,
                            minutoCorregido = manualMinute,
                            team = team,
                            playerId = playerId,
                            playerName = playerName,
                            observation = "Expulsión automática por 2ª Tarjeta Amarilla",
                            registeredBy = user.uid,
                            registeredByName = user.fullName,
                            status = EventStatus.ACTIVE
                        )
                        MatchRepository.addMatchEvent(autoRedEvent)
                        _infoMessage.value = "$playerName recibió su 2ª tarjeta amarilla y fue expulsado automáticamente."
                    }
                }
                onDone()
            }.onFailure { e ->
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearInfoMessage() {
        _infoMessage.value = null
    }

    // ================================================================
    // EDICIÓN Y ANULACIÓN DE EVENTOS CON TRAZABILIDAD (US-20)
    // ================================================================

    fun canEditEvents(): Boolean {
        val m = _match.value ?: return false
        return MatchRepository.isWithin24HoursOfFinish(m)
    }

    fun editEvent(
        user: UserModel,
        event: MatchEventModel,
        motivo: String,
        onDone: () -> Unit
    ) {
        val m = _match.value ?: return

        if (!MatchRepository.isWithin24HoursOfFinish(m)) {
            _error.value = "Han transcurrido más de 24 horas desde la finalización del partido. Los eventos están en modo solo lectura."
            return
        }

        if (motivo.isBlank()) {
            _error.value = "El motivo de la modificación es un campo obligatorio."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val res = MatchRepository.editMatchEvent(event, motivo, user)
            _isLoading.value = false
            res.onSuccess {
                _infoMessage.value = "Evento modificado correctamente y registrado en bitácora."
                onDone()
            }.onFailure { e ->
                _error.value = e.message
            }
        }
    }

    fun cancelEvent(
        user: UserModel,
        eventId: String,
        motivo: String,
        onDone: () -> Unit
    ) {
        val m = _match.value ?: return

        if (!MatchRepository.isWithin24HoursOfFinish(m)) {
            _error.value = "Han transcurrido más de 24 horas desde la finalización del partido. Los eventos están en modo solo lectura."
            return
        }

        if (motivo.isBlank()) {
            _error.value = "El motivo de la anulación es un campo obligatorio."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val res = MatchRepository.cancelMatchEvent(m.id, eventId, motivo, user)
            _isLoading.value = false
            res.onSuccess {
                _infoMessage.value = "Evento anulado correctamente y registrado en bitácora."
                onDone()
            }.onFailure { e ->
                _error.value = e.message
            }
        }
    }

    fun observeEventHistory(eventId: String) = MatchRepository.observeEventHistory(_match.value?.id ?: "", eventId)

    fun deleteEvent(
        user: UserModel,
        eventId: String,
        onDone: () -> Unit
    ) {
        val m = _match.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            val res = MatchRepository.deleteMatchEvent(m.id, eventId, user)
            _isLoading.value = false
            res.onSuccess {
                _infoMessage.value = "Evento eliminado permanentemente."
                onDone()
            }.onFailure { e ->
                _error.value = e.message
            }
        }
    }

    fun requestEventCancellationToAdmin(
        user: UserModel,
        event: MatchEventModel,
        justification: String,
        onDone: () -> Unit
    ) {
        val m = _match.value ?: return

        if (justification.isBlank()) {
            _error.value = "La justificación para la solicitud al Administrador es obligatoria."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val request = pe.edu.esan.sportpro.data.model.EventCancellationRequestModel(
                matchId = m.id,
                matchTitle = "${m.teamName} vs ${m.rival}",
                eventId = event.id,
                eventSummary = "Min ${event.minute}': ${EventType.labelOf(event.type)} - ${event.playerName}",
                requestingDtUid = user.uid,
                requestingDtName = user.fullName,
                requestingTeamName = m.teamName,
                justification = justification.trim(),
                status = pe.edu.esan.sportpro.data.model.RequestStatus.PENDING
            )

            val res = MatchRepository.requestEventCancellationToAdmin(request)
            _isLoading.value = false

            res.onSuccess {
                _infoMessage.value = "Solicitud de anulación enviada correctamente al Administrador."
                onDone()
            }.onFailure { e ->
                _error.value = e.message
            }
        }
    }
}
