package com.laura.verificacion;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Modo prueba: si está activo, cada verificación se guarda como un Intento con la etiqueta
 * (genuino, impostor, ataque) y la iluminación que elijas en la pantalla de resultados.
 * Solo se guardan números (distancia, tiempo, memoria), nunca fotos ni embeddings.
 */
public final class RegistroPruebas {

    private static final String PREF = "modo_prueba";
    private static final String ARCHIVO = "intentos_prueba.csv";

    private RegistroPruebas() {
    }

    private static SharedPreferences pref(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static boolean activo(Context c) {
        return pref(c).getBoolean("activo", false);
    }

    public static String etiqueta(Context c) {
        return pref(c).getString("etiqueta", Intento.GENUINO);
    }

    public static String iluminacion(Context c) {
        return pref(c).getString("luz", "MEDIA");
    }

    public static void configurar(Context c, boolean activo, String etiqueta, String luz) {
        pref(c).edit().putBoolean("activo", activo).putString("etiqueta", etiqueta)
                .putString("luz", luz).apply();
    }

    /** Memoria en uso (heap de Java + nativa, donde corre TFLite), en MB. */
    public static float memoriaUsadaMb() {
        Runtime rt = Runtime.getRuntime();
        double java = (rt.totalMemory() - rt.freeMemory()) / 1048576.0;
        double nativa = Debug.getNativeHeapAllocatedSize() / 1048576.0;
        return (float) (java + nativa);
    }

    /** Guarda el intento solo si el modo prueba está activo. */
    public static void registrar(Context c, float distancia, long tiempoMs, boolean vidaSuperada,
                                 float luminancia) {
        if (!activo(c)) {
            return;
        }
        Intento t = new Intento(etiqueta(c), iluminacion(c), distancia, tiempoMs, vidaSuperada,
                memoriaUsadaMb(), luminancia, System.currentTimeMillis());
        try (FileWriter fw = new FileWriter(new File(c.getFilesDir(), ARCHIVO), true)) {
            fw.write(t.aCsv() + "\n");
        } catch (IOException ignorado) {
            // si no se pudo guardar, la verificación igual continúa
        }
    }

    public static List<Intento> cargar(Context c) {
        List<Intento> lista = new ArrayList<>();
        File f = new File(c.getFilesDir(), ARCHIVO);
        if (!f.exists()) {
            return lista;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                Intento t = Intento.deCsv(linea);
                if (t != null) {
                    lista.add(t);
                }
            }
        } catch (IOException ignorado) {
            // devuelve lo que alcanzó a leer
        }
        return lista;
    }

    public static void borrar(Context c) {
        new File(c.getFilesDir(), ARCHIVO).delete();
    }
}
