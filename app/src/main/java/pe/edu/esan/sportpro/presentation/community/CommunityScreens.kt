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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import pe.edu.esan.sportpro.data.model.PostModel
import pe.edu.esan.sportpro.data.model.PostStatus
import pe.edu.esan.sportpro.data.model.PostVisibility
import pe.edu.esan.sportpro.data.model.ReactionType
import pe.edu.esan.sportpro.data.model.ReportReason
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.presentation.components.ConfirmDialog
import pe.edu.esan.sportpro.presentation.components.DropdownField
import pe.edu.esan.sportpro.presentation.components.EmptyState
import pe.edu.esan.sportpro.presentation.components.LoadingBox
import java.text.SimpleDateFormat
import java.util.Locale

private fun roleLabel(role: String) = Role.fromOrNull(role)?.label ?: role
private val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

// =====================================================================
// US-23: Feed
// =====================================================================

@Composable
fun CommunityFeedScreen(
    user: UserModel,
    onNewPost: () -> Unit,
    onOpenPost: (PostModel) -> Unit,
    viewModel: CommunityViewModel = viewModel()
) {
    val s by viewModel.state.collectAsState()
    val context = LocalContext.current
    var reporting by remember { mutableStateOf<PostModel?>(null) }
    var deleting by remember { mutableStateOf<PostModel?>(null) }

    LaunchedEffect(user.uid) { viewModel.start(user) }
    LaunchedEffect(s.message) {
        s.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); viewModel.clearMessage() }
    }
    val list = viewModel.visible(s)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = if (s.tab == PostVisibility.TEAM) 0 else 1) {
                Tab(selected = s.tab == PostVisibility.TEAM, onClick = { viewModel.setTab(PostVisibility.TEAM) }, text = { Text("Mi equipo") })
                Tab(selected = s.tab == PostVisibility.COMMUNITY, onClick = { viewModel.setTab(PostVisibility.COMMUNITY) }, text = { Text("Comunidad") })
            }
            when {
                s.loading -> LoadingBox()
                list.isEmpty() -> EmptyState("Aún no hay publicaciones.\nSé el primero en publicar.")
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(list, key = { it.id }) { post ->
                        PostCard(
                            post = post, user = user,
                            onOpen = { onOpenPost(post) },
                            onReact = { r -> viewModel.react(post, r) },
                            onReport = { reporting = post },
                            onDelete = { deleting = post }
                        )
                    }
                }
            }
        }
        if (user.roleEnum != Role.ADM) {
            ExtendedFloatingActionButton(
                onClick = onNewPost, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Publicar") },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            )
        }
    }

    reporting?.let { p ->
        ReportDialog(onConfirm = { reason, detail -> viewModel.report(p, reason, detail); reporting = null }, onDismiss = { reporting = null })
    }
    deleting?.let { p ->
        ConfirmDialog("Eliminar publicación", "Tu publicación dejará de verse en la comunidad.",
            onConfirm = { viewModel.delete(p); deleting = null }, onDismiss = { deleting = null })
    }
}

@Composable
fun PostCard(
    post: PostModel,
    user: UserModel,
    onOpen: (() -> Unit)?,
    onReact: (String) -> Unit,
    onReport: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().let { if (onOpen != null) it.clickable(onClick = onOpen) else it }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(post.authorName, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${roleLabel(post.authorRole)}${if (post.teamName.isNotBlank()) " · ${post.teamName}" else ""} · " +
                            (post.createdAt?.toDate()?.let { dateFmt.format(it) } ?: "Enviando…"),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline
                    )
                }
                TypeChip(PostVisibility.label(post.visibility))
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Opciones") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (post.authorUid == user.uid) {
                            DropdownMenuItem(text = { Text("Eliminar") }, onClick = { menu = false; onDelete() })
                        } else {
                            DropdownMenuItem(text = { Text("Reportar") }, onClick = { menu = false; onReport() })
                        }
                    }
                }
            }
            if (post.status == PostStatus.HIDDEN) {
                Text("Oculta temporalmente por reportes, en revisión del administrador.",
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Text(post.text, style = MaterialTheme.typography.bodyLarge)
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ReactionType.ALL.forEach { r ->
                    val count = post.reactions.values.count { it == r }
                    FilterChip(
                        selected = post.reactions[user.uid] == r,
                        onClick = { onReact(r) },
                        label = { Text(if (count > 0) "$r $count" else r) }
                    )
                }
                Text("💬 ${post.commentCount}", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** US-24: motivos de reporte; "Otro" exige descripción. */
@Composable
fun ReportDialog(onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var reason by remember { mutableStateOf(ReportReason.ALL.first()) }
    var detail by remember { mutableStateOf("") }
    val valid = reason != ReportReason.OTHER || detail.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reportar publicación") },
        text = {
            Column {
                ReportReason.ALL.forEach { r ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { reason = r }) {
                        RadioButton(selected = reason == r, onClick = { reason = r })
                        Text(r)
                    }
                }
                if (reason == ReportReason.OTHER) {
                    OutlinedTextField(detail, { detail = it.take(200) }, label = { Text("Describe el motivo *") }, modifier = Modifier.fillMaxWidth())
                }
                Text("Tu reporte es anónimo para el autor.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(reason, detail) }, enabled = valid) { Text("Enviar reporte") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// =====================================================================
// US-23: Crear publicación
// =====================================================================

@Composable
fun CreatePostScreen(user: UserModel, onDone: () -> Unit, viewModel: CreatePostViewModel = viewModel()) {
    val s by viewModel.state.collectAsState()
    LaunchedEffect(user.uid) { viewModel.start(user) }
    LaunchedEffect(s.saved) { if (s.saved) onDone() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = s.text, onValueChange = { v -> viewModel.onChange { it.copy(text = v.take(1000), error = null) } },
            label = { Text("¿Qué quieres compartir? (${s.text.length}/1000)") },
            minLines = 4, modifier = Modifier.fillMaxWidth()
        )
        Text("Visibilidad", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = s.visibility == PostVisibility.TEAM,
                onClick = { viewModel.onChange { it.copy(visibility = PostVisibility.TEAM) } }, label = { Text("Mi equipo") })
            FilterChip(selected = s.visibility == PostVisibility.COMMUNITY, enabled = !s.isMinor,
                onClick = { viewModel.onChange { it.copy(visibility = PostVisibility.COMMUNITY) } }, label = { Text("Comunidad") })
        }
        if (s.isMinor) Text("Como jugador menor de edad solo puedes publicar en “Mi equipo”.", style = MaterialTheme.typography.bodySmall)
        if (s.teams.size > 1) {
            DropdownField("Equipo", s.teams.map { it.name }, s.teams.firstOrNull { it.id == s.teamId }?.name ?: "",
                { name -> viewModel.onChange { st -> st.copy(teamId = st.teams.firstOrNull { it.name == name }?.id ?: "") } })
        }
        Text("No compartas teléfonos, direcciones ni datos personales de menores.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { viewModel.publish(user) }, enabled = !s.saving, modifier = Modifier.fillMaxWidth()) {
            if (s.saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Publicar")
        }
    }
}

// =====================================================================
// US-23: Detalle con comentarios
// =====================================================================

@Composable
fun PostDetailScreen(user: UserModel, postId: String, viewModel: PostDetailViewModel = viewModel()) {
    val s by viewModel.state.collectAsState()
    val context = LocalContext.current
    var reporting by remember { mutableStateOf(false) }
    val communityVm: CommunityViewModel = viewModel()
    LaunchedEffect(postId) { viewModel.start(postId); communityVm.start(user) }
    val cs by communityVm.state.collectAsState()
    LaunchedEffect(cs.message) { cs.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); communityVm.clearMessage() } }

    val post = s.post
    if (post == null || post.status == PostStatus.DELETED) { EmptyState("Publicación no disponible"); return }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                PostCard(post, user, onOpen = null, onReact = { viewModel.react(user, it) },
                    onReport = { reporting = true }, onDelete = { communityVm.delete(post) })
            }
            item { Text("Comentarios", style = MaterialTheme.typography.titleSmall) }
            if (s.comments.isEmpty()) item { Text("Sin comentarios todavía", color = MaterialTheme.colorScheme.outline) }
            items(s.comments, key = { it.id }) { c ->
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("${c.authorName} · ${roleLabel(c.authorRole)}", fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodySmall)
                        Text(c.text)
                    }
                    if (c.authorUid == user.uid) {
                        IconButton(onClick = { viewModel.deleteComment(c) }) { Icon(Icons.Default.Delete, "Eliminar comentario") }
                    }
                }
            }
        }
        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = s.input, onValueChange = viewModel::onInput,
                placeholder = { Text("Escribe un comentario (${s.input.length}/300)") },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.send(user) }, enabled = s.input.isNotBlank()) {
                Icon(Icons.AutoMirrored.Filled.Send, "Enviar")
            }
        }
    }

    if (reporting) {
        ReportDialog(onConfirm = { r, d -> communityVm.report(post, r, d); reporting = false }, onDismiss = { reporting = false })
    }
}
