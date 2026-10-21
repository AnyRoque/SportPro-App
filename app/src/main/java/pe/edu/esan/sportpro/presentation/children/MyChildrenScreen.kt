package pe.edu.esan.sportpro.presentation.children

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.edu.esan.sportpro.data.model.LinkStatus
import pe.edu.esan.sportpro.presentation.components.EmptyState
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import pe.edu.esan.sportpro.presentation.components.StatusChip

/** US-02: "Mis hijos" con el estado del vínculo al equipo. */
@Composable
fun MyChildrenScreen(
    parentUid: String,
    onAddChild: () -> Unit,
    viewModel: ChildrenListViewModel = viewModel()
) {
    val s by viewModel.state.collectAsState()
    LaunchedEffect(parentUid) { viewModel.start(parentUid) }

    Box(Modifier.fillMaxSize()) {
        when {
            s.loading -> LoadingBox()
            s.error != null -> EmptyState("Error: ${s.error}")
            s.children.isEmpty() -> EmptyState("Aún no registras a tus hijos.\nPresiona “Agregar hijo”.")
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(s.children, key = { it.id }) { child ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(child.fullName, style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                StatusChip(child.linkStatus)
                            }
                            Text("${child.teamName} · ${child.category}")
                            Text(
                                when (child.linkStatus) {
                                    LinkStatus.PENDING -> "Esperando que el Director Técnico acepte la solicitud"
                                    LinkStatus.ACCEPTED -> "Ya forma parte del plantel"
                                    else -> "La solicitud fue rechazada por el Director Técnico"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onAddChild,
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text("Agregar hijo") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }
}
