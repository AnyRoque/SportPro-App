package pe.edu.esan.sportpro.data.remote

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import pe.edu.esan.sportpro.data.model.AdminModel
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.UserModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Repositorio para la gestión dinámica de Administradores Autorizados.
 * Maneja la colección "Admins" (registro de auditoría) y el rol ADM en "users".
 */
object AdminRepository {

    private const val ADMINS_COLLECTION = "Admins"
    private const val USERS_COLLECTION = "users"
    private val firestore = FirebaseFirestore.getInstance()

    /** Lista predefinida de academias / clubes para la asignación de administradores. */
    val AVAILABLE_CLUBS = listOf(
        "Academia Deportiva Los Pinos",
        "Club Deportivo SportPro"
    )

    /**
     * Verifica si un correo ya pertenece a un administrador activo.
     * La fuente de verdad del rol es users/{uid}.role == "ADM" (US-06).
     */
    suspend fun isAdminAuthorized(email: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return false
        return withTimeoutOrNull(4000L) {
            try {
                !firestore.collection(USERS_COLLECTION)
                    .whereEqualTo("email", cleanEmail)
                    .whereEqualTo("role", Role.ADM.name)
                    .get()
                    .await()
                    .isEmpty
            } catch (_: Exception) {
                false
            }
        } ?: false
    }

    /**
     * Registra un nuevo usuario directamente en Firebase Authentication usando createUserWithEmailAndPassword,
     * guarda los datos en Firestore (Admins y users) y lo autoriza inmediatamente.
     */
    suspend fun registerNewAdminWithAuth(
        admin: AdminModel,
        password: String,
        creatorEmail: String,
        context: Context
    ): Result<Unit> {
        return try {
            val cleanEmail = admin.email.trim().lowercase()

            if (cleanEmail.isBlank()) throw Exception("El correo electrónico es obligatorio")
            if (admin.name.isBlank()) throw Exception("El nombre es obligatorio")
            if (admin.lastName.isBlank()) throw Exception("El apellido es obligatorio")
            if (password.length < 6) throw Exception("La contraseña debe tener al menos 6 caracteres")
            if (admin.phone.isBlank()) throw Exception("El teléfono es obligatorio")

            // Validar si ya está autorizado como admin
            if (isAdminAuthorized(cleanEmail)) {
                throw Exception("El correo '$cleanEmail' ya está registrado como administrador")
            }

            // Instancia secundaria de FirebaseApp para NO afectar la sesión del Administrador en curso
            val secondaryApp = try {
                FirebaseApp.getInstance("SecondaryAdminAuthApp")
            } catch (_: Exception) {
                val options = FirebaseApp.getInstance().options
                FirebaseApp.initializeApp(context, options, "SecondaryAdminAuthApp")
            }

            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
            var newUid: String?

            try {
                val authResult = secondaryAuth.createUserWithEmailAndPassword(cleanEmail, password).await()
                newUid = authResult.user?.uid
                secondaryAuth.signOut()
            } catch (_: FirebaseAuthUserCollisionException) {
                throw Exception("El correo '$cleanEmail' ya está registrado en Firebase Authentication")
            } catch (e: Exception) {
                throw Exception("Error al crear usuario en Firebase Auth: ${e.message}")
            }

            val timestamp = currentTimestampString()
            val docId = newUid ?: throw Exception("No se pudo obtener el UID del nuevo administrador")

            val newAdminData = admin.copy(
                id = docId,
                email = cleanEmail,
                role = Role.ADM.name,
                createdBy = creatorEmail.ifBlank { "Admin" },
                createdAt = timestamp,
                active = true
            )

            // Registrar en colección "Admins"
            firestore.collection(ADMINS_COLLECTION)
                .document(cleanEmail)
                .set(newAdminData)
                .await()

            // Registrar en colección "users"
            val userModel = UserModel(
                uid = docId,
                name = admin.name,
                lastName = admin.lastName,
                email = cleanEmail,
                phone = admin.phone,
                role = Role.ADM.name,
                acceptedTerms = true,
                active = true
            )
            firestore.collection(USERS_COLLECTION)
                .document(docId)
                .set(userModel)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun currentTimestampString(): String {
        return try {
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        } catch (_: Exception) {
            System.currentTimeMillis().toString()
        }
    }
}
