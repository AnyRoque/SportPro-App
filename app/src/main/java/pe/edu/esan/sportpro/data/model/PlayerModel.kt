package pe.edu.esan.sportpro.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

/**
 * Documento players/{playerId}.
 * Lo crea el DT manualmente (US-04) o el PAD al registrar a su hijo (US-02).
 */
data class PlayerModel(
    @DocumentId var id: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val category: String = "",
    val name: String = "",
    val lastName: String = "",
    val birthDate: String = "",
    val dni: String = "",
    val sex: String = "",
    val photoUrl: String = "",
    val position: String = "",
    val shirtNumber: Int? = null,
    val heightCm: Int? = null,
    val weightKg: Int? = null,
    val foot: String = "",
    val phone: String = "",
    val email: String = "",
    val emergencyName: String = "",
    val emergencyRelation: String = "",
    val emergencyPhone: String = "",
    /** uid del padre/apoderado (si fue registrado por un PAD) */
    val parentUid: String = "",
    val parentRelation: String = "",
    /** uid de la cuenta JUG vinculada (si existe) */
    val userUid: String = "",
    val dtUid: String = "",
    val linkStatus: String = LinkStatus.PENDING,
    val active: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    @get:Exclude
    val fullName: String get() = "$name $lastName".trim()
}
