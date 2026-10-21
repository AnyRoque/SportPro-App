package pe.edu.esan.sportpro.data.model

import com.google.firebase.firestore.Exclude

/**
 * Modelo para la colección de Administradores Autorizados ("Admins" / "Administradores").
 */
data class AdminModel(
    val id: String = "",
    val name: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val club: String = "",
    val role: String = Role.ADM.name,
    val createdBy: String = "",
    val createdAt: String = "",
    val active: Boolean = true
) {
    @get:Exclude
    val fullName: String get() = "$name $lastName".trim()
}
