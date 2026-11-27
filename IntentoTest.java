package com.laura.verificacion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IntentoTest {

    @Test
    public void csvIdaYVueltaConservaLosDatos() {
        Intento t = new Intento(Intento.GENUINO, "ALTA", 0.2345f, 87, true, 41.5f, 130f, 12345L);
        Intento r = Intento.deCsv(t.aCsv());
        assertNotNull(r);
        assertEquals(Intento.GENUINO, r.etiqueta);
        assertEquals("ALTA", r.iluminacion);
        assertEquals(0.2345f, r.distancia, 1e-4f);
        assertEquals(87L, r.tiempoMs);
        assertTrue(r.vidaSuperada);
        assertEquals(12345L, r.marca);
    }

    @Test
    public void unaLineaDanadaSeIgnora() {
        assertNull(Intento.deCsv("basura,1,2"));
        assertNull(Intento.deCsv(""));
    }

    @Test
    public void distingueAtaquesYComparados() {
        Intento a = new Intento(Intento.ATAQUE_VIDEO, "MEDIA", Intento.NO_COMPARADO, 0, false, 0f, 0f, 1L);
        assertTrue(a.esAtaque());
        assertFalse(a.fueComparado());
        Intento g = new Intento(Intento.GENUINO, "MEDIA", 0.3f, 50, true, 0f, 0f, 1L);
        assertFalse(g.esAtaque());
        assertTrue(g.fueComparado());
    }
}
