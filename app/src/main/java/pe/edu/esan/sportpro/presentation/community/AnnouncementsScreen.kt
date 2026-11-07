package pe.edu.esan.sportpro.presentation.community

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.edu.esan.sportpro.data.model.AnnouncementModel
import pe.edu.esan.sportpro.data.model.AnnouncementType
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.presentation.components.ConfirmDialog
import pe.edu.esan.sportpro.presentation.components.DropdownField
import pe.edu.esan.sportpro.presentation.components.EmptyState
import pe.edu.esan.sportpro.presentation.components.LoadingBox

/** US-22: anuncios vigentes (más reciente primero), filtro por tipo, no leídos y archivados. */
@Composable
fun AnnouncementsScreen(
    user: UserModel,
    onNew: () -> Unit,
    onEdit: (AnnouncementModel) -> Unit,
    viewModel: AnnouncementsViewModel = viewModel()
) {
    val s by viewModel.state.collectAsState()
    val context = LocalContext.current
    var expandedId by remember { mutableStateOf<String?>(null) }
    var toDelete by remember { mutableStateOf<AnnouncementModel?>(null) }
    val canPublish = user.roleEnum == Role.ADM || user.roleEnum == Role.DT

    LaunchedEffect(user.uid) { viewModel.start(user) }
    LaunchedEffect(s.message) {
        s.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); viewModel.clearMessage() }
    }

    val list = viewModel.visible(s)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = if (s.showArchived) 1 else 0) {
                Tab(selected = !s.showArchived, onClick = { viewModel.setArchived(false) }, text = { Text("Vigentes") })
                Tab(selected = s.showArchived, onClick = { viewModel.setArchived(true) }, text = { Text("Archivados") })
            }
            DropdownField(
                label = "Tipo", options = listOf("Todos") + AnnouncementType.ALL, selected = s.typeFilter,
                onSelected = viewModel::setFilter, modifier = Modifier.padding(16.dp, 8.dp)
            )
            when {
                s.loading -> LoadingBox()
                list.isEmpty() -> EmptyState(if (s.showArchived) "No hay anuncios archivados" else "No hay anuncios vigentes")
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(list, key = { it.id }) { a ->
                        val unread = user.uid !in a.readBy && a.authorUid != user.uid
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                expandedId = if (expandedId == a.id) null else a.id
                                viewModel.markRead(a)
                            },
                            colors = if (unread) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                     else CardDefaults.cardColors()
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (unread) Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(10.dp)) {}
                                    Text(a.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f).padding(start = if (unread) 8.dp else 0.dp))
                                    TypeChip(a.type)
                                }
                                Text("${a.teamName} · ${a.authorName}${if (a.edited) " · Editado" else ""}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    a.description,
                                    maxLines = if (expandedId == a.id) Int.MAX_VALUE else 2,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text("Vigente hasta ${a.validUntil}", style = MaterialTheme.typography.labelSmall)
                                if (a.authorUid == user.uid) {
                                    Row {
                                        TextButton(onClick = { onEdit(a) }) { Text("Editar") }
                                        TextButton(onClick = { toDelete = a }) { Text("Eliminar") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (canPublish) {
            ExtendedFloatingActionButton(
                onClick = onNew,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Nuevo anuncio") },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            )
        }
    }

    toDelete?.let { a ->
        ConfirmDialog(
            title = "Eliminar anuncio",
            message = "“${a.title}” dejará de verse para todos.",
            onConfirm = { viewModel.delete(a); toDelete = null },
            onDismiss = { toDelete = null }
        )
    }
}

@Composable
fun TypeChip(text: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
        Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}
