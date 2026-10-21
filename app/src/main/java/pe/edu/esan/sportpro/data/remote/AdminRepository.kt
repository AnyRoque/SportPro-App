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
 * Maneja la colección "Admins" (y "Administradores") en Cloud Firestore.
 */
object AdminRepository {

    private const val ADMINS_COLLECTION = "Admins"
    private const val ALT_ADMINS_COLLECTION = "Administradores"
    private const val USERS_COLLECTION = "users"
    private val firestore = FirebaseFirestore.getInstance()

    /** Lista predefinida de academias / clubes para la asignación de administradores. */
    val AVAILABLE_CLUBS = listOf(
        "Academia Deportiva Los Pinos",
        "Club Deportivo SportPro"
    )

    /**
     * Verifica dinámicamente si un correo electrónico pertenece a la lista de administradores autorizados.
     */
    suspend fun isAdminAuthorized(email: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return false

        // Manejo especial para cuenta inicial de prueba
        if (cleanEmail == "michi@gmail.com") {
            withTimeoutOrNull(3000L) { ensureDefaultAdminSeeded(cleanEmail) }
            return true
        }

        return withTimeoutOrNull(4000L) {
            try {
                // Consulta en la colección "Admins"
                val queryAdmins = firestore.collection(ADMINS_COLLECTION)
                    .whereEqualTo("email", cleanEmail)
                    .get()
                    .await()

                if (!queryAdmins.isEmpty) {
                    val adminDoc = queryAdmins.documents.first()
                    val active = adminDoc.getBoolean("active") ?: true
                    if (active) return@withTimeoutOrNull true
                }

                // Consulta de respaldo en la colección "Administradores"
                val queryAltAdmins = firestore.collection(ALT_ADMINS_COLLECTION)
                    .whereEqualTo("email", cleanEmail)
                    .get()
                    .await()

                if (!queryAltAdmins.isEmpty) {
                    val active = queryAltAdmins.documents.first().getBoolean("active") ?: true
                    if (active) return@withTimeoutOrNull true
                }

                // Consulta secundaria en la colección "users" con rol ADM
                val queryUsers = firestore.collection(USERS_COLLECTION)
                    .whereEqualTo("email", cleanEmail)
                    .whereEqualTo("role", Role.ADM.name)
                    .get()
                    .await()

                !queryUsers.isEmpty
            } catch (_: Exception) {
                false
            }
        } ?: false
    }

    /**
     * Garantiza que la cuenta inicial de pruebas 'michi@gmail.com' esté registrada en la colección de administradores autorizados.
     */
    suspend fun ensureDefaultAdminSeeded(email: String) {
        val cleanEmail = email.trim().lowercase()
        try {
            val docRef = firestore.collection(ADMINS_COLLECTION).document(cleanEmail)
            val doc = docRef.get().await()

            if (!doc.exists()) {
                val defaultAdmin = hashMapOf(
                    "id" to cleanEmail,
                    "name" to "Michi",
                    "lastName" to "Administrador",
                    "email" to cleanEmail,
                    "phone" to "999888777",
                    "club" to "Club Deportivo SportPro",
                    "role" to Role.ADM.name,
                    "createdBy" to "SYSTEM_SEED",
                    "createdAt" to currentTimestampString(),
                    "active" to true
                )
                docRef.set(defaultAdmin).await()
                // Duplicar en colección alternativa por compatibilidad
                firestore.collection(ALT_ADMINS_COLLECTION).document(cleanEmail).set(defaultAdmin).await()
            }

            // Asegurar que exista perfil en la colección users si hay UID activo
            val currentUid = FirebaseAuthManager.currentUid
            if (currentUid != null) {
                val userDocRef = firestore.collection(USERS_COLLECTION).document(currentUid)
                val userDoc = userDocRef.get().await()
                if (!userDoc.exists() || userDoc.getString("role") != Role.ADM.name) {
                    val adminUser = UserModel(
                        uid = currentUid,
                        name = "Michi",
                        lastName = "Administrador",
                        email = cleanEmail,
                        phone = "999888777",
                        role = Role.ADM.name,
                        acceptedTerms = true,
                        active = true
                    )
                    userDocRef.set(adminUser).await()
                }
            }
        } catch (_: Exception) {
            // Ignorar errores menores en el seeding automático
        }
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
            val docId = newUid ?: cleanEmail

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

            // Registrar en colección "Administradores" (copia de respaldo)
            firestore.collection(ALT_ADMINS_COLLECTION)
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
