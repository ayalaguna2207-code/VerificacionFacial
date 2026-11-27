package com.laura.verificacion;

/** Compara dos embeddings con la distancia coseno: 0 = idénticos, valores altos = personas distintas. */
public final class FaceComparator {

    private FaceComparator() {
    }

    public static float similitudCoseno(float[] a, float[] b) {
        double punto = 0, normaA = 0, normaB = 0;
        for (int i = 0; i < a.length; i++) {
            punto += a[i] * b[i];
            normaA += a[i] * a[i];
            normaB += b[i] * b[i];
        }
        if (normaA == 0 || normaB == 0) {
            return 0f;
        }
        return (float) (punto / (Math.sqrt(normaA) * Math.sqrt(normaB)));
    }

    public static float distanciaCoseno(float[] a, float[] b) {
        return 1f - similitudCoseno(a, b);
    }
}
