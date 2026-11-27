package com.laura.verificacion;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Foto de referencia que muestra qué pose hacer (frente, izquierda o derecha), según el
 * género elegido al registrarse. La vista previa de la cámara frontal funciona como espejo:
 * "tu izquierda" aparece hacia la izquierda de la pantalla, igual que en las fotos.
 */
public class PoseGuideView extends View {

    private Pose pose = Pose.FRENTE;
    private Genero genero = Genero.MUJER;
    private Bitmap foto;

    private final Paint borde = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flecha = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fotoPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path recorte = new Path();
    private final Path triangulo = new Path();
    private final RectF rect = new RectF();

    public PoseGuideView(Context contexto) {
        super(contexto);
        iniciar();
    }

    public PoseGuideView(Context contexto, AttributeSet atributos) {
        super(contexto, atributos);
        iniciar();
    }

    private void iniciar() {
        borde.setStyle(Paint.Style.STROKE);
        borde.setStrokeWidth(5f);
        borde.setColor(Color.rgb(77, 208, 225));
        flecha.setStyle(Paint.Style.FILL);
        flecha.setColor(Color.rgb(255, 213, 79));
        cargarFoto();
    }

    public void setPose(Pose nueva) {
        this.pose = nueva;
        cargarFoto();
        invalidate();
    }

    public void setGenero(Genero nuevo) {
        this.genero = nuevo;
        cargarFoto();
        invalidate();
    }

    private void cargarFoto() {
        String nombre = "guia_" + genero.name().toLowerCase(java.util.Locale.ROOT) + "_"
                + pose.name().toLowerCase(java.util.Locale.ROOT);
        int id = getResources().getIdentifier(nombre, "drawable", getContext().getPackageName());
        foto = id == 0 ? null : BitmapFactory.decodeResource(getResources(), id);
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth();
        float h = getHeight();
        rect.set(4f, 4f, w - 4f, h - 4f);
        float radio = 18f;

        if (foto != null) {
            // Recorte centrado (center-crop) dentro de un rectángulo redondeado.
            float escala = Math.max(rect.width() / foto.getWidth(), rect.height() / foto.getHeight());
            float ancho = rect.width() / escala;
            float alto = rect.height() / escala;
            int izq = Math.round((foto.getWidth() - ancho) / 2f);
            int arr = Math.round((foto.getHeight() - alto) / 2f);
            Rect origen = new Rect(izq, arr, Math.round(izq + ancho), Math.round(arr + alto));
            recorte.reset();
            recorte.addRoundRect(rect, radio, radio, Path.Direction.CW);
            c.save();
            c.clipPath(recorte);
            c.drawBitmap(foto, origen, rect, fotoPaint);
            c.restore();
        }
        c.drawRoundRect(rect, radio, radio, borde);

        // Flecha amarilla que indica hacia qué lado girar (en la pantalla).
        if (pose != Pose.FRENTE) {
            float cy = h - 22f;
            float lado = pose == Pose.IZQUIERDA ? 1f : -1f; // izquierda de pantalla = +1
            float xPunta = pose == Pose.IZQUIERDA ? 14f : w - 14f;
            triangulo.reset();
            triangulo.moveTo(xPunta, cy);
            triangulo.lineTo(xPunta + lado * 26f, cy - 15f);
            triangulo.lineTo(xPunta + lado * 26f, cy + 15f);
            triangulo.close();
            c.drawPath(triangulo, flecha);
        }
    }
}
