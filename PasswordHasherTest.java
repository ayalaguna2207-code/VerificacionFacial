package com.laura.verificacion;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PasswordHasherTest {

    @Test
    public void laClaveCorrectaSeVerifica() {
        String guardado = PasswordHasher.hash("Clave1234");
        assertTrue(PasswordHasher.verificar("Clave1234", guardado));
    }

    @Test
    public void unaClaveIncorrectaSeRechaza() {
        String guardado = PasswordHasher.hash("Clave1234");
        assertFalse(PasswordHasher.verificar("clave1234", guardado));
    }

    @Test
    public void lasSalesHacenQueLosHashesSeanDistintos() {
        assertNotEquals(PasswordHasher.hash("Clave1234"), PasswordHasher.hash("Clave1234"));
    }

    @Test
    public void elHashNoContieneLaClaveEnTextoPlano() {
        assertFalse(PasswordHasher.hash("Clave1234").contains("Clave1234"));
    }

    @Test
    public void unFormatoInvalidoSeRechazaSinFallar() {
        assertFalse(PasswordHasher.verificar("x", "basura"));
        assertFalse(PasswordHasher.verificar(null, "a$b$c$d"));
    }
}
