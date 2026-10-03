package pe.edu.esan.sportpro.util

import pe.edu.esan.sportpro.data.model.Role

/** Datos del formulario de registro (US-01). */
data class RegisterForm(
    val name: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: Role? = null,
    val birthDate: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val acceptedTerms: Boolean = false
)

/** Devuelve un mapa campo -> mensaje de error. Vacío = formulario válido. */
object FormValidators {

    fun validateRegister(f: RegisterForm): Map<String, String> {
        val e = mutableMapOf<String, String>()
        if (f.name.isBlank()) e["name"] = "Ingresa tus nombres"
        if (f.lastName.isBlank()) e["lastName"] = "Ingresa tus apellidos"
        if (!Validators.isValidEmail(f.email)) e["email"] = "Correo con formato inválido"
        if (!Validators.isValidPhone(f.phone)) e["phone"] = "El teléfono debe tener 9 dígitos"
        if (f.role == null) e["role"] = "Selecciona un rol"
        if (f.role == Role.ADM) e["role"] = "El rol Administrador no está disponible para registro"
        if (f.role == Role.JUG) {
            when {
                !Validators.isValidBirthDate(f.birthDate) -> e["birthDate"] = "Ingresa una fecha de nacimiento válida"
                Validators.isMinor(f.birthDate) -> e["birthDate"] =
                    "Los jugadores menores de edad deben ser registrados por su padre o apoderado"
            }
        }
        if (!Validators.isValidPassword(f.password)) e["password"] =
            "Mínimo 8 caracteres, con al menos una letra y un número"
        if (f.password != f.confirmPassword) e["confirmPassword"] = "Las contraseñas no coinciden"
        if (!f.acceptedTerms) e["terms"] = "Debes aceptar los términos y la política de privacidad"
        return e
    }

    fun validateTeam(name: String, category: String, field: String, district: String): Map<String, String> {
        val e = mutableMapOf<String, String>()
        if (name.isBlank()) e["name"] = "Ingresa el nombre del equipo"
        if (category.isBlank()) e["category"] = "Selecciona la categoría"
        if (field.isBlank()) e["field"] = "Ingresa el campo de entrenamiento"
        if (district.isBlank()) e["district"] = "Ingresa el distrito"
        return e
    }

    /**
     * Validación del perfil del jugador (US-04) y del registro de hijo (US-02).
     * [requireSportsData] = true cuando lo llena el DT (posición y emergencia obligatorias).
     */
    fun validatePlayer(
        name: String, lastName: String, birthDate: String, dni: String,
        category: String, position: String, shirtNumber: String, heightCm: String, weightKg: String,
        phone: String, email: String,
        emergencyName: String, emergencyRelation: String, emergencyPhone: String,
        usedShirtNumbers: Set<Int> = emptySet(),
        requireSportsData: Boolean = true
    ): Map<String, String> {
        val e = mutableMapOf<String, String>()
        if (name.isBlank()) e["name"] = "Ingresa los nombres"
        if (lastName.isBlank()) e["lastName"] = "Ingresa los apellidos"
        if (!Validators.isValidBirthDate(birthDate)) e["birthDate"] = "Fecha de nacimiento inválida"
        if (dni.isNotBlank() && !Validators.isValidDni(dni)) e["dni"] = "El DNI debe tener 8 dígitos"
        if (category.isBlank()) e["category"] = "Selecciona la categoría"
        if (requireSportsData && position.isBlank()) e["position"] = "Selecciona la posición"
        if (shirtNumber.isNotBlank()) {
            if (!Validators.isIntInRange(shirtNumber, 1, 99)) e["shirtNumber"] = "Número entre 1 y 99"
            else if (shirtNumber.trim().toInt() in usedShirtNumbers) e["shirtNumber"] = "Número ya usado en el equipo"
        }
        if (heightCm.isNotBlank() && !Validators.isIntInRange(heightCm, 100, 220)) e["heightCm"] = "Altura entre 100 y 220 cm"
        if (weightKg.isNotBlank() && !Validators.isIntInRange(weightKg, 20, 150)) e["weightKg"] = "Peso entre 20 y 150 kg"
        if (phone.isNotBlank() && !Validators.isValidPhone(phone)) e["phone"] = "El teléfono debe tener 9 dígitos"
        if (email.isNotBlank() && !Validators.isValidEmail(email)) e["email"] = "Correo inválido"
        if (emergencyName.isBlank()) e["emergencyName"] = "Ingresa el contacto de emergencia"
        if (emergencyRelation.isBlank()) e["emergencyRelation"] = "Indica el parentesco"
        if (!Validators.isValidPhone(emergencyPhone)) e["emergencyPhone"] = "El teléfono debe tener 9 dígitos"
        return e
    }
}
