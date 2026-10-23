package pe.edu.esan.sportpro.presentation.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pe.edu.esan.sportpro.data.model.ExerciseModel
import pe.edu.esan.sportpro.data.model.ObjectiveType
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.TrainingRepository
import pe.edu.esan.sportpro.presentation.components.DateField
import pe.edu.esan.sportpro.presentation.components.DropdownField
import pe.edu.esan.sportpro.presentation.components.FormTextField
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.presentation.components.SectionTitle
import java.util.UUID

/**
 * Formulario para crear, editar o duplicar una Sesión de Entrenamiento (US-11).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingFormScreen(
    user: UserModel,
    viewModel: TrainingViewModel,
    trainingId: String?,
    duplicateFromId: String?,
    onBack: () -> Unit
) {
    val state by viewModel.formState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddExerciseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(trainingId, duplicateFromId) {
        viewModel.loadForm(dtUid = user.uid, trainingId = trainingId, duplicateFromId = duplicateFromId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            state.isDuplicate -> "Duplicar Entrenamiento"
                            state.id.isNotBlank() -> "Editar Entrenamiento"
                            else -> "Planificar Entrenamiento"
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.loading) {
            LoadingBox()
            return@Scaffold
        }

        if (state.approvedTeams.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = state.errorMessage ?: "No cuentas con ningún equipo en estado 'APROBADO'. No es posible planificar entrenamientos.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text("Volver")
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            SectionTitle("1. Datos Generales del Entrenamiento")

            // Selección de Equipo / Categoría
            val teamOptions = state.approvedTeams.map { "${it.name} (${it.category})" }
            val selectedTeamLabel = state.team?.let { "${it.name} (${it.category})" } ?: teamOptions.first()

            DropdownField(
                label = "Equipo / Categoría",
                options = teamOptions,
                selected = selectedTeamLabel,
                onSelected = { label ->
                    val chosen = state.approvedTeams.find { "${it.name} (${it.category})" == label }
                    if (chosen != null) viewModel.onTeamSelected(chosen)
                },
                error = state.fieldErrors["team"]
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Selector de Fecha (Validado que no sea anterior a hoy)
            DateField(
                label = "Fecha de la Sesión",
                value = state.date,
                onValueChange = { viewModel.onDateChanged(it) },
                error = state.fieldErrors["date"]
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormTextField(
                    value = state.startTime,
                    onValueChange = { viewModel.onTimeChanged(it) },
                    label = "Hora de inicio (HH:mm)",
                    modifier = Modifier.weight(1f),
                    error = state.fieldErrors["time"]
                )

                FormTextField(
                    value = state.durationMinutes.toString(),
                    onValueChange = { input ->
                        val mins = input.filter { it.isDigit() }.toIntOrNull() ?: 0
                        viewModel.onDurationChanged(mins)
                    },
                    label = "Duración total (min)",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            FormTextField(
                value = state.location,
                onValueChange = { viewModel.onLocationChanged(it) },
                label = "Lugar de entrenamiento",
                error = state.fieldErrors["location"]
            )

            SectionTitle("2. Objetivos de la Sesión")

            DropdownField(
                label = "Clasificación de Objetivo",
                options = ObjectiveType.ALL,
                selected = state.objectiveType,
                onSelected = { viewModel.onObjectiveTypeChanged(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            FormTextField(
                value = state.objectiveDescription,
                onValueChange = { viewModel.onObjectiveDescriptionChanged(it) },
                label = "Descripción detallada del objetivo",
                singleLine = false,
                error = state.fieldErrors["objective"]
            )

            SectionTitle("3. Lista de Ejercicios Planificados")

            val totalExerciseDuration = state.exercises.sumOf { it.durationMinutes }
            val isExceeded = totalExerciseDuration > state.durationMinutes

            Surface(
                color = if (isExceeded) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Duración planificada: $totalExerciseDuration / ${state.durationMinutes} min",
                        fontWeight = FontWeight.Bold,
                        color = if (isExceeded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            state.fieldErrors["exercises"]?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Lista de ejercicios agregados con reordenamiento
            state.exercises.forEachIndexed { index, exercise ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${index + 1}. ${exercise.name} (${exercise.durationMinutes} min)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = exercise.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Botones de Reordenamiento Subir/Bajar
                        IconButton(onClick = { viewModel.moveExercise(index, true) }, enabled = index > 0) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir")
                        }
                        IconButton(onClick = { viewModel.moveExercise(index, false) }, enabled = index < state.exercises.lastIndex) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar")
                        }
                        IconButton(onClick = { viewModel.removeExercise(exercise.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showAddExerciseDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar Ejercicio a la Lista")
            }

            Spacer(modifier = Modifier.height(28.dp))

            state.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = {
                    viewModel.saveTraining(user, onSuccess = onBack)
                },
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar Sesión de Entrenamiento", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        AddExerciseDialog(
            onAddExercise = { exercise ->
                viewModel.addExercise(exercise)
                showAddExerciseDialog = false
            },
            onDismiss = { showAddExerciseDialog = false }
        )
    }
}

@Composable
fun AddExerciseDialog(
    onAddExercise: (ExerciseModel) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Biblioteca (US-12), 1 = Personalizado

    var customName by remember { mutableStateOf("") }
    var customDescription by remember { mutableStateOf("") }
    var customDuration by remember { mutableStateOf("15") }
    var customType by remember { mutableStateOf(ObjectiveType.TECHNICAL) }

    val libraryExercises = remember { TrainingRepository.getExerciseLibrary() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Ejercicio") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("Biblioteca", modifier = Modifier.padding(8.dp))
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("Personalizado", modifier = Modifier.padding(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // Seleccionar de Biblioteca
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        libraryExercises.forEach { libEx ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                onClick = {
                                    onAddExercise(libEx.copy(id = UUID.randomUUID().toString()))
                                }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(libEx.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text(libEx.description, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "Tipo: ${libEx.type} · ${libEx.durationMinutes} min",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Crear Ejercicio Personalizado
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FormTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = "Nombre del ejercicio"
                        )

                        FormTextField(
                            value = customDescription,
                            onValueChange = { customDescription = it },
                            label = "Descripción",
                            singleLine = false
                        )

                        FormTextField(
                            value = customDuration,
                            onValueChange = { input -> customDuration = input.filter { it.isDigit() } },
                            label = "Duración (minutos)",
                            keyboardType = KeyboardType.Number
                        )

                        DropdownField(
                            label = "Tipo",
                            options = ObjectiveType.ALL,
                            selected = customType,
                            onSelected = { customType = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 1) {
                Button(
                    onClick = {
                        if (customName.isNotBlank()) {
                            val duration = customDuration.toIntOrNull() ?: 15
                            onAddExercise(
                                ExerciseModel(
                                    id = UUID.randomUUID().toString(),
                                    name = customName.trim(),
                                    description = customDescription.trim(),
                                    type = customType,
                                    durationMinutes = duration
                                )
                            )
                        }
                    },
                    enabled = customName.isNotBlank()
                ) {
                    Text("Agregar Ejercicio")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
