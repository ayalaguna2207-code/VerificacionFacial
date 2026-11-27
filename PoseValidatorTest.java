package com.laura.verificacion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PoseValidatorTest {

    private static final float S = PoseValidator.SIGNO_IZQUIERDA;

    @Test
    public void frenteAceptaAngulosPequenos() {
        assertTrue(PoseValidator.cumple(Pose.FRENTE, 0f, 0f));
        assertTrue(PoseValidator.cumple(Pose.FRENTE, 8f, 5f));
        assertFalse(PoseValidator.cumple(Pose.FRENTE, 25f, 0f));
    }

    @Test
    public void izquierdaExigeGiroHaciaLaIzquierda() {
        assertTrue(PoseValidator.cumple(Pose.IZQUIERDA, 25f * S, 0f));
        assertFalse(PoseValidator.cumple(Pose.IZQUIERDA, 5f * S, 0f));
        assertFalse(PoseValidator.cumple(Pose.IZQUIERDA, -25f * S, 0f));
        assertFalse(PoseValidator.cumple(Pose.IZQUIERDA, 70f * S, 0f));
    }

    @Test
    public void derechaExigeGiroHaciaLaDerecha() {
        assertTrue(PoseValidator.cumple(Pose.DERECHA, -25f * S, 0f));
        assertFalse(PoseValidator.cumple(Pose.DERECHA, 25f * S, 0f));
    }

    @Test
    public void elCabeceoExcesivoInvalidaCualquierPose() {
        assertFalse(PoseValidator.cumple(Pose.FRENTE, 0f, 30f));
        assertFalse(PoseValidator.cumple(Pose.IZQUIERDA, 25f * S, -30f));
    }

    @Test
    public void lasSugerenciasIndicanQueCorregir() {
        assertEquals("Pose correcta, no te muevas", PoseValidator.sugerencia(Pose.FRENTE, 0f, 0f));
        assertEquals("Gira más hacia tu izquierda", PoseValidator.sugerencia(Pose.IZQUIERDA, 3f * S, 0f));
        assertEquals("Gira más hacia tu derecha", PoseValidator.sugerencia(Pose.DERECHA, -3f * S, 0f));
    }
}
