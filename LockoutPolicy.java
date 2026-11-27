package com.laura.verificacion;

/** Política de bloqueo: tras {@link #MAX_FALLOS} intentos fallidos, se bloquea el acceso un tiempo. */
public class LockoutPolicy {

    public static final int MAX_FALLOS = 3;
    public static final long BLOQUEO_MS = 60_000L;

    private int fallos;
    private long bloqueadoHasta;

    public LockoutPolicy() {
        this(0, 0L);
    }

    public LockoutPolicy(int fallos, long bloqueadoHasta) {
        this.fallos = fallos;
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public boolean estaBloqueado(long ahoraMs) {
        return ahoraMs < bloqueadoHasta;
    }

    public long segundosRestantes(long ahoraMs) {
        long restante = bloqueadoHasta - ahoraMs;
        return restante <= 0 ? 0 : (restante + 999) / 1000;
    }

    public int intentosRestantes() {
        return Math.max(0, MAX_FALLOS - fallos);
    }

    public void registrarFallo(long ahoraMs) {
        fallos++;
        if (fallos >= MAX_FALLOS) {
            bloqueadoHasta = ahoraMs + BLOQUEO_MS;
            fallos = 0;
        }
    }

    public void registrarExito() {
        fallos = 0;
        bloqueadoHasta = 0L;
    }

    public int getFallos() {
        return fallos;
    }

    public long getBloqueadoHasta() {
        return bloqueadoHasta;
    }
}
