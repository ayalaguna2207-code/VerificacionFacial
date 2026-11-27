package com.laura.verificacion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CalibradorTest {

    private static final List<Float> GENUINOS = Arrays.asList(0.10f, 0.20f, 0.25f, 0.50f);
    private static final List<Float> IMPOSTORES = Arrays.asList(0.40f, 0.60f, 0.70f, 0.80f);

    @Test
    public void farCuentaImpostoresAceptados() {
        assertEquals(0.25f, Calibrador.far(IMPOSTORES, 0.45f), 1e-6f);
    }

    @Test
    public void frrCuentaGenuinosRechazados() {
        assertEquals(0.25f, Calibrador.frr(GENUINOS, 0.45f), 1e-6f);
    }

    @Test
    public void exactitudCombinaAmbosGrupos() {
        assertEquals(0.75f, Calibrador.exactitud(GENUINOS, IMPOSTORES, 0.45f), 1e-6f);
    }

    @Test
    public void listasVaciasDanCeroSinFallar() {
        List<Float> vacia = Collections.emptyList();
        assertEquals(0f, Calibrador.far(vacia, 0.5f), 1e-6f);
        assertEquals(0f, Calibrador.frr(vacia, 0.5f), 1e-6f);
        assertEquals(0f, Calibrador.exactitud(vacia, vacia, 0.5f), 1e-6f);
    }

    @Test
    public void eerQuedaEntreGruposBienSeparados() {
        List<Float> g = Arrays.asList(0.10f, 0.20f, 0.25f);
        List<Float> i = Arrays.asList(0.60f, 0.70f, 0.80f);
        Calibrador.Punto p = Calibrador.eer(Calibrador.curva(g, i, 100));
        assertTrue(p.umbral > 0.25f && p.umbral <= 0.60f);
        assertEquals(0f, p.far, 1e-6f);
        assertEquals(0f, p.frr, 1e-6f);
    }

    @Test
    public void laCurvaTieneUnPuntoPorPasoMasUno() {
        assertEquals(101, Calibrador.curva(GENUINOS, IMPOSTORES, 100).size());
    }
}
