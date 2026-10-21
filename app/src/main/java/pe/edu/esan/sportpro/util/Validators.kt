package pe.edu.esan.sportpro.util

import java.text.Normalizer
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/** Validaciones reutilizables (sin dependencias de Android para poder probarlas con JUnit). */
object Validators {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    val DATE_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)

    fun isValidEmail(email: String) = EMAIL_REGEX.matches(email.trim())

    /** Mínimo 8 caracteres, al menos una letra y un número. */
    fun isValidPassword(p: String) = p.length >= 8 && p.any { it.isLetter() } && p.any { it.isDigit() }

    fun isValidPhone(p: String) = p.length == 9 && p.all { it.isDigit() }

    fun isValidDni(d: String) = d.length == 8 && d.all { it.isDigit() }

    fun parseDate(s: String): LocalDate? = try {
        LocalDate.parse(s.trim(), DATE_FORMAT)
    } catch (e: Exception) {
        null
    }

    fun formatDate(date: LocalDate): String = date.format(DATE_FORMAT)

    /** Fecha válida y anterior a hoy. */
    fun isValidBirthDate(s: String, today: LocalDate = LocalDate.now()): Boolean =
        parseDate(s)?.isBefore(today) ?: false

    fun ageFrom(s: String, today: LocalDate = LocalDate.now()): Int? =
        parseDate(s)?.let { Period.between(it, today).years }

    fun isMinor(birthDate: String, today: LocalDate = LocalDate.now()): Boolean =
        (ageFrom(birthDate, today) ?: 0) < 18

    fun isIntInRange(value: String, min: Int, max: Int): Boolean =
        value.trim().toIntOrNull()?.let { it in min..max } ?: false

    /** Categoría sugerida según la edad (US-02). */
    fun suggestedCategory(birthDate: String, today: LocalDate = LocalDate.now()): String? {
        val age = ageFrom(birthDate, today) ?: return null
        return when {
            age < 8 -> "Sub-8"
            age < 10 -> "Sub-10"
            age < 12 -> "Sub-12"
            age < 15 -> "Sub-15"
            age < 17 -> "Sub-17"
            else -> "Primera"
        }
    }

    /** "Los Halcones FC" -> "los halcones fc" (sin tildes ni espacios extra) */
    fun normalize(text: String): String =
        Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .replace(Regex("\\s+"), " ")
}
