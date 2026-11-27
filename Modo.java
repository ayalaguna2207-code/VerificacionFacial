package com.laura.verificacion;

/** Las cuatro etapas de la aplicación. Cada etapa reutiliza la misma pantalla de cámara. */
public enum Modo {
    DETECCION("Etapa 1 · Detección de rostro"),
    REGISTRO("Etapa 2 · Registro de referencia"),
    VERIFICACION("Etapa 3 · Verificación 1:1"),
    LIVENESS("Etapa 4 · Verificación con prueba de vida");

    public final String titulo;

    Modo(String titulo) {
        this.titulo = titulo;
    }
}
