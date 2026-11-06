package pe.edu.esan.sportpro.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

object LineupConstants {
    const val MAX_SUBSTITUTES = 7
}

/**
 * Coordenadas y posición táctica en el campo gráfico para un jugador titular (US-18).
 */
data class TacticalPosition(
    val slotIndex: Int = 0,
    val positionName: String = "", // e.g. "Portero", "Defensa central", "Mediocampista", "Delantero"
    val xRatio: Float = 0.5f,      // Coordenada X relativa (0.0 a 1.0)
    val yRatio: Float = 0.8f,      // Coordenada Y relativa (0.0 a 1.0)
    val playerId: String? = null,
    val playerName: String? = null,
    val shirtNumber: Int? = null
)

/**
 * Registro completo de alineación e inicio de partido (US-18).
 * Documento matches/{matchId}/lineup/starting
 */
data class LineupModel(
    @DocumentId var id: String = "starting",
    val matchId: String = "",
    val formation: String = "4-3-3",
    val modality: Int = 11,
    val positions: List<TacticalPosition> = emptyList(),
    val substitutePlayerIds: List<String> = emptyList(),
    val savedBy: String = "",
    @ServerTimestamp val savedAt: Timestamp? = null
)

/**
 * Diseños tácticos de posiciones predeterminadas en el campo gráfico.
 */
object TacticalLayouts {

    fun generateDefaultPositions(formation: String, modality: Int): List<TacticalPosition> {
        return when (modality) {
            7 -> generateF7Positions(formation)
            9 -> generateF9Positions(formation)
            else -> generateF11Positions(formation)
        }
    }

    private fun generateF7Positions(formation: String): List<TacticalPosition> {
        val gk = TacticalPosition(0, "Portero", 0.5f, 0.88f)
        return when (formation) {
            "3-2-1" -> listOf(
                gk,
                TacticalPosition(1, "Defensa izq", 0.22f, 0.68f),
                TacticalPosition(2, "Defensa cen", 0.50f, 0.68f),
                TacticalPosition(3, "Defensa der", 0.78f, 0.68f),
                TacticalPosition(4, "Medio izq", 0.35f, 0.44f),
                TacticalPosition(5, "Medio der", 0.65f, 0.44f),
                TacticalPosition(6, "Delantero", 0.50f, 0.20f)
            )
            "2-2-2" -> listOf(
                gk,
                TacticalPosition(1, "Defensa izq", 0.35f, 0.68f),
                TacticalPosition(2, "Defensa der", 0.65f, 0.68f),
                TacticalPosition(3, "Medio izq", 0.35f, 0.44f),
                TacticalPosition(4, "Medio der", 0.65f, 0.44f),
                TacticalPosition(5, "Delantero izq", 0.35f, 0.20f),
                TacticalPosition(6, "Delantero der", 0.65f, 0.20f)
            )
            else -> listOf( // "2-3-1" por defecto
                gk,
                TacticalPosition(1, "Defensa izq", 0.35f, 0.68f),
                TacticalPosition(2, "Defensa der", 0.65f, 0.68f),
                TacticalPosition(3, "Medio izq", 0.20f, 0.44f),
                TacticalPosition(4, "Medio cen", 0.50f, 0.44f),
                TacticalPosition(5, "Medio der", 0.80f, 0.44f),
                TacticalPosition(6, "Delantero", 0.50f, 0.20f)
            )
        }
    }

    private fun generateF9Positions(formation: String): List<TacticalPosition> {
        val gk = TacticalPosition(0, "Portero", 0.5f, 0.88f)
        return when (formation) {
            "3-4-1" -> listOf(
                gk,
                TacticalPosition(1, "Defensa izq", 0.22f, 0.68f),
                TacticalPosition(2, "Defensa cen", 0.50f, 0.68f),
                TacticalPosition(3, "Defensa der", 0.78f, 0.68f),
                TacticalPosition(4, "Medio izq", 0.18f, 0.45f),
                TacticalPosition(5, "Medio cen izq", 0.40f, 0.45f),
                TacticalPosition(6, "Medio cen der", 0.60f, 0.45f),
                TacticalPosition(7, "Medio der", 0.82f, 0.45f),
                TacticalPosition(8, "Delantero", 0.50f, 0.20f)
            )
            else -> listOf( // "3-3-2" por defecto
                gk,
                TacticalPosition(1, "Defensa izq", 0.22f, 0.68f),
                TacticalPosition(2, "Defensa cen", 0.50f, 0.68f),
                TacticalPosition(3, "Defensa der", 0.78f, 0.68f),
                TacticalPosition(4, "Medio izq", 0.22f, 0.45f),
                TacticalPosition(5, "Medio cen", 0.50f, 0.45f),
                TacticalPosition(6, "Medio der", 0.78f, 0.45f),
                TacticalPosition(7, "Delantero izq", 0.35f, 0.20f),
                TacticalPosition(8, "Delantero der", 0.65f, 0.20f)
            )
        }
    }

    private fun generateF11Positions(formation: String): List<TacticalPosition> {
        val gk = TacticalPosition(0, "Portero", 0.5f, 0.88f)
        return when (formation) {
            "4-4-2" -> listOf(
                gk,
                TacticalPosition(1, "Lateral izq", 0.15f, 0.68f),
                TacticalPosition(2, "Central izq", 0.38f, 0.68f),
                TacticalPosition(3, "Central der", 0.62f, 0.68f),
                TacticalPosition(4, "Lateral der", 0.85f, 0.68f),
                TacticalPosition(5, "Medio izq", 0.15f, 0.45f),
                TacticalPosition(6, "Pivote izq", 0.38f, 0.45f),
                TacticalPosition(7, "Pivote der", 0.62f, 0.45f),
                TacticalPosition(8, "Medio der", 0.85f, 0.45f),
                TacticalPosition(9, "Delantero izq", 0.35f, 0.20f),
                TacticalPosition(10, "Delantero der", 0.65f, 0.20f)
            )
            "3-5-2" -> listOf(
                gk,
                TacticalPosition(1, "Central izq", 0.25f, 0.68f),
                TacticalPosition(2, "Central cen", 0.50f, 0.68f),
                TacticalPosition(3, "Central der", 0.75f, 0.68f),
                TacticalPosition(4, "Carrilero izq", 0.12f, 0.45f),
                TacticalPosition(5, "Pivote izq", 0.31f, 0.45f),
                TacticalPosition(6, "Pivote cen", 0.50f, 0.45f),
                TacticalPosition(7, "Pivote der", 0.69f, 0.45f),
                TacticalPosition(8, "Carrilero der", 0.88f, 0.45f),
                TacticalPosition(9, "Delantero izq", 0.35f, 0.20f),
                TacticalPosition(10, "Delantero der", 0.65f, 0.20f)
            )
            else -> listOf( // "4-3-3" por defecto
                gk,
                TacticalPosition(1, "Lateral izq", 0.15f, 0.68f),
                TacticalPosition(2, "Central izq", 0.38f, 0.68f),
                TacticalPosition(3, "Central der", 0.62f, 0.68f),
                TacticalPosition(4, "Lateral der", 0.85f, 0.68f),
                TacticalPosition(5, "Medio izq", 0.25f, 0.45f),
                TacticalPosition(6, "Pivote cen", 0.50f, 0.45f),
                TacticalPosition(7, "Medio der", 0.75f, 0.45f),
                TacticalPosition(8, "Extremo izq", 0.20f, 0.20f),
                TacticalPosition(9, "Delantero cen", 0.50f, 0.20f),
                TacticalPosition(10, "Extremo der", 0.80f, 0.20f)
            )
        }
    }
}
