package com.laura.verificacion;

import java.util.Locale;

/** Un intento de verificación guardado durante las pruebas (sin fotos ni embeddings). */
public class Intento {

    public static final String GENUINO = "GENUINO";
    public static final String IMPOSTOR = "IMPOSTOR";
    public static final String ATAQUE_FOTO_IMPRESA = "ATAQUE_FOTO_IMPRESA";
    public static final String ATAQUE_FOTO_PANTALLA = "ATAQUE_FOTO_PANTALLA";
    public static final String ATAQUE_VIDEO = "ATAQUE_VIDEO";
    public static final String[] ETIQUETAS = {GENUINO, IMPOSTOR, ATAQUE_FOTO_IMPRESA,
            ATAQUE_FOTO_PANTALLA, ATAQUE_VIDEO};
    public static final String[] LUCES = {"ALTA", "MEDIA", "BAJA"};

    /** Distancia que se guarda cuando no se llegó a comparar (p. ej. falló la prueba de vida). */
    public static final float NO_COMPARADO = -1f;

    public final String etiqueta;
    public final String iluminacion;
    public final float distancia;
    public final long tiempoMs;
    public final boolean vidaSuperada;
    public final float memoriaMb;
    public final float luminancia;
    public final long marca;

    public Intento(String etiqueta, String iluminacion, float distancia, long tiempoMs,
                   boolean vidaSuperada, float memoriaMb, float luminancia, long marca) {
        this.etiqueta = etiqueta;
        this.iluminacion = iluminacion;
        this.distancia = distancia;
        this.tiempoMs = tiempoMs;
        this.vidaSuperada = vidaSuperada;
        this.memoriaMb = memoriaMb;
        this.luminancia = luminancia;
        this.marca = marca;
    }

    public boolean esAtaque() {
        return etiqueta.startsWith("ATAQUE");
    }

    public boolean fueComparado() {
        return distancia >= 0f;
    }

    public String aCsv() {
        return String.format(Locale.US, "%s,%s,%.4f,%d,%b,%.1f,%.1f,%d",
                etiqueta, iluminacion, distancia, tiempoMs, vidaSuperada, memoriaMb, luminancia, marca);
    }

    public static Intento deCsv(String linea) {
        try {
            String[] p = linea.trim().split(",");
            if (p.length < 8) {
                return null;
            }
            return new Intento(p[0], p[1], Float.parseFloat(p[2]), Long.parseLong(p[3]),
                    Boolean.parseBoolean(p[4]), Float.parseFloat(p[5]), Float.parseFloat(p[6]),
                    Long.parseLong(p[7]));
        } catch (Exception e) {
            return null;
        }
    }
}
