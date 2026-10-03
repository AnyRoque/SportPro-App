package pe.edu.esan.sportpro.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.data.model.UserModel

/** Autenticación y perfil de usuario (US-01). */
object FirebaseAuthManager {

    private const val USERS = "users"
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val currentUid: String? get() = auth.currentUser?.uid

    suspend fun registerUser(user: UserModel, password: String): Result<UserModel> {
        return try {
            // Seguridad: el rol ADM nunca se crea desde el registro público
            if (Role.fromOrNull(user.role) !in Role.selfRegister)
                throw Exception("Rol no permitido para registro")

            // Firebase auth
            val authResult = auth.createUserWithEmailAndPassword(user.email.trim(), password).await()
            val uid = authResult.user?.uid ?: throw Exception("Usuario inválido")

            // Firebase firestore
            val saved = user.copy(uid = uid, email = user.email.trim(), active = true)
            firestore.collection(USERS).document(uid).set(saved).await()

            Result.success(saved)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("El correo ya está registrado"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<UserModel> {
        return try {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            getUserProfile()
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Correo o contraseña incorrectos"))
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("Correo o contraseña incorrectos"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Lee users/{uid} para identificar el rol después de iniciar sesión. */
    suspend fun getUserProfile(): Result<UserModel> {
        return try {
            val uid = currentUid ?: throw Exception("No hay sesión iniciada")
            val doc = firestore.collection(USERS).document(uid).get().await()
            val user = doc.toObject(UserModel::class.java) ?: throw Exception("Perfil no encontrado")
            if (!user.active) {
                auth.signOut()
                throw Exception("Tu cuenta está desactivada")
            }
            Result.success(user.copy(uid = uid))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() = auth.signOut()
}
