package com.laura.verificacion;

import java.util.List;

/** Verificación 1:1: compara un embedding contra los 3 de referencia y decide con el umbral. */
public final class Verificador {

    private Verificador() {
    }

    /** Menor distancia coseno entre el embedding de prueba y las referencias. */
    public static float mejorDistancia(float[] prueba, List<float[]> referencias) {
        if (referencias == null || referencias.isEmpty()) {
            throw new IllegalArgumentException("No hay embeddings de referencia");
        }
        float mejor = Float.MAX_VALUE;
        for (float[] ref : referencias) {
            mejor = Math.min(mejor, FaceComparator.distanciaCoseno(prueba, ref));
        }
        return mejor;
    }

    /** Misma persona si la distancia es válida (>= 0) y menor que el umbral. */
    public static boolean acepta(float distancia, float umbral) {
        return distancia >= 0f && distancia < umbral;
    }
}
