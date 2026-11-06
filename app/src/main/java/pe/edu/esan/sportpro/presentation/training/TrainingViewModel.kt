package pe.edu.esan.sportpro.presentation.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.ExerciseModel
import pe.edu.esan.sportpro.data.model.ObjectiveType
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.TrainingModel
import pe.edu.esan.sportpro.data.model.TrainingStatus
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.TrainingRepository
import pe.edu.esan.sportpro.util.Validators
import java.time.LocalDate

data class TrainingListUiState(
    val loading: Boolean = true,
    val trainings: List<TrainingModel> = emptyList(),
    val viewMode: String = "LIST", // "LIST" o "WEEKLY"
    val filterStatus: String = "ALL", // "ALL", "PROGRAMADA", "REALIZADA", "CANCELADA"
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

data class TrainingFormState(
    val id: String = "",
    val team: TeamModel? = null,
    val approvedTeams: List<TeamModel> = emptyList(),
    val date: String = "",
    val startTime: String = "17:00",
    val durationMinutes: Int = 60,
    val location: String = "",
    val objectiveType: String = ObjectiveType.TECHNICAL,
    val objectiveDescription: String = "",
    val exercises: List<ExerciseModel> = emptyList(),
    val isDuplicate: Boolean = false,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val fieldErrors: Map<String, String> = emptyMap(),
    val errorMessage: String? = null
)

class TrainingViewModel : ViewModel() {

    private val _listUi = MutableStateFlow(TrainingListUiState())
    val listUi: StateFlow<TrainingListUiState> = _listUi.asStateFlow()

    private val _formState = MutableStateFlow(TrainingFormState())
    val formState: StateFlow<TrainingFormState> = _formState.asStateFlow()

    fun initList(user: UserModel) {
        viewModelScope.launch {
            _listUi.value = _listUi.value.copy(loading = true)
            TrainingRepository.observeTrainingsForUser(user)
                .catch { e ->
                    _listUi.value = _listUi.value.copy(loading = false, errorMessage = e.message)
                }
                .collect { list ->
                    _listUi.value = _listUi.value.copy(
                        loading = false,
                        trainings = list.sortedByDescending { it.date }
                    )
                }
        }
    }

    fun setViewMode(mode: String) {
        _listUi.value = _listUi.value.copy(viewMode = mode)
    }

    fun setFilterStatus(status: String) {
        _listUi.value = _listUi.value.copy(filterStatus = status)
    }

    fun loadForm(dtUid: String, trainingId: String? = null, duplicateFromId: String? = null) {
        viewModelScope.launch {
            _formState.value = TrainingFormState(loading = true)

            // Cargar equipos aprobados del DT
            val teamsResult = TrainingRepository.getApprovedTeamsByDt(dtUid)
            val approvedTeams = teamsResult.getOrDefault(emptyList())

            if (approvedTeams.isEmpty()) {
                _formState.value = TrainingFormState(
                    approvedTeams = emptyList(),
                    loading = false,
                    errorMessage = "Debes contar con al menos un equipo en estado 'APROBADO' para planificar entrenamientos."
                )
                return@launch
            }

            if (!trainingId.isNull_or_blank()) {
                // Modo Edición
                val result = TrainingRepository.getTraining(trainingId!!)
                result.onSuccess { training ->
                    val matchedTeam = approvedTeams.find { it.id == training.teamId } ?: approvedTeams.first()
                    _formState.value = TrainingFormState(
                        id = training.id,
                        team = matchedTeam,
                        approvedTeams = approvedTeams,
                        date = training.date,
                        startTime = training.startTime,
                        durationMinutes = training.durationMinutes,
                        location = training.location,
                        objectiveType = training.objectiveType,
                        objectiveDescription = training.objectiveDescription,
                        exercises = training.exercises,
                        loading = false
                    )
                }.onFailure { e ->
                    _formState.value = TrainingFormState(
                        approvedTeams = approvedTeams,
                        loading = false,
                        errorMessage = e.message
                    )
                }
            } else if (!duplicateFromId.isNull_or_blank()) {
                // Modo Duplicar Sesión
                val result = TrainingRepository.getTraining(duplicateFromId!!)
                result.onSuccess { training ->
                    val matchedTeam = approvedTeams.find { it.id == training.teamId } ?: approvedTeams.first()
                    _formState.value = TrainingFormState(
                        id = "",
                        team = matchedTeam,
                        approvedTeams = approvedTeams,
                        date = "", // Requiere seleccionar una nueva fecha
                        startTime = training.startTime,
                        durationMinutes = training.durationMinutes,
                        location = training.location,
                        objectiveType = training.objectiveType,
                        objectiveDescription = training.objectiveDescription,
                        exercises = training.exercises,
                        isDuplicate = true,
                        loading = false
                    )
                }.onFailure { e ->
                    _formState.value = TrainingFormState(
                        approvedTeams = approvedTeams,
                        loading = false,
                        errorMessage = e.message
                    )
                }
            } else {
                // Modo Creación
                _formState.value = TrainingFormState(
                    approvedTeams = approvedTeams,
                    team = approvedTeams.firstOrNull(),
                    date = Validators.formatDate(LocalDate.now()),
                    loading = false
                )
            }
        }
    }

    fun onTeamSelected(team: TeamModel) {
        _formState.value = _formState.value.copy(team = team)
    }

    fun onDateChanged(date: String) {
        _formState.value = _formState.value.copy(
            date = date,
            fieldErrors = _formState.value.fieldErrors - "date"
        )
    }

    fun onTimeChanged(time: String) {
        _formState.value = _formState.value.copy(
            startTime = time,
            fieldErrors = _formState.value.fieldErrors - "time"
        )
    }

    fun onDurationChanged(minutes: Int) {
        _formState.value = _formState.value.copy(durationMinutes = minutes)
    }

    fun onLocationChanged(location: String) {
        _formState.value = _formState.value.copy(
            location = location,
            fieldErrors = _formState.value.fieldErrors - "location"
        )
    }

    fun onObjectiveTypeChanged(type: String) {
        _formState.value = _formState.value.copy(objectiveType = type)
    }

    fun onObjectiveDescriptionChanged(desc: String) {
        _formState.value = _formState.value.copy(
            objectiveDescription = desc,
            fieldErrors = _formState.value.fieldErrors - "objective"
        )
    }

    fun addExercise(exercise: ExerciseModel) {
        val currentList = _formState.value.exercises
        _formState.value = _formState.value.copy(
            exercises = currentList + exercise,
            fieldErrors = _formState.value.fieldErrors - "exercises"
        )
    }

    fun removeExercise(exerciseId: String) {
        val currentList = _formState.value.exercises
        _formState.value = _formState.value.copy(
            exercises = currentList.filterNot { it.id == exerciseId }
        )
    }

    fun moveExercise(index: Int, up: Boolean) {
        val list = _formState.value.exercises.toMutableList()
        val targetIndex = if (up) index - 1 else index + 1
        if (targetIndex in list.indices) {
            val temp = list[index]
            list[index] = list[targetIndex]
            list[targetIndex] = temp
            _formState.value = _formState.value.copy(exercises = list)
        }
    }

    fun validateForm(): Boolean {
        val errors = mutableMapOf<String, String>()
        val state = _formState.value

        if (state.team == null) {
            errors["team"] = "Debes seleccionar un equipo"
        }

        if (state.date.isBlank()) {
            errors["date"] = "La fecha es obligatoria"
        } else {
            val parsedDate = Validators.parseDate(state.date)
            if (parsedDate == null) {
                errors["date"] = "Formato de fecha inválido"
            } else if (parsedDate.isBefore(LocalDate.now())) {
                errors["date"] = "La fecha no puede ser anterior a la fecha actual"
            }
        }

        if (state.location.isBlank()) {
            errors["location"] = "El lugar de entrenamiento es obligatorio"
        }

        if (state.objectiveDescription.isBlank()) {
            errors["objective"] = "La descripción del objetivo es obligatoria"
        }

        val totalExercisesDuration = state.exercises.sumOf { it.durationMinutes }
        if (totalExercisesDuration > state.durationMinutes) {
            errors["exercises"] = "La suma de ejercicios ($totalExercisesDuration min) supera la duración total (${state.durationMinutes} min)"
        }

        _formState.value = state.copy(fieldErrors = errors)
        return errors.isEmpty()
    }

    fun saveTraining(dtUser: UserModel, onSuccess: () -> Unit) {
        if (!validateForm()) return

        val state = _formState.value
        val team = state.team ?: return

        _formState.value = state.copy(saving = true)

        viewModelScope.launch {
            val training = TrainingModel(
                id = state.id,
                dtUid = dtUser.uid,
                dtName = dtUser.fullName,
                teamId = team.id,
                teamName = team.name,
                category = team.category,
                date = state.date,
                startTime = state.startTime,
                durationMinutes = state.durationMinutes,
                location = state.location.trim(),
                objectiveType = state.objectiveType,
                objectiveDescription = state.objectiveDescription.trim(),
                exercises = state.exercises,
                status = TrainingStatus.PROGRAMMED,
                duplicatedFromId = if (state.isDuplicate) state.id else ""
            )

            val result = if (state.id.isBlank()) {
                TrainingRepository.createTraining(training)
            } else {
                TrainingRepository.updateTraining(training)
            }

            _formState.value = _formState.value.copy(saving = false)

            result.onSuccess {
                _listUi.value = _listUi.value.copy(
                    infoMessage = if (state.isDuplicate) "Sesión duplicada correctamente" else "Sesión guardada correctamente"
                )
                onSuccess()
            }.onFailure { e ->
                _formState.value = _formState.value.copy(
                    errorMessage = e.message ?: "Error al guardar la sesión"
                )
            }
        }
    }

    fun cancelTraining(trainingId: String, reason: String, dtUid: String) {
        viewModelScope.launch {
            val result = TrainingRepository.cancelTraining(trainingId, reason, dtUid)
            result.onSuccess {
                _listUi.value = _listUi.value.copy(infoMessage = "Sesión cancelada exitosamente")
            }.onFailure { e ->
                _listUi.value = _listUi.value.copy(errorMessage = e.message)
            }
        }
    }

    fun clearMessages() {
        _listUi.value = _listUi.value.copy(errorMessage = null, infoMessage = null)
        _formState.value = _formState.value.copy(errorMessage = null)
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
}
