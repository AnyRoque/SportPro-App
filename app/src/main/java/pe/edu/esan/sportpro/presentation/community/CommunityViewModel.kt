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
import pe.edu.esan.sportpro.data.model.CommentModel
import pe.edu.esan.sportpro.data.model.PostModel
import pe.edu.esan.sportpro.data.model.PostStatus
import pe.edu.esan.sportpro.data.model.PostVisibility
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.CommunityRepository
import pe.edu.esan.sportpro.data.remote.UserTeamsRepository
import pe.edu.esan.sportpro.util.Validators

data class CommunityState(
    val loading: Boolean = true,
    val posts: List<PostModel> = emptyList(),
    val teams: List<TeamModel> = emptyList(),
    val tab: String = PostVisibility.TEAM,
    val message: String? = null
)

/** US-23: feed de la comunidad (Mi equipo / Comunidad) y US-24: reportes. */
class CommunityViewModel : ViewModel() {

    private val _state = MutableStateFlow(CommunityState())
    val state: StateFlow<CommunityState> = _state.asStateFlow()
    private var job: Job? = null
    private var user: UserModel? = null

    fun start(u: UserModel) {
        if (job != null) return
        user = u
        viewModelScope.launch {
            val teams = UserTeamsRepository.getTeamsForUser(u)
            _state.update { it.copy(teams = teams) }
        }
        job = viewModelScope.launch {
            CommunityRepository.observeFeed()
                .catch { e -> _state.update { it.copy(loading = false, message = e.message) } }
                .collect { list -> _state.update { it.copy(loading = false, posts = list) } }
        }
    }

    /**
     * Visible: no eliminada; las ocultas solo las ve su autor;
     * pestaña "Mi equipo" muestra solo publicaciones de los equipos del usuario.
     */
    fun visible(s: CommunityState): List<PostModel> {
        val u = user ?: return emptyList()
        val teamIds = s.teams.map { it.id }.toSet()
        return s.posts.filter { p ->
            p.status != PostStatus.DELETED &&
                (p.status == PostStatus.VISIBLE || p.authorUid == u.uid) &&
                when (s.tab) {
                    PostVisibility.TEAM -> p.teamId in teamIds
                    else -> p.visibility == PostVisibility.COMMUNITY
                }
        }
    }

    fun setTab(t: String) = _state.update { it.copy(tab = t) }

    fun react(post: PostModel, reaction: String) {
        val uid = user?.uid ?: return
        viewModelScope.launch { CommunityRepository.toggleReaction(post, uid, reaction) }
    }

    fun delete(post: PostModel) = viewModelScope.launch {
        CommunityRepository.deletePost(post.id)
            .onSuccess { _state.update { it.copy(message = "Publicación eliminada") } }
            .onFailure { e -> _state.update { it.copy(message = e.message) } }
    }

    fun report(post: PostModel, reason: String, detail: String) = viewModelScope.launch {
        val uid = user?.uid ?: return@launch
        CommunityRepository.reportPost(post, uid, reason, detail)
            .onSuccess { _state.update { it.copy(message = "Reporte enviado. Un administrador lo revisará") } }
            .onFailure { e -> _state.update { it.copy(message = e.message) } }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

data class CreatePostState(
    val text: String = "",
    val visibility: String = PostVisibility.TEAM,
    val teams: List<TeamModel> = emptyList(),
    val teamId: String = "",
    val isMinor: Boolean = false,
    val error: String? = null,
    val saving: Boolean = false,
    val saved: Boolean = false
)

/** US-23: crear publicación (texto, visibilidad). Un jugador menor solo publica en "Mi equipo". */
class CreatePostViewModel : ViewModel() {

    private val _state = MutableStateFlow(CreatePostState())
    val state: StateFlow<CreatePostState> = _state.asStateFlow()
    private var started = false

    fun start(u: UserModel) {
        if (started) return
        started = true
        viewModelScope.launch {
            val teams = UserTeamsRepository.getTeamsForUser(u)
            val minor = u.roleEnum == Role.JUG && u.birthDate.isNotBlank() && Validators.isMinor(u.birthDate)
            _state.update { it.copy(teams = teams, teamId = teams.firstOrNull()?.id ?: "", isMinor = minor) }
        }
    }

    fun onChange(t: (CreatePostState) -> CreatePostState) = _state.update(t)

    fun publish(u: UserModel) {
        val s = _state.value
        val error = when {
            s.text.isBlank() -> "Escribe algo para publicar"
            s.text.length > 1000 -> "Máximo 1000 caracteres"
            s.teamId.isBlank() && s.visibility == PostVisibility.TEAM -> "No perteneces a ningún equipo aprobado"
            s.isMinor && s.visibility == PostVisibility.COMMUNITY -> "Los jugadores menores solo pueden publicar en “Mi equipo”"
            CommunityRepository.containsOffensiveWords(s.text) -> "La publicación contiene palabras no permitidas"
            else -> null
        }
        if (error != null) { _state.update { it.copy(error = error) }; return }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            val team = s.teams.firstOrNull { it.id == s.teamId }
            val post = PostModel(
                text = s.text.trim(), visibility = s.visibility,
                teamId = team?.id ?: "", teamName = team?.name ?: "",
                authorUid = u.uid, authorName = u.fullName, authorRole = u.role
            )
            CommunityRepository.createPost(post)
                .onSuccess { _state.update { it.copy(saving = false, saved = true) } }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.message) } }
        }
    }
}

data class PostDetailState(
    val post: PostModel? = null,
    val comments: List<CommentModel> = emptyList(),
    val input: String = "",
    val error: String? = null
)

/** US-23: detalle con comentarios en tiempo real. */
class PostDetailViewModel : ViewModel() {

    private val _state = MutableStateFlow(PostDetailState())
    val state: StateFlow<PostDetailState> = _state.asStateFlow()
    private var started = false

    fun start(postId: String) {
        if (started) return
        started = true
        viewModelScope.launch {
            CommunityRepository.observePost(postId).catch { }.collect { p -> _state.update { it.copy(post = p) } }
        }
        viewModelScope.launch {
            CommunityRepository.observeComments(postId).catch { }.collect { c -> _state.update { it.copy(comments = c) } }
        }
    }

    fun onInput(v: String) = _state.update { it.copy(input = v.take(300), error = null) }

    fun send(u: UserModel) {
        val s = _state.value
        val post = s.post ?: return
        if (s.input.isBlank()) return
        viewModelScope.launch {
            CommunityRepository.addComment(
                CommentModel(postId = post.id, text = s.input.trim(), authorUid = u.uid, authorName = u.fullName, authorRole = u.role)
            ).onSuccess { _state.update { it.copy(input = "") } }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    fun deleteComment(c: CommentModel) = viewModelScope.launch {
        CommunityRepository.deleteComment(c.postId, c.id)
    }

    fun react(u: UserModel, reaction: String) {
        val post = _state.value.post ?: return
        viewModelScope.launch { CommunityRepository.toggleReaction(post, u.uid, reaction) }
    }
}
