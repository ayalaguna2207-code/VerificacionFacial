package com.laura.verificacion;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.ArrayList;
import java.util.List;

/** Dibuja encima de la cámara el recuadro del rostro, los ojos y una etiqueta. */
public class FaceOverlayView extends View {

    private static class Caja {
        RectF rect;
        float[] ojos; // x1, y1, x2, y2 (puede ser null)
    }

    private volatile List<Caja> cajas = new ArrayList<>();
    private volatile int anchoImagen = 1;
    private volatile int altoImagen = 1;
    private volatile boolean espejo = true;
    private volatile int color = Color.WHITE;
    private volatile String etiqueta;

    private final Paint pincelCaja = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pincelPunto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pincelTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pincelFondo = new Paint(Paint.ANTI_ALIAS_FLAG);

    public FaceOverlayView(Context contexto) {
        super(contexto);
        iniciar();
    }

    public FaceOverlayView(Context contexto, AttributeSet atributos) {
        super(contexto, atributos);
        iniciar();
    }

    private void iniciar() {
        pincelCaja.setStyle(Paint.Style.STROKE);
        pincelCaja.setStrokeWidth(6f);
        pincelPunto.setStyle(Paint.Style.FILL);
        pincelPunto.setColor(Color.CYAN);
        pincelTexto.setColor(Color.WHITE);
        pincelTexto.setTextSize(44f);
        pincelTexto.setFakeBoldText(true);
        pincelFondo.setStyle(Paint.Style.FILL);
    }

    /**
     * Recibe los rostros detectados sobre la imagen (en sus coordenadas) y los dibuja.
     * Se puede llamar desde el hilo de análisis.
     */
    public void actualizar(int anchoImagen, int altoImagen, boolean espejo,
                           List<Face> caras, int color, String etiqueta) {
        List<Caja> nuevas = new ArrayList<>();
        for (Face cara : caras) {
            Caja c = new Caja();
            Rect b = cara.getBoundingBox();
            c.rect = new RectF(b);
            FaceLandmark a = cara.getLandmark(FaceLandmark.LEFT_EYE);
            FaceLandmark d = cara.getLandmark(FaceLandmark.RIGHT_EYE);
            if (a != null && d != null) {
                c.ojos = new float[]{a.getPosition().x, a.getPosition().y,
                        d.getPosition().x, d.getPosition().y};
            }
            nuevas.add(c);
        }
        this.anchoImagen = Math.max(1, anchoImagen);
        this.altoImagen = Math.max(1, altoImagen);
        this.espejo = espejo;
        this.color = color;
        this.etiqueta = etiqueta;
        this.cajas = nuevas;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float vw = getWidth();
        float vh = getHeight();
        // PreviewView usa FILL_CENTER: escala para cubrir la vista y recorta el exceso al centro.
        float escala = Math.max(vw / anchoImagen, vh / altoImagen);
        float dx = (vw - anchoImagen * escala) / 2f;
        float dy = (vh - altoImagen * escala) / 2f;

        pincelCaja.setColor(color);
        pincelFondo.setColor(color);

        for (Caja c : cajas) {
            float x1 = mapearX(c.rect.left, escala, dx, vw);
            float x2 = mapearX(c.rect.right, escala, dx, vw);
            float izq = Math.min(x1, x2);
            float der = Math.max(x1, x2);
            float arr = c.rect.top * escala + dy;
            float aba = c.rect.bottom * escala + dy;
            canvas.drawRect(izq, arr, der, aba, pincelCaja);

            if (c.ojos != null) {
                canvas.drawCircle(mapearX(c.ojos[0], escala, dx, vw), c.ojos[1] * escala + dy, 9f, pincelPunto);
                canvas.drawCircle(mapearX(c.ojos[2], escala, dx, vw), c.ojos[3] * escala + dy, 9f, pincelPunto);
            }

            String texto = etiqueta;
            if (texto != null && !texto.isEmpty()) {
                float anchoTexto = pincelTexto.measureText(texto);
                float yBase = arr - 18f;
                if (yBase < 60f) {
                    yBase = aba + 56f;
                }
                canvas.drawRect(izq, yBase - 46f, izq + anchoTexto + 24f, yBase + 12f, pincelFondo);
                canvas.drawText(texto, izq + 12f, yBase, pincelTexto);
            }
        }
    }

    private float mapearX(float x, float escala, float dx, float anchoVista) {
        float px = x * escala + dx;
        return espejo ? anchoVista - px : px;
    }
}
