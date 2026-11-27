package com.laura.verificacion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LockoutPolicyTest {

    @Test
    public void alInicioNoEstaBloqueado() {
        assertFalse(new LockoutPolicy().estaBloqueado(1000L));
    }

    @Test
    public void tresFallosBloqueanElAcceso() {
        LockoutPolicy p = new LockoutPolicy();
        p.registrarFallo(0L);
        p.registrarFallo(0L);
        assertFalse(p.estaBloqueado(0L));
        assertEquals(1, p.intentosRestantes());
        p.registrarFallo(0L);
        assertTrue(p.estaBloqueado(1L));
        assertEquals(60L, p.segundosRestantes(0L));
    }

    @Test
    public void elBloqueoTerminaConElTiempo() {
        LockoutPolicy p = new LockoutPolicy();
        for (int i = 0; i < LockoutPolicy.MAX_FALLOS; i++) {
            p.registrarFallo(0L);
        }
        assertFalse(p.estaBloqueado(LockoutPolicy.BLOQUEO_MS));
        assertEquals(0L, p.segundosRestantes(LockoutPolicy.BLOQUEO_MS + 5));
    }

    @Test
    public void unExitoReiniciaLosFallos() {
        LockoutPolicy p = new LockoutPolicy();
        p.registrarFallo(0L);
        p.registrarFallo(0L);
        p.registrarExito();
        assertEquals(LockoutPolicy.MAX_FALLOS, p.intentosRestantes());
    }
}
