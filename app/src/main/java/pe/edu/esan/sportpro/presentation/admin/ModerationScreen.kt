package pe.edu.esan.sportpro.presentation.admin

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.esan.sportpro.data.model.ModerationAction
import pe.edu.esan.sportpro.data.model.ReportModel
import pe.edu.esan.sportpro.data.remote.CommunityRepository
import pe.edu.esan.sportpro.presentation.components.ConfirmDialog
import pe.edu.esan.sportpro.presentation.components.EmptyState
import pe.edu.esan.sportpro.presentation.components.LoadingBox

/** Reportes de una misma publicación agrupados. */
data class ReportGroup(val postId: String, val reports: List<ReportModel>) {
    val first get() = reports.first()
}

data class ModerationState(
    val loading: Boolean = true,
    val groups: List<ReportGroup> = emptyList(),
    val message: String? = null
)

/** US-24: cola de moderación del ADM (más reportadas primero). */
class ModerationViewModel : ViewModel() {
    private val _state = MutableStateFlow(ModerationState())
    val state: StateFlow<ModerationState> = _state.asStateFlow()
    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            CommunityRepository.observePendingReports()
                .catch { e -> _state.update { it.copy(loading = false, message = e.message) } }
                .collect { list ->
                    val groups = list.groupBy { it.postId }
                        .map { (id, r) -> ReportGroup(id, r) }
                        .sortedByDescending { it.reports.size }
                    _state.update { it.copy(loading = false, groups = groups) }
                }
        }
    }

    fun moderate(postId: String, action: String, adminUid: String) = viewModelScope.launch {
        CommunityRepository.moderate(postId, action, adminUid)
            .onSuccess { _state.update { it.copy(message = "Decisión registrada") } }
            .onFailure { e -> _state.update { it.copy(message = e.message) } }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

@Composable
fun ModerationScreen(adminUid: String, viewModel: ModerationViewModel = viewModel()) {
    val s by viewModel.state.collectAsState()
    val context = LocalContext.current
    var pending by remember { mutableStateOf<Pair<ReportGroup, String>?>(null) }

    LaunchedEffect(Unit) { viewModel.start() }
    LaunchedEffect(s.message) {
        s.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); viewModel.clearMessage() }
    }

    when {
        s.loading -> LoadingBox()
        s.groups.isEmpty() -> EmptyState("No hay reportes pendientes")
        else -> LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(s.groups, key = { it.postId }) { g ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${g.reports.size} reporte(s) · Autor: ${g.first.postAuthorName}",
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                        Text("“${g.first.postText}”", style = MaterialTheme.typography.bodyMedium)
                        Text("Motivos", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                        g.reports.forEach { r ->
                            Text("• ${r.reason}${if (r.detail.isNotBlank()) ": ${r.detail}" else ""}",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { pending = g to ModerationAction.KEEP }, modifier = Modifier.weight(1f)) {
                                Text("Mantener")
                            }
                            OutlinedButton(onClick = { pending = g to ModerationAction.HIDE }, modifier = Modifier.weight(1f)) {
                                Text("Ocultar")
                            }
                            Button(
                                onClick = { pending = g to ModerationAction.DELETE },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) { Text("Eliminar") }
                        }
                    }
                }
            }
        }
    }

    pending?.let { (g, action) ->
        val label = when (action) {
            ModerationAction.KEEP -> "mantener visible"
            ModerationAction.HIDE -> "ocultar"
            else -> "eliminar"
        }
        ConfirmDialog(
            title = "Confirmar decisión",
            message = "¿Deseas $label esta publicación? Se notificará al autor y a quienes la reportaron.",
            onConfirm = { viewModel.moderate(g.postId, action, adminUid); pending = null },
            onDismiss = { pending = null }
        )
    }
}
