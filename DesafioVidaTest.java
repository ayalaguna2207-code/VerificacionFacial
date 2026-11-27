package com.laura.verificacion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class DesafioVidaTest {

    @Test
    public void parpadeoSeSuperaConAbiertoCerradoAbierto() {
        DesafioVida d = new DesafioVida(DesafioVida.Tipo.PARPADEO, 5000, 0);
        d.actualizar(0.9f, 0.9f, 0f, 100);
        d.actualizar(0.1f, 0.1f, 0f, 200);
        assertEquals(DesafioVida.Estado.EN_CURSO, d.getEstado());
        d.actualizar(0.9f, 0.9f, 0f, 300);
        assertEquals(DesafioVida.Estado.SUPERADO, d.getEstado());
    }

    @Test
    public void ojosSiempreAbiertosNoSuperanElParpadeo() {
        DesafioVida d = new DesafioVida(DesafioVida.Tipo.PARPADEO, 5000, 0);
        for (int i = 0; i < 30; i++) {
            d.actualizar(0.95f, 0.95f, 0f, i * 100L);
        }
        assertEquals(DesafioVida.Estado.EN_CURSO, d.getEstado());
    }

    @Test
    public void giroIzquierdaExigeVerFrenteYLuegoGirar() {
        DesafioVida d = new DesafioVida(DesafioVida.Tipo.GIRO_IZQUIERDA, 5000, 0);
        d.actualizar(0.9f, 0.9f, 30f, 100); // ya venía girado: no cuenta
        assertEquals(DesafioVida.Estado.EN_CURSO, d.getEstado());
        d.actualizar(0.9f, 0.9f, 0f, 200);
        d.actualizar(0.9f, 0.9f, 30f, 300);
        assertEquals(DesafioVida.Estado.SUPERADO, d.getEstado());
    }

    @Test
    public void giroDerechaNoSeSuperaGirandoALaIzquierda() {
        DesafioVida d = new DesafioVida(DesafioVida.Tipo.GIRO_DERECHA, 5000, 0);
        d.actualizar(0.9f, 0.9f, 0f, 100);
        d.actualizar(0.9f, 0.9f, 35f, 200);
        assertEquals(DesafioVida.Estado.EN_CURSO, d.getEstado());
        d.actualizar(0.9f, 0.9f, -35f, 300);
        assertEquals(DesafioVida.Estado.SUPERADO, d.getEstado());
    }

    @Test
    public void expiraCuandoSeAcabaElTiempo() {
        DesafioVida d = new DesafioVida(DesafioVida.Tipo.PARPADEO, 1000, 0);
        assertEquals(DesafioVida.Estado.EXPIRADO, d.revisarTiempo(2000));
        d.actualizar(0.9f, 0.9f, 0f, 2100);
        d.actualizar(0.1f, 0.1f, 0f, 2200);
        d.actualizar(0.9f, 0.9f, 0f, 2300);
        assertEquals(DesafioVida.Estado.EXPIRADO, d.getEstado());
    }

    @Test
    public void elAzarEligeLosTresTipos() {
        Random r = new Random(7);
        Set<DesafioVida.Tipo> vistos = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            vistos.add(DesafioVida.aleatorio(r, 1000, 0).getTipo());
        }
        assertTrue(vistos.size() == 3);
    }
}
