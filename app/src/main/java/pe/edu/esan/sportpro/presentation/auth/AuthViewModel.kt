package pe.edu.esan.sportpro.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.FirebaseAuthManager
import pe.edu.esan.sportpro.util.FormValidators
import pe.edu.esan.sportpro.util.RegisterForm
import pe.edu.esan.sportpro.util.Validators

/** Sesión actual: se comparte en toda la app para conocer el rol del usuario. */
data class SessionState(
    val checking: Boolean = true,
    val user: UserModel? = null,
    /** true justo después del registro: el PAD es llevado a "Registro de hijos" (US-02) */
    val justRegistered: Boolean = false
)

data class AuthUiState(
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val fieldErrors: Map<String, String> = emptyMap()
)

class AuthViewModel : ViewModel() {

    private val _session = MutableStateFlow(SessionState())
    val session: StateFlow<SessionState> = _session.asStateFlow()

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    init {
        // Si ya hay sesión de Firebase, se recupera el perfil y el rol
        if (FirebaseAuthManager.currentUid == null) {
            _session.value = SessionState(checking = false, user = null)
        } else {
            viewModelScope.launch {
                val result = FirebaseAuthManager.getUserProfile()
                _session.value = SessionState(checking = false, user = result.getOrNull())
            }
        }
    }

    fun login(email: String, password: String) {
        if (!Validators.isValidEmail(email) || password.isBlank()) {
            _ui.value = AuthUiState(errorMessage = "Ingresa un correo válido y tu contraseña")
            return
        }
        viewModelScope.launch {
            _ui.value = AuthUiState(loading = true)
            val result = FirebaseAuthManager.loginUser(email, password)
            result.onSuccess { user ->
                _session.value = SessionState(checking = false, user = user)
                _ui.value = AuthUiState()
            }.onFailure { e ->
                _ui.value = AuthUiState(errorMessage = e.message ?: "Error desconocido")
            }
        }
    }

    fun register(form: RegisterForm) {
        val errors = FormValidators.validateRegister(form)
        if (errors.isNotEmpty()) {
            _ui.value = AuthUiState(fieldErrors = errors)
            return
        }
        viewModelScope.launch {
            _ui.value = AuthUiState(loading = true)
            val user = UserModel(
                name = form.name.trim(),
                lastName = form.lastName.trim(),
                email = form.email.trim(),
                phone = form.phone.trim(),
                role = form.role!!.name,
                birthDate = if (form.role == Role.JUG) form.birthDate else "",
                acceptedTerms = form.acceptedTerms
            )
            FirebaseAuthManager.registerUser(user, form.password)
                .onSuccess { saved ->
                    _session.value = SessionState(checking = false, user = saved, justRegistered = true)
                    _ui.value = AuthUiState()
                }
                .onFailure { e ->
                    val fieldErrors = if (e.message == "El correo ya está registrado")
                        mapOf("email" to e.message!!) else emptyMap()
                    _ui.value = AuthUiState(errorMessage = e.message, fieldErrors = fieldErrors)
                }
        }
    }

    fun sendPasswordReset(email: String) {
        if (!Validators.isValidEmail(email)) {
            _ui.value = AuthUiState(errorMessage = "Ingresa un correo válido")
            return
        }
        viewModelScope.launch {
            _ui.value = AuthUiState(loading = true)
            FirebaseAuthManager.sendPasswordReset(email)
                .onSuccess { _ui.value = AuthUiState(infoMessage = "Te enviamos un enlace a $email para restablecer tu contraseña") }
                .onFailure { e -> _ui.value = AuthUiState(errorMessage = e.message) }
        }
    }

    fun logout() {
        FirebaseAuthManager.logout()
        _session.value = SessionState(checking = false, user = null)
        _ui.value = AuthUiState()
    }

    fun consumeJustRegistered() = _session.update { it.copy(justRegistered = false) }

    fun clearMessages() = _ui.update { it.copy(errorMessage = null, infoMessage = null) }

    fun clearFieldError(field: String) = _ui.update { it.copy(fieldErrors = it.fieldErrors - field) }
}
