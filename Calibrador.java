package com.laura.verificacion;

import java.util.ArrayList;
import java.util.List;

/** Calcula FAR, FRR, exactitud y la curva contra el umbral. Distancias en [0, 1] aprox. */
public final class Calibrador {

    public static class Punto {
        public final float umbral;
        public final float far;
        public final float frr;

        public Punto(float umbral, float far, float frr) {
            this.umbral = umbral;
            this.far = far;
            this.frr = frr;
        }
    }

    private Calibrador() {
    }

    public static List<Punto> curva(List<Float> genuinos, List<Float> impostores, int pasos) {
        List<Punto> puntos = new ArrayList<>();
        for (int i = 0; i <= pasos; i++) {
            float t = (float) i / pasos;
            puntos.add(new Punto(t, far(impostores, t), frr(genuinos, t)));
        }
        return puntos;
    }

    /** FAR: proporción de impostores aceptados (distancia menor al umbral). */
    public static float far(List<Float> impostores, float umbral) {
        if (impostores.isEmpty()) {
            return 0f;
        }
        int c = 0;
        for (float d : impostores) {
            if (d < umbral) {
                c++;
            }
        }
        return (float) c / impostores.size();
    }

    /** FRR: proporción de genuinos rechazados (distancia mayor o igual al umbral). */
    public static float frr(List<Float> genuinos, float umbral) {
        if (genuinos.isEmpty()) {
            return 0f;
        }
        int c = 0;
        for (float d : genuinos) {
            if (d >= umbral) {
                c++;
            }
        }
        return (float) c / genuinos.size();
    }

    public static float exactitud(List<Float> genuinos, List<Float> impostores, float umbral) {
        int ok = 0;
        for (float d : genuinos) {
            if (d < umbral) {
                ok++;
            }
        }
        for (float d : impostores) {
            if (d >= umbral) {
                ok++;
            }
        }
        int total = genuinos.size() + impostores.size();
        return total == 0 ? 0f : (float) ok / total;
    }

    /**
     * Punto donde FAR y FRR se cruzan (EER). Si hay varios puntos igual de buenos
     * (grupos bien separados), devuelve el del medio para que el umbral quede con margen.
     */
    public static Punto eer(List<Punto> curva) {
        float menorDiferencia = Float.MAX_VALUE;
        for (Punto p : curva) {
            menorDiferencia = Math.min(menorDiferencia, Math.abs(p.far - p.frr));
        }
        float menorError = Float.MAX_VALUE;
        for (Punto p : curva) {
            if (Math.abs(p.far - p.frr) - menorDiferencia < 1e-6f) {
                menorError = Math.min(menorError, p.far + p.frr);
            }
        }
        List<Punto> mejores = new ArrayList<>();
        for (Punto p : curva) {
            if (Math.abs(p.far - p.frr) - menorDiferencia < 1e-6f && p.far + p.frr - menorError < 1e-6f) {
                mejores.add(p);
            }
        }
        return mejores.get(mejores.size() / 2);
    }
}
