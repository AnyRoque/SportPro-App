package pe.edu.esan.sportpro.data.model

import com.google.firebase.firestore.Exclude

/** Documento users/{uid} */
data class UserModel(
    val uid: String = "",
    val name: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "",
    val birthDate: String = "",
    val acceptedTerms: Boolean = false,
    val active: Boolean = true
) {
    @get:Exclude
    val fullName: String get() = "$name $lastName".trim()

    @get:Exclude
    val roleEnum: Role? get() = Role.fromOrNull(role)
}
