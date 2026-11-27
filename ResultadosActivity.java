package com.laura.verificacion;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Pantalla de pruebas y resultados: modo prueba, curva FAR/FRR contra el umbral,
 * exactitud, tiempos, tamaño del modelo, memoria, desempeño por iluminación y ataques.
 */
public class ResultadosActivity extends AppCompatActivity {

    private static final String MODELO = "mobile_face_net.tflite";

    private TextView tvReporte;
    private TextView tvUmbral;
    private CurvaView curva;
    private SeekBar barra;
    private Switch swModo;
    private Spinner spEtiqueta;
    private Spinner spLuz;
    private float umbral;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        umbral = Umbral.obtener(this);

        ScrollView scroll = new ScrollView(this);
        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setPadding(32, 32, 32, 32);
        scroll.addView(raiz);
        setContentView(scroll);
        Bordes.aplicar(this);

        TextView titulo = new TextView(this);
        titulo.setText("Pruebas y resultados");
        titulo.setTextSize(22f);
        titulo.setTypeface(null, Typeface.BOLD);
        raiz.addView(titulo);

        swModo = new Switch(this);
        swModo.setText("Modo prueba (guardar cada verificación)");
        swModo.setChecked(RegistroPruebas.activo(this));
        raiz.addView(swModo);

        spEtiqueta = new Spinner(this);
        spEtiqueta.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                Intento.ETIQUETAS));
        spEtiqueta.setSelection(Arrays.asList(Intento.ETIQUETAS).indexOf(RegistroPruebas.etiqueta(this)));
        raiz.addView(spEtiqueta);

        spLuz = new Spinner(this);
        spLuz.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                Intento.LUCES));
        spLuz.setSelection(Arrays.asList(Intento.LUCES).indexOf(RegistroPruebas.iluminacion(this)));
        raiz.addView(spLuz);

        Button aplicar = new Button(this);
        aplicar.setText("Aplicar configuración de prueba");
        aplicar.setOnClickListener(v -> {
            RegistroPruebas.configurar(this, swModo.isChecked(),
                    (String) spEtiqueta.getSelectedItem(), (String) spLuz.getSelectedItem());
            Toast.makeText(this, "Cada verificación se guardará como "
                    + spEtiqueta.getSelectedItem() + " / luz " + spLuz.getSelectedItem(),
                    Toast.LENGTH_LONG).show();
        });
        raiz.addView(aplicar);

        tvUmbral = new TextView(this);
        tvUmbral.setPadding(0, 24, 0, 0);
        raiz.addView(tvUmbral);

        barra = new SeekBar(this);
        barra.setMax(100);
        barra.setProgress(Math.round(umbral * 100));
        barra.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean usuario) {
                umbral = p / 100f;
                refrescar();
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });
        raiz.addView(barra);

        curva = new CurvaView(this);
        curva.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 650));
        raiz.addView(curva);

        tvReporte = new TextView(this);
        tvReporte.setTextSize(14f);
        raiz.addView(tvReporte);

        Button usarEer = new Button(this);
        usarEer.setText("Mover la barra al umbral EER (FAR = FRR)");
        usarEer.setOnClickListener(v -> {
            List<Calibrador.Punto> c = Calibrador.curva(distancias(Intento.GENUINO),
                    distancias(Intento.IMPOSTOR), 100);
            barra.setProgress(Math.round(Calibrador.eer(c).umbral * 100));
        });
        raiz.addView(usarEer);

        Button guardar = new Button(this);
        guardar.setText("Guardar este umbral en la app");
        guardar.setOnClickListener(v -> {
            Umbral.guardar(this, umbral);
            Toast.makeText(this, String.format(Locale.US, "Umbral %.2f guardado: la verificación ya lo usa",
                    umbral), Toast.LENGTH_LONG).show();
        });
        raiz.addView(guardar);

        Button exportar = new Button(this);
        exportar.setText("Exportar datos (CSV)");
        exportar.setOnClickListener(v -> {
            StringBuilder sb = new StringBuilder(
                    "etiqueta,iluminacion,distancia,tiempoMs,vida,memoriaMb,luminancia,marca\n");
            for (Intento t : RegistroPruebas.cargar(this)) {
                sb.append(t.aCsv()).append('\n');
            }
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, sb.toString());
            startActivity(Intent.createChooser(i, "Enviar datos de prueba"));
        });
        raiz.addView(exportar);

        Button borrar = new Button(this);
        borrar.setText("Borrar datos de prueba");
        borrar.setOnClickListener(v -> {
            RegistroPruebas.borrar(this);
            refrescar();
        });
        raiz.addView(borrar);

        refrescar();
    }

    /** Distancias válidas (ya comparadas) de una etiqueta. */
    private List<Float> distancias(String etiqueta) {
        List<Float> l = new ArrayList<>();
        for (Intento t : RegistroPruebas.cargar(this)) {
            if (t.etiqueta.equals(etiqueta) && t.fueComparado()) {
                l.add(t.distancia);
            }
        }
        return l;
    }

    private List<Float> distancias(String etiqueta, String luz) {
        List<Float> l = new ArrayList<>();
        for (Intento t : RegistroPruebas.cargar(this)) {
            if (t.etiqueta.equals(etiqueta) && t.iluminacion.equals(luz) && t.fueComparado()) {
                l.add(t.distancia);
            }
        }
        return l;
    }

    private double tamanoModeloMb() {
        try (InputStream is = getAssets().open(MODELO)) {
            long n = 0;
            byte[] buf = new byte[65536];
            int r;
            while ((r = is.read(buf)) > 0) {
                n += r;
            }
            return n / 1048576.0;
        } catch (Exception e) {
            return -1;
        }
    }

    private void refrescar() {
        List<Intento> todos = RegistroPruebas.cargar(this);
        List<Float> g = distancias(Intento.GENUINO);
        List<Float> imp = distancias(Intento.IMPOSTOR);
        tvUmbral.setText(String.format(Locale.US, "Umbral elegido: %.2f  (en uso por la app: %.2f)",
                umbral, Umbral.obtener(this)));

        List<Calibrador.Punto> c = Calibrador.curva(g, imp, 100);
        Calibrador.Punto eer = (g.isEmpty() || imp.isEmpty()) ? null : Calibrador.eer(c);
        curva.setDatos(c, eer, umbral);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US, "Intentos guardados: %d (genuinos %d, impostores %d)\n\n",
                todos.size(), g.size(), imp.size()));
        sb.append(String.format(Locale.US, "GENERAL (umbral %.2f)\n  Exactitud: %.1f%%\n  FAR: %.1f%%\n  FRR: %.1f%%\n",
                umbral, 100 * Calibrador.exactitud(g, imp, umbral),
                100 * Calibrador.far(imp, umbral), 100 * Calibrador.frr(g, umbral)));
        if (eer != null) {
            sb.append(String.format(Locale.US, "  EER: %.1f%% en umbral %.2f\n",
                    100 * (eer.far + eer.frr) / 2f, eer.umbral));
        }

        int vidaFallidaGenuinos = 0;
        int genuinosTotal = 0;
        for (Intento t : todos) {
            if (t.etiqueta.equals(Intento.GENUINO)) {
                genuinosTotal++;
                if (!t.vidaSuperada) {
                    vidaFallidaGenuinos++;
                }
            }
        }
        if (genuinosTotal > 0) {
            sb.append(String.format(Locale.US, "  Genuinos que fallaron la prueba de vida: %d/%d\n",
                    vidaFallidaGenuinos, genuinosTotal));
        }

        sb.append("\nPOR ILUMINACIÓN\n");
        for (String luz : Intento.LUCES) {
            List<Float> gl = distancias(Intento.GENUINO, luz);
            List<Float> il = distancias(Intento.IMPOSTOR, luz);
            if (gl.isEmpty() && il.isEmpty()) {
                continue;
            }
            double suma = 0;
            int n = 0;
            for (Intento t : todos) {
                if (t.iluminacion.equals(luz) && !t.esAtaque()) {
                    suma += t.luminancia;
                    n++;
                }
            }
            sb.append(String.format(Locale.US,
                    "  %s: exactitud %.1f%%, FAR %.1f%%, FRR %.1f%% (n=%d, luminancia media %.0f/255)\n",
                    luz, 100 * Calibrador.exactitud(gl, il, umbral), 100 * Calibrador.far(il, umbral),
                    100 * Calibrador.frr(gl, umbral), gl.size() + il.size(), n == 0 ? 0 : suma / n));
        }

        sb.append("\nATAQUES (¿cuántos lograron pasar liveness y umbral?)\n");
        for (String ataque : new String[]{Intento.ATAQUE_FOTO_IMPRESA, Intento.ATAQUE_FOTO_PANTALLA,
                Intento.ATAQUE_VIDEO}) {
            int n = 0;
            int vidaOk = 0;
            int pasan = 0;
            for (Intento t : todos) {
                if (!t.etiqueta.equals(ataque)) {
                    continue;
                }
                n++;
                if (t.vidaSuperada) {
                    vidaOk++;
                }
                if (t.vidaSuperada && Verificador.acepta(t.distancia, umbral)) {
                    pasan++;
                }
            }
            if (n > 0) {
                sb.append(String.format(Locale.US,
                        "  %s: pasaron %d/%d (%.0f%%); superaron la vida %d/%d\n",
                        ataque, pasan, n, 100f * pasan / n, vidaOk, n));
            }
        }

        double suma = 0;
        long max = 0;
        int cont = 0;
        double memMax = 0;
        for (Intento t : todos) {
            if (t.fueComparado()) {
                suma += t.tiempoMs;
                max = Math.max(max, t.tiempoMs);
                cont++;
            }
            memMax = Math.max(memMax, t.memoriaMb);
        }
        sb.append("\nRENDIMIENTO\n");
        if (cont > 0) {
            sb.append(String.format(Locale.US, "  Tiempo medio de procesamiento: %.0f ms (máx. %d ms, n=%d)\n",
                    suma / cont, max, cont));
        }
        sb.append(String.format(Locale.US, "  Tamaño del modelo: %.2f MB\n", tamanoModeloMb()));
        sb.append(String.format(Locale.US, "  Memoria ahora: %.1f MB", RegistroPruebas.memoriaUsadaMb()));
        if (memMax > 0) {
            sb.append(String.format(Locale.US, " (pico durante verificaciones: %.1f MB)", memMax));
        }
        sb.append('\n');
        tvReporte.setText(sb.toString());
    }
}
