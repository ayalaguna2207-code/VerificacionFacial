package com.laura.verificacion;

import android.content.Context;

/** Umbral de distancia coseno que usa la app para aceptar o rechazar. Se calibra en Resultados. */
public final class Umbral {

    public static final float POR_DEFECTO = 0.45f;
    private static final String PREF = "config_verificacion";

    private Umbral() {
    }

    public static float obtener(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getFloat("umbral", POR_DEFECTO);
    }

    public static void guardar(Context c, float valor) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putFloat("umbral", valor).apply();
    }
}
