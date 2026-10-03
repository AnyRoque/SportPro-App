package pe.edu.esan.sportpro.presentation.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.LinkStatus
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.remote.PlayerRepository
import pe.edu.esan.sportpro.data.remote.TeamRepository
import pe.edu.esan.sportpro.util.FormValidators

data class PlayerFormState(
    val isEdit: Boolean = false,
    val name: String = "",
    val lastName: String = "",
    val birthDate: String = "",
    val dni: String = "",
    val category: String = "",
    val position: String = "",
    val shirtNumber: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val foot: String = "",
    val phone: String = "",
    val email: String = "",
    val emergencyName: String = "",
    val emergencyRelation: String = "",
    val emergencyPhone: String = "",
    val errors: Map<String, String> = emptyMap(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val message: String? = null
)

/** US-04: crear o editar el perfil de un jugador del equipo. */
class PlayerFormViewModel : ViewModel() {

    private val _state = MutableStateFlow(PlayerFormState())
    val state: StateFlow<PlayerFormState> = _state.asStateFlow()

    private var team: TeamModel? = null
    private var original: PlayerModel? = null
    private var usedNumbers: Set<Int> = emptySet()
    private var started = false

    fun load(teamId: String, playerId: String?, dtUid: String) {
        if (started) return
        started = true
        viewModelScope.launch {
            team = TeamRepository.getTeam(teamId).getOrNull()
            val teamPlayers = PlayerRepository.observePlayersByTeam(teamId, dtUid).first()
            original = playerId?.let { id -> teamPlayers.firstOrNull { it.id == id } }
            usedNumbers = teamPlayers.filter { it.id != playerId && it.active }.mapNotNull { it.shirtNumber }.toSet()

            val p = original
            _state.value = if (p == null) {
                PlayerFormState(loading = false, category = team?.category ?: "")
            } else {
                PlayerFormState(
                    isEdit = true, loading = false,
                    name = p.name, lastName = p.lastName, birthDate = p.birthDate, dni = p.dni,
                    category = p.category, position = p.position,
                    shirtNumber = p.shirtNumber?.toString() ?: "",
                    heightCm = p.heightCm?.toString() ?: "", weightKg = p.weightKg?.toString() ?: "",
                    foot = p.foot, phone = p.phone, email = p.email,
                    emergencyName = p.emergencyName, emergencyRelation = p.emergencyRelation,
                    emergencyPhone = p.emergencyPhone
                )
            }
        }
    }

    fun onChange(transform: (PlayerFormState) -> PlayerFormState) = _state.update(transform)

    fun save(dtUid: String) {
        val s = _state.value
        val errors = FormValidators.validatePlayer(
            s.name, s.lastName, s.birthDate, s.dni, s.category, s.position, s.shirtNumber,
            s.heightCm, s.weightKg, s.phone, s.email,
            s.emergencyName, s.emergencyRelation, s.emergencyPhone,
            usedShirtNumbers = usedNumbers, requireSportsData = true
        )
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors, message = "Revisa los campos marcados") }
            return
        }
        val t = team ?: run {
            _state.update { it.copy(message = "No se pudo cargar el equipo") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, errors = emptyMap(), message = null) }
            val base = original ?: PlayerModel(
                teamId = t.id, teamName = t.name, dtUid = dtUid,
                linkStatus = LinkStatus.ACCEPTED, createdBy = dtUid
            )
            val player = base.copy(
                name = s.name.trim(), lastName = s.lastName.trim(), birthDate = s.birthDate,
                dni = if (original != null) base.dni else s.dni.trim(),
                category = s.category, position = s.position,
                shirtNumber = s.shirtNumber.trim().toIntOrNull(),
                heightCm = s.heightCm.trim().toIntOrNull(), weightKg = s.weightKg.trim().toIntOrNull(),
                foot = s.foot, phone = s.phone.trim(), email = s.email.trim(),
                emergencyName = s.emergencyName.trim(), emergencyRelation = s.emergencyRelation,
                emergencyPhone = s.emergencyPhone.trim()
            )
            val result = if (original == null) PlayerRepository.createPlayer(player).map { }
                         else PlayerRepository.updatePlayer(player)
            result
                .onSuccess { _state.update { it.copy(saving = false, saved = true) } }
                .onFailure { e -> _state.update { it.copy(saving = false, message = e.message) } }
        }
    }
}
