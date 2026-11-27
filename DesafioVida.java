package com.laura.verificacion;

import java.util.Random;

/**
 * Prueba de vida activa y aleatoria: parpadeo, giro a la izquierda o giro a la derecha.
 * Es Java puro (sin Android) para poder probarla con pruebas unitarias.
 * El giro se recibe ya normalizado: positivo = el usuario gira hacia SU izquierda
 * (ver PoseValidator.haciaIzquierda).
 */
public final class DesafioVida {

    public enum Tipo { PARPADEO, GIRO_IZQUIERDA, GIRO_DERECHA }

    public enum Estado { EN_CURSO, SUPERADO, EXPIRADO }

    public static final float OJO_ABIERTO = 0.60f;
    public static final float OJO_CERRADO = 0.35f;
    public static final float GIRO_FRENTE = 12f;
    public static final float GIRO_OBJETIVO = 22f;

    private final Tipo tipo;
    private final long limiteMs;
    private boolean vioAbierto;
    private boolean vioCerrado;
    private boolean vioFrente;
    private Estado estado = Estado.EN_CURSO;

    public DesafioVida(Tipo tipo, long duracionMs, long ahoraMs) {
        this.tipo = tipo;
        this.limiteMs = ahoraMs + duracionMs;
    }

    public static DesafioVida aleatorio(Random azar, long duracionMs, long ahoraMs) {
        Tipo[] tipos = Tipo.values();
        return new DesafioVida(tipos[azar.nextInt(tipos.length)], duracionMs, ahoraMs);
    }

    public Tipo getTipo() {
        return tipo;
    }

    public Estado getEstado() {
        return estado;
    }

    public String instruccion() {
        switch (tipo) {
            case PARPADEO:
                return "Parpadea una vez";
            case GIRO_IZQUIERDA:
                return "Gira la cabeza hacia TU izquierda";
            default:
                return "Gira la cabeza hacia TU derecha";
        }
    }

    /** Dibujo guía que se muestra arriba mientras dura el desafío. */
    public Pose poseGuia() {
        switch (tipo) {
            case GIRO_IZQUIERDA:
                return Pose.IZQUIERDA;
            case GIRO_DERECHA:
                return Pose.DERECHA;
            default:
                return Pose.FRENTE;
        }
    }

    public long segundosRestantes(long ahoraMs) {
        return Math.max(0L, (limiteMs - ahoraMs + 999L) / 1000L);
    }

    /** Marca el desafío como expirado si se acabó el tiempo (aunque no haya rostro válido). */
    public Estado revisarTiempo(long ahoraMs) {
        if (estado == Estado.EN_CURSO && ahoraMs > limiteMs) {
            estado = Estado.EXPIRADO;
        }
        return estado;
    }

    /** Se llama en cada fotograma con los datos de ML Kit. */
    public Estado actualizar(Float ojoIzq, Float ojoDer, float giroHaciaIzquierda, long ahoraMs) {
        if (estado != Estado.EN_CURSO) {
            return estado;
        }
        if (ahoraMs > limiteMs) {
            estado = Estado.EXPIRADO;
            return estado;
        }
        switch (tipo) {
            case PARPADEO:
                if (ojoIzq == null || ojoDer == null) {
                    break;
                }
                float media = (ojoIzq + ojoDer) / 2f;
                if (media >= OJO_ABIERTO) {
                    if (vioCerrado) {
                        estado = Estado.SUPERADO;
                    } else {
                        vioAbierto = true;
                    }
                } else if (media <= OJO_CERRADO && vioAbierto) {
                    vioCerrado = true;
                }
                break;
            case GIRO_IZQUIERDA:
                if (Math.abs(giroHaciaIzquierda) <= GIRO_FRENTE) {
                    vioFrente = true;
                } else if (vioFrente && giroHaciaIzquierda >= GIRO_OBJETIVO) {
                    estado = Estado.SUPERADO;
                }
                break;
            default:
                if (Math.abs(giroHaciaIzquierda) <= GIRO_FRENTE) {
                    vioFrente = true;
                } else if (vioFrente && giroHaciaIzquierda <= -GIRO_OBJETIVO) {
                    estado = Estado.SUPERADO;
                }
                break;
        }
        return estado;
    }
}
