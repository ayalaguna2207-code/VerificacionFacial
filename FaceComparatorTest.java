package com.laura.verificacion;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FaceComparatorTest {

    private static final float DELTA = 1e-5f;

    @Test
    public void vectoresIgualesTienenDistanciaCero() {
        float[] v = {0.3f, -0.5f, 0.8f};
        assertEquals(0f, FaceComparator.distanciaCoseno(v, v), DELTA);
        assertEquals(1f, FaceComparator.similitudCoseno(v, v), DELTA);
    }

    @Test
    public void vectoresOrtogonalesTienenDistanciaUno() {
        assertEquals(1f, FaceComparator.distanciaCoseno(new float[]{1, 0}, new float[]{0, 1}), DELTA);
    }

    @Test
    public void vectoresOpuestosTienenDistanciaDos() {
        assertEquals(2f, FaceComparator.distanciaCoseno(new float[]{1, 2}, new float[]{-1, -2}), DELTA);
    }

    @Test
    public void laEscalaNoCambiaLaDistancia() {
        assertEquals(0f, FaceComparator.distanciaCoseno(new float[]{1, 2, 3}, new float[]{2, 4, 6}), DELTA);
    }

    @Test
    public void unVectorNuloNoCausaDivisionPorCero() {
        assertEquals(0f, FaceComparator.similitudCoseno(new float[]{0, 0}, new float[]{1, 1}), DELTA);
    }
}
