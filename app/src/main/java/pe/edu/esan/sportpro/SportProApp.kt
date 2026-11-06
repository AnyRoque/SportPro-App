package pe.edu.esan.sportpro

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

/**
 * Clase Application de la aplicación SportPro.
 * Configura la caché persistente offline de Cloud Firestore para garantzar funcionamiento 100% gratuito (Plan Spark de Firebase).
 */
class SportProApp : Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            // Configurar almacenamiento en memoria/caché local ilimitada para lectura/escritura sin conexión
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                        .build()
                )
                .build()

            FirebaseFirestore.getInstance().firestoreSettings = settings
        } catch (_: Exception) {
            // Firestore ya inicializado por el SDK
        }
    }
}
