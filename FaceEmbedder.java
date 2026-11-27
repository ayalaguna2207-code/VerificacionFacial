package com.laura.verificacion;

import android.content.Context;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Ejecuta el modelo MobileFaceNet (TensorFlow Lite) dentro del teléfono.
 * Entrada: rostro alineado 112x112 RGB. Salida: vector (embedding) de 192 valores.
 */
public class FaceEmbedder implements AutoCloseable {

    private static final String ARCHIVO_MODELO = "mobile_face_net.tflite";
    private static final int T = FaceAligner.TAMANO;
    public static final int DIMENSION = 192;

    private final Interpreter interprete;
    private final ByteBuffer entrada =
            ByteBuffer.allocateDirect(T * T * 3 * 4).order(ByteOrder.nativeOrder());

    public FaceEmbedder(Context contexto) throws IOException {
        ByteBuffer modelo = cargarModelo(contexto);
        Interpreter.Options opciones = new Interpreter.Options();
        opciones.setNumThreads(4);
        interprete = new Interpreter(modelo, opciones);
    }

    private static ByteBuffer cargarModelo(Context contexto) throws IOException {
        try (InputStream is = contexto.getAssets().open(ARCHIVO_MODELO)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = is.read(buffer)) > 0) {
                bos.write(buffer, 0, n);
            }
            byte[] bytes = bos.toByteArray();
            ByteBuffer modelo = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());
            modelo.put(bytes);
            modelo.rewind();
            return modelo;
        }
    }

    /** Devuelve el embedding normalizado (norma 1) del rostro ya alineado a 112x112. */
    public synchronized float[] generar(Bitmap rostro112) {
        int[] pixeles = new int[T * T];
        rostro112.getPixels(pixeles, 0, T, 0, 0, T, T);

        entrada.rewind();
        for (int p : pixeles) {
            entrada.putFloat((((p >> 16) & 0xFF) - 127.5f) / 128f); // R
            entrada.putFloat((((p >> 8) & 0xFF) - 127.5f) / 128f);  // G
            entrada.putFloat(((p & 0xFF) - 127.5f) / 128f);         // B
        }
        entrada.rewind();

        float[][] salida = new float[1][DIMENSION];
        interprete.run(entrada, salida);
        return normalizar(salida[0]);
    }

    private static float[] normalizar(float[] v) {
        double suma = 0;
        for (float x : v) {
            suma += x * x;
        }
        float norma = (float) Math.sqrt(suma);
        if (norma < 1e-9f) {
            return v;
        }
        float[] r = new float[v.length];
        for (int i = 0; i < v.length; i++) {
            r[i] = v[i] / norma;
        }
        return r;
    }

    @Override
    public void close() {
        interprete.close();
    }
}
