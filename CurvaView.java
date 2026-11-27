package com.laura.verificacion;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import java.util.List;

/** Dibuja las curvas FAR y FRR contra el umbral, el umbral elegido y el punto EER. Sin librerías. */
public class CurvaView extends View {

    private List<Calibrador.Punto> puntos;
    private Calibrador.Punto eer;
    private float umbral = Umbral.POR_DEFECTO;

    private final Paint eje = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint far = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint frr = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linUmbral = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint punto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CurvaView(Context c) {
        super(c);
        eje.setColor(Color.GRAY);
        eje.setStrokeWidth(2f);
        far.setColor(Color.rgb(200, 40, 40));
        far.setStrokeWidth(5f);
        frr.setColor(Color.rgb(30, 90, 200));
        frr.setStrokeWidth(5f);
        linUmbral.setColor(Color.rgb(20, 140, 60));
        linUmbral.setStrokeWidth(4f);
        punto.setColor(Color.BLACK);
        texto.setColor(Color.DKGRAY);
        texto.setTextSize(30f);
    }

    public void setDatos(List<Calibrador.Punto> puntos, Calibrador.Punto eer, float umbral) {
        this.puntos = puntos;
        this.eer = eer;
        this.umbral = umbral;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float l = 80, r = getWidth() - 24, t = 24, b = getHeight() - 64;
        c.drawLine(l, b, r, b, eje);
        c.drawLine(l, t, l, b, eje);
        c.drawText("100%", 6, t + 26, texto);
        c.drawText("0", 40, b, texto);
        c.drawText("umbral (distancia coseno) 0 → 1", l, getHeight() - 16, texto);
        if (puntos == null || puntos.size() < 2) {
            c.drawText("Aún no hay datos de prueba", l + 20, (t + b) / 2, texto);
            return;
        }
        for (int i = 1; i < puntos.size(); i++) {
            Calibrador.Punto a = puntos.get(i - 1);
            Calibrador.Punto p = puntos.get(i);
            float x1 = l + a.umbral * (r - l);
            float x2 = l + p.umbral * (r - l);
            c.drawLine(x1, b - a.far * (b - t), x2, b - p.far * (b - t), far);
            c.drawLine(x1, b - a.frr * (b - t), x2, b - p.frr * (b - t), frr);
        }
        float tx = l + umbral * (r - l);
        c.drawLine(tx, t, tx, b, linUmbral);
        if (eer != null) {
            float ex = l + eer.umbral * (r - l);
            float ey = b - ((eer.far + eer.frr) / 2f) * (b - t);
            c.drawCircle(ex, ey, 11f, punto);
        }
        c.drawText("FAR", r - 210, t + 30, far);
        c.drawText("FRR", r - 120, t + 30, frr);
    }
}
