package pe.edu.esan.sportpro.presentation.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.AnnouncementModel
import pe.edu.esan.sportpro.data.model.AnnouncementType
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.AnnouncementRepository
import pe.edu.esan.sportpro.data.remote.UserTeamsRepository
import pe.edu.esan.sportpro.util.Validators
import java.time.LocalDate

data class AnnouncementsState(
    val loading: Boolean = true,
    val all: List<AnnouncementModel> = emptyList(),
    val teamIds: Set<String> = emptySet(),
    val showArchived: Boolean = false,
    val typeFilter: String = "Todos",
    val message: String? = null
)

/** US-22: lista de anuncios vigentes / archivados, filtrable por tipo. */
class AnnouncementsViewModel : ViewModel() {

    private val _state = MutableStateFlow(AnnouncementsState())
    val state: StateFlow<AnnouncementsState> = _state.asStateFlow()
    private var job: Job? = null
    private var user: UserModel? = null

    fun start(u: UserModel) {
        if (job != null) return
        user = u
        viewModelScope.launch {
            val teams = UserTeamsRepository.getTeamsForUser(u).map { it.id }.toSet()
            _state.update { it.copy(teamIds = teams) }
        }
        job = viewModelScope.launch {
            AnnouncementRepository.observeAll()
                .catch { e -> _state.update { it.copy(loading = false, message = e.message) } }
                .collect { list -> _state.update { it.copy(loading = false, all = list) } }
        }
    }

    fun visible(s: AnnouncementsState): List<AnnouncementModel> {
        val u = user ?: return emptyList()
        return s.all
            .filter { AnnouncementRepository.isForUser(it, u, s.teamIds) }
            .filter { AnnouncementRepository.isActive(it) != s.showArchived }
            .filter { s.typeFilter == "Todos" || it.type == s.typeFilter }
    }

    fun setArchived(v: Boolean) = _state.update { it.copy(showArchived = v) }
    fun setFilter(v: String) = _state.update { it.copy(typeFilter = v) }

    fun markRead(a: AnnouncementModel) {
        val uid = user?.uid ?: return
        if (uid !in a.readBy) viewModelScope.launch { AnnouncementRepository.markRead(a.id, uid) }
    }

    fun delete(a: AnnouncementModel) = viewModelScope.launch {
        AnnouncementRepository.delete(a.id)
            .onSuccess { _state.update { it.copy(message = "Anuncio eliminado") } }
            .onFailure { e -> _state.update { it.copy(message = e.message) } }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

data class AnnouncementFormState(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = AnnouncementType.GENERAL,
    val teams: List<TeamModel> = emptyList(),
    val teamId: String = "",
    val roles: Set<String> = setOf("DT", "JUG", "PAD"),
    val validUntil: String = "",
    val errors: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val message: String? = null
)

/** US-22: crear / editar anuncio (ADM o DT). */
class AnnouncementFormViewModel : ViewModel() {

    private val _state = MutableStateFlow(AnnouncementFormState())
    val state: StateFlow<AnnouncementFormState> = _state.asStateFlow()
    private var original: AnnouncementModel? = null
    private var started = false

    fun start(user: UserModel, editId: String?) {
        if (started) return
        started = true
        viewModelScope.launch {
            val toEdit = editId?.let { AnnouncementRepository.get(it) }
            original = toEdit
            val teams = UserTeamsRepository.getTeamsForUser(user)
            _state.update {
                if (toEdit == null) it.copy(teams = teams, teamId = if (user.roleEnum == Role.DT) teams.firstOrNull()?.id ?: "" else "")
                else it.copy(
                    teams = teams, id = toEdit.id, title = toEdit.title, description = toEdit.description,
                    type = toEdit.type, teamId = toEdit.teamId, roles = toEdit.targetRoles.toSet(), validUntil = toEdit.validUntil
                )
            }
        }
    }

    fun onChange(t: (AnnouncementFormState) -> AnnouncementFormState) = _state.update(t)

    fun save(user: UserModel) {
        val s = _state.value
        val e = mutableMapOf<String, String>()
        if (s.title.isBlank()) e["title"] = "Ingresa el título"
        if (s.title.length > 80) e["title"] = "Máximo 80 caracteres"
        if (s.description.isBlank()) e["description"] = "Ingresa la descripción"
        if (s.description.length > 500) e["description"] = "Máximo 500 caracteres"
        if (s.roles.isEmpty()) e["roles"] = "Elige al menos un destinatario"
        // El DT solo puede enviar a sus equipos; el ADM puede elegir toda la academia
        if (user.roleEnum == Role.DT && s.teamId.isBlank()) e["teamId"] = "Selecciona el equipo"
        val until = Validators.parseDate(s.validUntil)
        if (until == null) e["validUntil"] = "Ingresa la fecha de vigencia"
        else if (until.isBefore(LocalDate.now())) e["validUntil"] = "La vigencia no puede ser una fecha pasada"
        if (e.isNotEmpty()) { _state.update { it.copy(errors = e) }; return }

        viewModelScope.launch {
            _state.update { it.copy(saving = true, errors = emptyMap()) }
            val team = s.teams.firstOrNull { it.id == s.teamId }
            val base = original ?: AnnouncementModel(authorUid = user.uid, authorName = user.fullName, authorRole = user.role)
            val model = base.copy(
                title = s.title.trim(), description = s.description.trim(), type = s.type,
                teamId = s.teamId, teamName = team?.name ?: "Toda la academia",
                targetRoles = s.roles.toList(), validUntil = s.validUntil
            )
            val recipients = if (original == null) AnnouncementRepository.recipientsFor(s.teamId, s.roles.toList())
                .filter { it != user.uid } else emptyList()
            AnnouncementRepository.save(model, recipients)
                .onSuccess { _state.update { it.copy(saving = false, saved = true) } }
                .onFailure { err -> _state.update { it.copy(saving = false, message = err.message) } }
        }
    }
}
