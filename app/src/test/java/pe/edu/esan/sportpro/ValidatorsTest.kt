package pe.edu.esan.sportpro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.esan.sportpro.data.model.Role
import pe.edu.esan.sportpro.util.FormValidators
import pe.edu.esan.sportpro.util.RegisterForm
import pe.edu.esan.sportpro.util.Validators
import java.time.LocalDate

/** Pruebas de los criterios de aceptación de validación (US-01, US-02, US-03, US-04). */
class ValidatorsTest {

    private val today = LocalDate.of(2026, 10, 3)

    @Test fun email() {
        assertTrue(Validators.isValidEmail("dt@sportpro.pe"))
        assertFalse(Validators.isValidEmail("dt@sportpro"))
    }

    @Test fun password_minimo8_letra_y_numero() {
        assertTrue(Validators.isValidPassword("futbol2026"))
        assertFalse(Validators.isValidPassword("futbol"))
        assertFalse(Validators.isValidPassword("12345678"))
    }

    @Test fun dni_y_telefono() {
        assertTrue(Validators.isValidDni("12345678"))
        assertFalse(Validators.isValidDni("1234567"))
        assertTrue(Validators.isValidPhone("987654321"))
        assertFalse(Validators.isValidPhone("98765432a"))
    }

    @Test fun fecha_estricta() {
        assertTrue(Validators.isValidBirthDate("15/03/2014", today))
        assertFalse(Validators.isValidBirthDate("31/02/2014", today))
        assertFalse(Validators.isValidBirthDate("15/03/2030", today))
    }

    @Test fun categoria_sugerida_por_edad() {
        assertEquals("Sub-12", Validators.suggestedCategory("15/03/2016", today)) // 10 años
        assertEquals("Sub-15", Validators.suggestedCategory("01/01/2013", today)) // 13 años
        assertEquals("Primera", Validators.suggestedCategory("01/01/2000", today))
    }

    @Test fun normaliza_nombre_de_equipo() {
        assertEquals("los halcones fc", Validators.normalize("  Los  Halcónes FC "))
    }

    @Test fun registro_no_permite_admin() {
        val errors = FormValidators.validateRegister(validForm().copy(role = Role.ADM))
        assertTrue(errors.containsKey("role"))
    }

    @Test fun registro_jugador_menor_bloqueado() {
        val errors = FormValidators.validateRegister(validForm().copy(role = Role.JUG, birthDate = "01/01/2015"))
        assertTrue(errors.containsKey("birthDate"))
    }

    @Test fun registro_valido() {
        assertTrue(FormValidators.validateRegister(validForm()).isEmpty())
    }

    @Test fun camiseta_repetida_en_equipo() {
        val errors = FormValidators.validatePlayer(
            "Ana", "Pérez", "01/01/2014", "", "Sub-15", "Delantero", "10", "", "", "", "",
            "Luis Pérez", "Padre", "987654321", usedShirtNumbers = setOf(10)
        )
        assertTrue(errors.containsKey("shirtNumber"))
    }

    private fun validForm() = RegisterForm(
        name = "Luis", lastName = "Chang", email = "dt@sportpro.pe", phone = "987654321",
        role = Role.DT, password = "futbol2026", confirmPassword = "futbol2026", acceptedTerms = true
    )
}
