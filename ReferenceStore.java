package com.laura.verificacion;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Guarda la referencia del usuario SOLO en el almacenamiento privado de la app:
 * el vector numérico (embedding) y una miniatura del rostro recortado.
 */
public class ReferenceStore {

    private final File archivoEmbedding;
    private final File archivoMiniatura;

    public ReferenceStore(Context contexto) {
        File carpeta = contexto.getFilesDir();
        archivoEmbedding = new File(carpeta, "referencia_embedding.bin");
        archivoMiniatura = new File(carpeta, "referencia_miniatura.jpg");
    }

    public boolean existe() {
        return archivoEmbedding.exists();
    }

    public void guardar(float[] embedding, Bitmap rostro) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(archivoEmbedding))) {
            out.writeInt(embedding.length);
            for (float v : embedding) {
                out.writeFloat(v);
            }
        }
        try (FileOutputStream fo = new FileOutputStream(archivoMiniatura)) {
            rostro.compress(Bitmap.CompressFormat.JPEG, 90, fo);
        }
    }

    public float[] cargar() {
        if (!existe()) {
            return null;
        }
        try (DataInputStream in = new DataInputStream(new FileInputStream(archivoEmbedding))) {
            int n = in.readInt();
            float[] embedding = new float[n];
            for (int i = 0; i < n; i++) {
                embedding[i] = in.readFloat();
            }
            return embedding;
        } catch (IOException e) {
            return null;
        }
    }

    public Bitmap cargarMiniatura() {
        if (!archivoMiniatura.exists()) {
            return null;
        }
        return BitmapFactory.decodeFile(archivoMiniatura.getPath());
    }

    public void borrar() {
        archivoEmbedding.delete();
        archivoMiniatura.delete();
    }
}
