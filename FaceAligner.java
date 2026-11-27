package com.laura.verificacion;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;

/**
 * Recorta y alinea el rostro a 112x112 píxeles, el tamaño que espera el modelo de embeddings.
 * Usa los dos ojos para rotar, escalar y trasladar el rostro a una posición estándar.
 */
public final class FaceAligner {

    public static final int TAMANO = 112;

    // Posición estándar de los ojos (izquierdo, derecho) en una imagen de 112x112.
    private static final float[] DESTINO_OJOS = {38.2946f, 51.6963f, 73.5318f, 51.5014f};

    private FaceAligner() {
    }

    public static Bitmap alinear(Bitmap imagen, Face cara) {
        FaceLandmark ojoA = cara.getLandmark(FaceLandmark.LEFT_EYE);
        FaceLandmark ojoB = cara.getLandmark(FaceLandmark.RIGHT_EYE);
        if (ojoA == null || ojoB == null) {
            return recorteSimple(imagen, cara);
        }
        PointF a = ojoA.getPosition();
        PointF b = ojoB.getPosition();
        // Se ordenan por posición en la imagen para no depender de la convención izquierda/derecha.
        PointF izquierdo = a.x <= b.x ? a : b;
        PointF derecho = a.x <= b.x ? b : a;

        float[] origen = {izquierdo.x, izquierdo.y, derecho.x, derecho.y};
        Matrix matriz = new Matrix();
        matriz.setPolyToPoly(origen, 0, DESTINO_OJOS, 0, 2);

        Bitmap salida = Bitmap.createBitmap(TAMANO, TAMANO, Bitmap.Config.ARGB_8888);
        Canvas lienzo = new Canvas(salida);
        lienzo.drawBitmap(imagen, matriz, new Paint(Paint.FILTER_BITMAP_FLAG));
        return salida;
    }

    private static Bitmap recorteSimple(Bitmap imagen, Face cara) {
        Rect caja = cara.getBoundingBox();
        int izq = Math.max(0, caja.left);
        int arr = Math.max(0, caja.top);
        int ancho = Math.min(imagen.getWidth() - izq, caja.width());
        int alto = Math.min(imagen.getHeight() - arr, caja.height());
        if (ancho <= 0 || alto <= 0) {
            return Bitmap.createBitmap(TAMANO, TAMANO, Bitmap.Config.ARGB_8888);
        }
        Bitmap recorte = Bitmap.createBitmap(imagen, izq, arr, ancho, alto);
        return Bitmap.createScaledBitmap(recorte, TAMANO, TAMANO, true);
    }
}
