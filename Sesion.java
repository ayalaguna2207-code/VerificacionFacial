package com.laura.verificacion;

/** Usuario con la sesión iniciada. Vive solo en memoria: al cerrar la app, se pierde. */
public final class Sesion {

    private static Usuario actual;

    private Sesion() {
    }

    public static synchronized void iniciar(Usuario usuario) {
        actual = usuario;
    }

    public static synchronized void cerrar() {
        actual = null;
    }

    public static synchronized Usuario actual() {
        return actual;
    }
}
