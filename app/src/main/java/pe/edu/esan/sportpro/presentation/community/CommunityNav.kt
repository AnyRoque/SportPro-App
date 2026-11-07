package pe.edu.esan.sportpro.presentation.community

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.presentation.navigation.Routes
import pe.edu.esan.sportpro.presentation.navigation.Secured
import pe.edu.esan.sportpro.presentation.navigation.SecuredContext
import pe.edu.esan.sportpro.presentation.summary.MatchSummaryScreen
import pe.edu.esan.sportpro.presentation.summary.SummariesListScreen

/**
 * Módulo de Comunicación e IA (Any Roque):
 * US-22 Anuncios · US-23 Comunidad · US-24 Reportes · US-25/26/27 Resumen IA.
 */
fun NavGraphBuilder.communityGraph(sec: SecuredContext) {
    val nav = sec.navController
    val members = setOf(Role.DT, Role.JUG, Role.PAD)

    // US-22
    composable(Routes.ANNOUNCEMENTS) {
        sec.Secured("Anuncios") { u ->
            AnnouncementsScreen(
                user = u,
                onNew = { nav.navigate(Routes.announcementForm()) },
                onEdit = { a -> nav.navigate(Routes.announcementForm(a.id)) }
            )
        }
    }
    composable(
        Routes.ANNOUNCEMENT_FORM,
        arguments = listOf(navArgument("editId") { type = NavType.StringType; nullable = true; defaultValue = null })
    ) { entry ->
        val editId = entry.arguments?.getString("editId")
        sec.Secured(if (editId == null) "Nuevo anuncio" else "Editar anuncio", allowed = setOf(Role.ADM, Role.DT), showBack = true) { u ->
            AnnouncementFormScreen(user = u, editId = editId, onDone = { nav.popBackStack() })
        }
    }

    // US-23 / US-24
    composable(Routes.COMMUNITY) {
        sec.Secured("Comunidad", allowed = members) { u ->
            CommunityFeedScreen(
                user = u,
                onNewPost = { nav.navigate(Routes.CREATE_POST) },
                onOpenPost = { p -> nav.navigate(Routes.postDetail(p.id)) }
            )
        }
    }
    composable(Routes.CREATE_POST) {
        sec.Secured("Nueva publicación", allowed = members, showBack = true) { u ->
            CreatePostScreen(user = u, onDone = { nav.popBackStack() })
        }
    }
    composable(Routes.POST_DETAIL, arguments = listOf(navArgument("postId") { type = NavType.StringType })) { entry ->
        val postId = entry.arguments?.getString("postId").orEmpty()
        sec.Secured("Publicación", allowed = members, showBack = true) { u ->
            PostDetailScreen(user = u, postId = postId)
        }
    }

    // US-25 / US-26 / US-27
    composable(Routes.SUMMARIES) {
        sec.Secured("Resúmenes de partidos", allowed = members) { u ->
            SummariesListScreen(user = u, onOpen = { id -> nav.navigate(Routes.matchSummary(id)) })
        }
    }
    composable(Routes.MATCH_SUMMARY, arguments = listOf(navArgument("matchId") { type = NavType.StringType })) { entry ->
        val matchId = entry.arguments?.getString("matchId").orEmpty()
        sec.Secured("Resumen del partido", allowed = members, showBack = true) { u ->
            MatchSummaryScreen(user = u, matchId = matchId)
        }
    }
}
