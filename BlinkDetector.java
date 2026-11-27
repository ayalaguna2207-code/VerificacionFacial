package com.laura.verificacion;

/**
 * Prueba de vida activa: detecta un parpadeo completo usando la probabilidad de "ojo abierto"
 * que entrega ML Kit. Secuencia: ojos abiertos -> ojos cerrados -> ojos abiertos de nuevo.
 */
public class BlinkDetector {

    private enum Fase {
        ESPERANDO_OJOS_ABIERTOS, ESPERANDO_CIERRE, ESPERANDO_REAPERTURA, COMPLETO
    }

    private static final float ABIERTO = 0.60f;
    private static final float CERRADO = 0.35f;

    private Fase fase = Fase.ESPERANDO_OJOS_ABIERTOS;

    public synchronized void reiniciar() {
        fase = Fase.ESPERANDO_OJOS_ABIERTOS;
    }

    public synchronized boolean completo() {
        return fase == Fase.COMPLETO;
    }

    /** Devuelve true cuando el parpadeo ya se completó. */
    public synchronized boolean actualizar(Float ojoIzquierdo, Float ojoDerecho) {
        if (fase == Fase.COMPLETO) {
            return true;
        }
        if (ojoIzquierdo == null || ojoDerecho == null) {
            return false;
        }
        float apertura = (ojoIzquierdo + ojoDerecho) / 2f;
        switch (fase) {
            case ESPERANDO_OJOS_ABIERTOS:
                if (apertura >= ABIERTO) {
                    fase = Fase.ESPERANDO_CIERRE;
                }
                break;
            case ESPERANDO_CIERRE:
                if (apertura <= CERRADO) {
                    fase = Fase.ESPERANDO_REAPERTURA;
                }
                break;
            case ESPERANDO_REAPERTURA:
                if (apertura >= ABIERTO) {
                    fase = Fase.COMPLETO;
                }
                break;
            default:
                break;
        }
        return fase == Fase.COMPLETO;
    }
}
