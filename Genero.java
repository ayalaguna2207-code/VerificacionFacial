package com.laura.verificacion;

/** Define qué fotos de referencia se muestran como guía de pose. */
public enum Genero {
    MUJER("Mujer"),
    HOMBRE("Hombre");

    public final String etiqueta;

    Genero(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public Genero otro() {
        return this == MUJER ? HOMBRE : MUJER;
    }

    /** Lee el valor guardado; si no existe (cuentas viejas) usa MUJER. */
    public static Genero deTexto(String texto) {
        for (Genero g : values()) {
            if (g.name().equals(texto)) {
                return g;
            }
        }
        return MUJER;
    }
}
