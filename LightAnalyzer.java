package com.laura.verificacion;

/** Mide la luminosidad media de una zona de la imagen para exigir una iluminación adecuada. */
public final class LightAnalyzer {

    public static final float MINIMA = 70f;
    public static final float MAXIMA = 200f;

    private LightAnalyzer() {
    }

    /** Luminancia media (0 a 255) de una lista de píxeles ARGB. */
    public static float luminanciaMedia(int[] pixelesArgb) {
        if (pixelesArgb == null || pixelesArgb.length == 0) {
            return 0f;
        }
        double suma = 0;
        for (int p : pixelesArgb) {
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = p & 0xFF;
            suma += 0.299 * r + 0.587 * g + 0.114 * b;
        }
        return (float) (suma / pixelesArgb.length);
    }

    /** Devuelve null si la luz sirve, o el motivo si no. */
    public static String problema(float luminancia) {
        if (luminancia < MINIMA) {
            return "Hay muy poca luz en tu rostro";
        }
        if (luminancia > MAXIMA) {
            return "Hay demasiada luz o brillo en tu rostro";
        }
        return null;
    }
}
