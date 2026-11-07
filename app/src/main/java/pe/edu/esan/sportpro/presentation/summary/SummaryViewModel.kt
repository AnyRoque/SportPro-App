package pe.edu.esan.sportpro.presentation.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.MatchEventModel
import pe.edu.esan.sportpro.data.model.MatchModel
import pe.edu.esan.sportpro.data.model.MatchSummaryModel
import pe.edu.esan.sportpro.data.model.SummaryStatus
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.MatchRepository
import pe.edu.esan.sportpro.data.remote.SummaryRepository

data class MatchSummaryState(
    val loading: Boolean = true,
    val match: MatchModel? = null,
    val events: List<MatchEventModel> = emptyList(),
    val summary: MatchSummaryModel? = null,
    val facts: String = "",
    val analysis: String = "",
    val editing: Boolean = false,
    val generating: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

/** US-25 / US-26 / US-27: generar, revisar, aprobar y ver el resumen de un partido. */
class MatchSummaryViewModel : ViewModel() {

    private val _state = MutableStateFlow(MatchSummaryState())
    val state: StateFlow<MatchSummaryState> = _state.asStateFlow()
    private var started = false

    fun start(matchId: String) {
        if (started) return
        started = true
        viewModelScope.launch {
            MatchRepository.getMatch(matchId)
                .onSuccess { m -> _state.update { it.copy(match = m) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
        viewModelScope.launch {
            MatchRepository.observeMatchEvents(matchId).catch { }
                .collect { p -> _state.update { it.copy(events = p.first) } }
        }
        viewModelScope.launch {
            SummaryRepository.observeSummary(matchId)
                .catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                .collect { sm ->
                    _state.update {
                        // No pisar lo que el DT está editando
                        if (it.editing) it.copy(loading = false, summary = sm)
                        else it.copy(loading = false, summary = sm,
                            facts = sm?.finalFacts.orEmpty(), analysis = sm?.finalAnalysis.orEmpty())
                    }
                }
        }
    }

    fun canGenerate(s: MatchSummaryState): Boolean {
        val m = s.match ?: return false
        return SummaryRepository.hasEnoughEvents(m, s.events)
    }

    fun generate(dt: UserModel) {
        val s = _state.value
        val m = s.match ?: return
        viewModelScope.launch {
            _state.update { it.copy(generating = true, error = null, editing = false) }
            SummaryRepository.generate(m, s.events, dt)
                .onSuccess { _state.update { it.copy(generating = false, message = "Resumen generado. Revísalo antes de publicar") } }
                .onFailure { e -> _state.update { it.copy(generating = false, error = e.message) } }
        }
    }

    fun startEdit() = _state.update { it.copy(editing = true) }
    fun onFacts(v: String) = _state.update { it.copy(facts = v.take(2000)) }
    fun onAnalysis(v: String) = _state.update { it.copy(analysis = v.take(600)) }

    fun saveEdit() {
        val s = _state.value
        val m = s.match ?: return
        if (s.facts.isBlank()) { _state.update { it.copy(error = "Los hechos no pueden quedar vacíos") }; return }
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            SummaryRepository.saveEdit(m.id, s.facts.trim(), s.analysis.trim())
                .onSuccess { _state.update { it.copy(saving = false, editing = false, message = "Cambios guardados") } }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.message) } }
        }
    }

    fun approve(dt: UserModel) {
        val s = _state.value
        val sm = s.summary ?: return
        if (s.facts.isBlank()) { _state.update { it.copy(error = "Los hechos no pueden quedar vacíos") }; return }
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            SummaryRepository.approve(sm, s.facts.trim(), s.analysis.trim(), dt)
                .onSuccess { _state.update { it.copy(saving = false, editing = false, message = "Resumen publicado y equipo notificado") } }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.message) } }
        }
    }

    fun reject() = setStatus(SummaryStatus.REJECTED, "Resumen rechazado. Puedes regenerarlo")
    fun unpublish() = setStatus(SummaryStatus.PENDING_REVIEW, "Resumen despublicado")

    private fun setStatus(status: String, msg: String) {
        val m = _state.value.match ?: return
        viewModelScope.launch {
            SummaryRepository.setStatus(m.id, status)
                .onSuccess { _state.update { it.copy(message = msg) } }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

data class SummariesListState(
    val loading: Boolean = true,
    val list: List<MatchSummaryModel> = emptyList(),
    val error: String? = null
)

/** US-27: resúmenes publicados de los partidos del usuario. */
class SummariesListViewModel : ViewModel() {
    private val _state = MutableStateFlow(SummariesListState())
    val state: StateFlow<SummariesListState> = _state.asStateFlow()
    private var started = false

    fun start(uid: String) {
        if (started) return
        started = true
        viewModelScope.launch {
            SummaryRepository.observePublishedForUser(uid)
                .catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
                .collect { l -> _state.update { it.copy(loading = false, list = l) } }
        }
    }
}
