package com.laura.verificacion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VerificadorTest {

    private static final List<float[]> REFS = Arrays.asList(
            new float[]{0f, 1f}, new float[]{1f, 0f}, new float[]{0.9f, 0.1f});

    @Test
    public void usaLaMejorDeLasTresReferencias() {
        assertEquals(0f, Verificador.mejorDistancia(new float[]{1f, 0f}, REFS), 1e-5f);
    }

    @Test
    public void aceptaSoloSiLaDistanciaEsMenorAlUmbral() {
        assertTrue(Verificador.acepta(0.30f, 0.45f));
        assertFalse(Verificador.acepta(0.45f, 0.45f));
        assertFalse(Verificador.acepta(0.80f, 0.45f));
    }

    @Test
    public void unIntentoNoComparadoNuncaSeAcepta() {
        assertFalse(Verificador.acepta(Intento.NO_COMPARADO, 0.45f));
    }

    @Test(expected = IllegalArgumentException.class)
    public void sinReferenciasLanzaError() {
        Verificador.mejorDistancia(new float[]{1f, 0f}, Collections.<float[]>emptyList());
    }
}
