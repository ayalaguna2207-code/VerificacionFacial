package com.laura.verificacion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class LightAnalyzerTest {

    private static int[] gris(int nivel, int cantidad) {
        int[] p = new int[cantidad];
        for (int i = 0; i < cantidad; i++) {
            p[i] = 0xFF000000 | (nivel << 16) | (nivel << 8) | nivel;
        }
        return p;
    }

    @Test
    public void laLuminanciaDeUnGrisEsSuNivel() {
        assertEquals(128f, LightAnalyzer.luminanciaMedia(gris(128, 100)), 0.5f);
    }

    @Test
    public void unaImagenOscuraSeRechaza() {
        assertNotNull(LightAnalyzer.problema(LightAnalyzer.luminanciaMedia(gris(20, 50))));
    }

    @Test
    public void unaImagenSobreexpuestaSeRechaza() {
        assertNotNull(LightAnalyzer.problema(LightAnalyzer.luminanciaMedia(gris(250, 50))));
    }

    @Test
    public void unaIluminacionNormalSeAcepta() {
        assertNull(LightAnalyzer.problema(LightAnalyzer.luminanciaMedia(gris(130, 50))));
    }

    @Test
    public void unaListaVaciaNoFalla() {
        assertEquals(0f, LightAnalyzer.luminanciaMedia(new int[0]), 0f);
    }
}
