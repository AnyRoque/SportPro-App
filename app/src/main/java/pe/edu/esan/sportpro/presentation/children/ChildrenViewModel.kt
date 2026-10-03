package pe.edu.esan.sportpro.presentation.children

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
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.PlayerRepository
import pe.edu.esan.sportpro.data.remote.TeamRepository
import pe.edu.esan.sportpro.util.FormValidators
import pe.edu.esan.sportpro.util.Validators

data class ChildrenListState(val loading: Boolean = true, val children: List<PlayerModel> = emptyList(), val error: String? = null)

/** US-02: lista "Mis hijos" del PAD (solo ve a sus propios hijos). */
class ChildrenListViewModel : ViewModel() {
    private val _state = MutableStateFlow(ChildrenListState())
    val state: StateFlow<ChildrenListState> = _state.asStateFlow()
    private var job: Job? = null

    fun start(parentUid: String) {
        if (job != null) return
        job = viewModelScope.launch {
            PlayerRepository.observeChildrenByParent(parentUid)
                .catch { e -> _state.value = ChildrenListState(loading = false, error = e.message) }
                .collect { _state.value = ChildrenListState(loading = false, children = it) }
        }
    }
}

data class ChildFormState(
    val name: String = "",
    val lastName: String = "",
    val birthDate: String = "",
    val dni: String = "",
    val sex: String = "",
    val relation: String = "",
    val teams: List<TeamModel> = emptyList(),
    val teamId: String = "",
    val category: String = "",
    val errors: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
    val savedCount: Int = 0,
    val finished: Boolean = false,
    val message: String? = null
)

/** US-02: registro de hijo y solicitud de vínculo al equipo (queda Pendiente hasta que el DT acepte). */
class ChildFormViewModel : ViewModel() {
    private val _state = MutableStateFlow(ChildFormState())
    val state: StateFlow<ChildFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Solo se muestran equipos aprobados (US-07)
            TeamRepository.getApprovedTeams()
                .onSuccess { list -> _state.update { it.copy(teams = list) } }
                .onFailure { e -> _state.update { it.copy(message = e.message) } }
        }
    }

    fun onChange(transform: (ChildFormState) -> ChildFormState) = _state.update(transform)

    /** Al elegir la fecha se sugiere la categoría según la edad. */
    fun onBirthDate(date: String) = _state.update {
        val suggested = Validators.suggestedCategory(date) ?: it.category
        it.copy(birthDate = date, category = suggested, teamId = if (suggested != it.category) "" else it.teamId)
    }

    fun save(parent: UserModel, addAnother: Boolean) {
        val s = _state.value
        val errors = FormValidators.validatePlayer(
            name = s.name, lastName = s.lastName, birthDate = s.birthDate, dni = s.dni,
            category = s.category, position = "", shirtNumber = "", heightCm = "", weightKg = "",
            phone = "", email = "",
            emergencyName = parent.fullName, emergencyRelation = s.relation, emergencyPhone = parent.phone,
            requireSportsData = false
        ).toMutableMap()
        if (!Validators.isValidDni(s.dni)) errors["dni"] = "El DNI debe tener 8 dígitos"
        if (s.sex.isBlank()) errors["sex"] = "Selecciona el sexo"
        if (s.relation.isBlank()) errors["relation"] = "Indica el parentesco"
        if (s.teamId.isBlank()) errors["teamId"] = "Selecciona el equipo"
        errors.remove("emergencyRelation"); errors.remove("emergencyName"); errors.remove("emergencyPhone")
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        val team = s.teams.first { it.id == s.teamId }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, errors = emptyMap(), message = null) }
            val child = PlayerModel(
                teamId = team.id, teamName = team.name, dtUid = team.dtUid, category = s.category,
                name = s.name.trim(), lastName = s.lastName.trim(), birthDate = s.birthDate,
                dni = s.dni, sex = s.sex,
                // El padre queda como contacto de emergencia por defecto
                emergencyName = parent.fullName, emergencyRelation = s.relation, emergencyPhone = parent.phone,
                parentUid = parent.uid, parentRelation = s.relation,
                linkStatus = LinkStatus.PENDING, createdBy = parent.uid
            )
            PlayerRepository.createPlayer(child)
                .onSuccess {
                    _state.update {
                        if (addAnother) ChildFormState(teams = it.teams, relation = it.relation, savedCount = it.savedCount + 1,
                            message = "Solicitud enviada al Director Técnico. Registra al siguiente hijo.")
                        else it.copy(saving = false, savedCount = it.savedCount + 1, finished = true)
                    }
                }
                .onFailure { e -> _state.update { it.copy(saving = false, message = e.message) } }
        }
    }
}
