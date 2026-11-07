package pe.edu.esan.sportpro.presentation.summary

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import pe.edu.esan.sportpro.data.model.MatchStatus
import pe.edu.esan.sportpro.data.model.MatchSummaryModel
import pe.edu.esan.sportpro.data.model.SummaryStatus
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.SummaryRepository
import pe.edu.esan.sportpro.presentation.components.ConfirmDialog
import pe.edu.esan.sportpro.presentation.components.EmptyState
import pe.edu.esan.sportpro.presentation.components.FormTextField
import pe.edu.esan.sportpro.presentation.components.LoadingBox

/**
 * US-25 / US-26 / US-27: resumen narrativo del partido.
 * - DT del partido: generar / regenerar / editar / aprobar / rechazar / despublicar.
 * - JUG / PAD / otros: solo ven el resumen PUBLICADO.
 */
@Composable
fun MatchSummaryScreen(user: UserModel, matchId: String, viewModel: MatchSummaryViewModel = viewModel()) {
    val s by viewModel.state.collectAsState()
    val context = LocalContext.current
    var confirm by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(matchId) { viewModel.start(matchId) }
    LaunchedEffect(s.message) {
        s.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); viewModel.clearMessage() }
    }

    val m = s.match
    if (s.loading || m == null) {
        if (s.error != null && m == null) EmptyState(s.error ?: "") else LoadingBox()
        return
    }
    val isDt = user.uid == m.dtUid
    val sm = s.summary

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("${m.teamName} ${m.ownScore} - ${m.rivalScore} ${m.rival}",
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("${m.date} · ${m.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

        if (!isDt) {
            // Vista JUG / PAD (US-27)
            if (sm == null || sm.status != SummaryStatus.PUBLISHED) {
                EmptyState("El resumen de este partido aún no está disponible")
            } else PublishedSummary(sm)
            return@Column
        }

        // ---------------- Vista DT ----------------
        s.error?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(12.dp)) {
                    Text(it, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        when {
            m.status != MatchStatus.FINISHED ->
                EmptyState("El resumen se podrá generar cuando el partido esté finalizado")

            s.generating -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Generando resumen con IA…")
            }

            sm == null || sm.status == SummaryStatus.REJECTED -> {
                if (sm?.status == SummaryStatus.REJECTED) Text("El resumen anterior fue rechazado.", color = MaterialTheme.colorScheme.outline)
                if (!viewModel.canGenerate(s)) {
                    Text("No hay eventos suficientes para generar un resumen completo (se requiere inicio del partido y al menos un evento más).",
                        color = MaterialTheme.colorScheme.error)
                }
                val used = sm?.generation ?: 0
                Button(
                    onClick = { viewModel.generate(user) },
                    enabled = viewModel.canGenerate(s) && used < SummaryRepository.MAX_GENERATIONS,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (sm == null) "Generar resumen con IA" else "Regenerar resumen") }
                Text("Generaciones usadas: $used/${SummaryRepository.MAX_GENERATIONS}", style = MaterialTheme.typography.labelSmall)
            }

            sm.status == SummaryStatus.PUBLISHED -> {
                PublishedSummary(sm)
                OutlinedButton(onClick = { confirm = "UNPUBLISH" }, modifier = Modifier.fillMaxWidth()) { Text("Despublicar") }
            }

            else -> { // PENDIENTE_REVISION (US-26)
                StatusBanner("Pendiente de revisión · Generación ${sm.generation}/${SummaryRepository.MAX_GENERATIONS}")
                sm.warnings.forEach { w ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(w, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (s.editing) {
                    FormTextField(s.facts, viewModel::onFacts, "Hechos (${s.facts.length}/2000)", singleLine = false)
                    FormTextField(s.analysis, viewModel::onAnalysis, "Análisis (${s.analysis.length}/600)", singleLine = false)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = viewModel::saveEdit, enabled = !s.saving, modifier = Modifier.weight(1f)) { Text("Guardar borrador") }
                        Button(onClick = { confirm = "APPROVE" }, enabled = !s.saving, modifier = Modifier.weight(1f)) { Text("Aprobar y publicar") }
                    }
                } else {
                    SummaryBody(s.facts, s.analysis)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = viewModel::startEdit, modifier = Modifier.weight(1f)) { Text("Editar") }
                        Button(onClick = { confirm = "APPROVE" }, enabled = !s.saving, modifier = Modifier.weight(1f)) { Text("Aprobar y publicar") }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { confirm = "REJECT" }) { Text("Rechazar") }
                    TextButton(
                        onClick = { viewModel.generate(user) },
                        enabled = sm.generation < SummaryRepository.MAX_GENERATIONS && viewModel.canGenerate(s)
                    ) { Text("Regenerar") }
                }
                if (sm.aiFacts != s.facts) {
                    HorizontalDivider()
                    Text("Texto original de la IA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    Text(sm.aiFacts, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }

    confirm?.let { c ->
        val (title, msg) = when (c) {
            "APPROVE" -> "Publicar resumen" to "Se publicará para jugadores y padres del equipo y se les notificará."
            "REJECT" -> "Rechazar resumen" to "El resumen se descartará. Podrás regenerarlo si quedan intentos."
            else -> "Despublicar resumen" to "Dejará de ser visible para el equipo."
        }
        ConfirmDialog(title, msg,
            onConfirm = {
                when (c) {
                    "APPROVE" -> viewModel.approve(user)
                    "REJECT" -> viewModel.reject()
                    else -> viewModel.unpublish()
                }
                confirm = null
            },
            onDismiss = { confirm = null })
    }
}

@Composable
private fun StatusBanner(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Text(text, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SummaryBody(facts: String, analysis: String) {
    Text("Hechos del partido", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Text(facts, style = MaterialTheme.typography.bodyMedium)
    if (analysis.isNotBlank()) {
        Text("Análisis", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(analysis, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PublishedSummary(sm: MatchSummaryModel) {
    SummaryBody(sm.finalFacts, sm.finalAnalysis)
    HorizontalDivider()
    Text("Generado con IA y revisado por el DT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    Text("Aprobado por ${sm.approvedByName} el ${sm.approvedAt}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
}

/** US-27: lista de resúmenes publicados de mis partidos. */
@Composable
fun SummariesListScreen(user: UserModel, onOpen: (String) -> Unit, viewModel: SummariesListViewModel = viewModel()) {
    val s by viewModel.state.collectAsState()
    LaunchedEffect(user.uid) { viewModel.start(user.uid) }
    when {
        s.loading -> LoadingBox()
        s.error != null -> EmptyState(s.error ?: "")
        s.list.isEmpty() -> EmptyState("Aún no hay resúmenes publicados")
        else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(s.list, key = { it.matchId }) { sm ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(sm.matchId) }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${sm.teamName} ${sm.ownScore} - ${sm.rivalScore} ${sm.rival}",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(sm.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(sm.finalFacts, maxLines = 3, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
