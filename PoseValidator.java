package com.laura.verificacion;

/**
 * Decide si el ángulo de la cabeza corresponde a la pose pedida.
 * Usa el ángulo Y (giro a los lados) y X (cabeceo) que entrega ML Kit, en grados.
 */
public final class PoseValidator {

    /**
     * Convención de signo. Con la cámara frontal, ML Kit da un ángulo Y positivo cuando
     * el usuario gira hacia SU izquierda. Si en el celular sale al revés (pide izquierda
     * y solo acepta cuando giras a la derecha), cambia este valor a -1f y nada más.
     */
    public static final float SIGNO_IZQUIERDA = 1f;

    public static final float FRENTE_MAX = 10f;
    public static final float GIRO_MIN = 18f;
    public static final float GIRO_MAX = 40f;
    public static final float CABECEO_MAX = 20f;

    private PoseValidator() {
    }

    /** Ángulo normalizado: positivo = el usuario gira hacia su izquierda. */
    public static float haciaIzquierda(float anguloY) {
        return anguloY * SIGNO_IZQUIERDA;
    }

    public static boolean cumple(Pose pose, float anguloY, float cabeceoX) {
        if (Math.abs(cabeceoX) > CABECEO_MAX) {
            return false;
        }
        float g = haciaIzquierda(anguloY);
        switch (pose) {
            case FRENTE:
                return Math.abs(g) <= FRENTE_MAX;
            case IZQUIERDA:
                return g >= GIRO_MIN && g <= GIRO_MAX;
            case DERECHA:
                return g <= -GIRO_MIN && g >= -GIRO_MAX;
            default:
                return false;
        }
    }

    /** Mensaje para el usuario sobre qué corregir. Si la pose ya es correcta, lo dice. */
    public static String sugerencia(Pose pose, float anguloY, float cabeceoX) {
        if (Math.abs(cabeceoX) > CABECEO_MAX) {
            return "Mantén la cabeza derecha, sin subir ni bajar la barbilla";
        }
        if (cumple(pose, anguloY, cabeceoX)) {
            return "Pose correcta, no te muevas";
        }
        float g = haciaIzquierda(anguloY);
        switch (pose) {
            case FRENTE:
                return g > 0 ? "Gira un poco a tu derecha para quedar de frente"
                        : "Gira un poco a tu izquierda para quedar de frente";
            case IZQUIERDA:
                return g < GIRO_MIN ? "Gira más hacia tu izquierda"
                        : "Giraste demasiado, regresa un poco";
            case DERECHA:
                return g > -GIRO_MIN ? "Gira más hacia tu derecha"
                        : "Giraste demasiado, regresa un poco";
            default:
                return "";
        }
    }
}
