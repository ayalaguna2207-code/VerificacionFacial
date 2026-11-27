package com.laura.verificacion;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.face.Face;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pantalla única de cámara. Según el {@link Modo} recibido ejecuta la etapa 1, 2, 3 o 4.
 * El análisis de cada fotograma se hace en un hilo aparte; la interfaz se actualiza en el hilo principal.
 */
public class CameraActivity extends AppCompatActivity {

    public static final String EXTRA_MODO = "modo";

    private static final float DISTANCIA_MINIMA = 0.20f;
    private static final float PASO_DISTANCIA = 0.006f;
    private static final long TIEMPO_PARPADEO_MS = 8000;
    private static final long TIEMPO_EXTRA_COMPARAR_MS = 4000;

    private static final int VERDE = Color.rgb(46, 204, 113);
    private static final int ROJO = Color.rgb(231, 76, 60);
    private static final int AMARILLO = Color.rgb(241, 196, 15);
    private static final int CIAN = Color.rgb(0, 200, 255);

    private enum FaseVida {PIDIENDO_PARPADEO, DECIDIDO}

    private Modo modo;
    private PreviewView vistaPrevia;
    private FaceOverlayView superposicion;
    private TextView tvInfo;
    private TextView tvUmbral;
    private SeekBar seekUmbral;
    private Button btnAccion;

    private final ExecutorService hiloAnalisis = Executors.newSingleThreadExecutor();
    private FaceDetectorHelper detector;
    private FaceEmbedder embedder;
    private ReferenceStore almacen;
    private float[] referencia;
    private final BlinkDetector parpadeo = new BlinkDetector();

    private volatile float umbral = 0.45f;
    private volatile boolean capturarAhora = false;
    private volatile boolean reiniciarPedido = true;

    // Estado de la etapa 4 (solo se toca desde el hilo de análisis)
    private FaseVida faseVida = FaseVida.PIDIENDO_PARPADEO;
    private long inicioFase;
    private int colorResultado = Color.WHITE;
    private String etiquetaResultado;

    private final ActivityResultLauncher<String> pedirPermiso = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), concedido -> {
                if (concedido) {
                    iniciarCamara();
                } else {
                    Toast.makeText(this, "Se necesita el permiso de cámara", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        setContentView(R.layout.activity_camera);

        String nombre = getIntent().getStringExtra(EXTRA_MODO);
        modo = nombre == null ? Modo.DETECCION : Modo.valueOf(nombre);

        vistaPrevia = findViewById(R.id.previewView);
        superposicion = findViewById(R.id.overlay);
        tvInfo = findViewById(R.id.tvInfo);
        tvUmbral = findViewById(R.id.tvUmbral);
        seekUmbral = findViewById(R.id.seekUmbral);
        btnAccion = findViewById(R.id.btnAccion);
        ((TextView) findViewById(R.id.tvTitulo)).setText(modo.titulo);

        almacen = new ReferenceStore(this);
        referencia = almacen.cargar();

        try {
            detector = new FaceDetectorHelper();
            embedder = new FaceEmbedder(this);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo cargar el modelo: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        configurarControles();

        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara();
        } else {
            pedirPermiso.launch(Manifest.permission.CAMERA);
        }
    }

    private void configurarControles() {
        boolean usaUmbral = modo == Modo.VERIFICACION || modo == Modo.LIVENESS;
        int visibilidad = usaUmbral ? View.VISIBLE : View.GONE;
        seekUmbral.setVisibility(visibilidad);
        tvUmbral.setVisibility(visibilidad);
        seekUmbral.setProgress(Math.round((umbral - DISTANCIA_MINIMA) / PASO_DISTANCIA));
        actualizarTextoUmbral();
        seekUmbral.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar barra, int progreso, boolean delUsuario) {
                umbral = DISTANCIA_MINIMA + progreso * PASO_DISTANCIA;
                actualizarTextoUmbral();
            }

            @Override
            public void onStartTrackingTouch(SeekBar barra) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar barra) {
            }
        });

        switch (modo) {
            case REGISTRO:
                btnAccion.setText("Registrar mi rostro");
                btnAccion.setOnClickListener(v -> pedirConsentimiento());
                break;
            case LIVENESS:
                btnAccion.setText("Reintentar");
                btnAccion.setOnClickListener(v -> reiniciarPedido = true);
                break;
            default:
                btnAccion.setVisibility(View.GONE);
                break;
        }
    }

    private void actualizarTextoUmbral() {
        tvUmbral.setText(String.format(Locale.US,
                "Umbral de distancia coseno: %.2f  (más bajo = más estricto)", umbral));
    }

    private void pedirConsentimiento() {
        new AlertDialog.Builder(this)
                .setTitle("Consentimiento")
                .setMessage("Se guardará solo en este teléfono, en el almacenamiento privado de la app, "
                        + "un vector numérico de tu rostro y una miniatura. No se envía nada a internet. "
                        + "Puedes borrarlo desde el menú principal.")
                .setPositiveButton("Acepto", (d, w) -> capturarAhora = true)
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void iniciarCamara() {
        ListenableFuture<ProcessCameraProvider> futuro = ProcessCameraProvider.getInstance(this);
        futuro.addListener(() -> {
            try {
                enlazarCamara(futuro.get());
            } catch (Exception e) {
                Toast.makeText(this, "No se pudo abrir la cámara: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void enlazarCamara(ProcessCameraProvider proveedor) {
        Preview vista = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build();
        vista.setSurfaceProvider(vistaPrevia.getSurfaceProvider());

        ImageAnalysis analisis = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();
        analisis.setAnalyzer(hiloAnalisis, this::analizar);

        proveedor.unbindAll();
        proveedor.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, vista, analisis);
    }

    // ------------------------------------------------------------------ análisis de cada fotograma

    private void analizar(ImageProxy imagen) {
        try {
            Bitmap bmp = imagen.toBitmap();
            int rotacion = imagen.getImageInfo().getRotationDegrees();
            if (rotacion != 0) {
                Matrix m = new Matrix();
                m.postRotate(rotacion);
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            }
            procesar(bmp);
        } catch (Exception e) {
            mostrar("Error: " + e.getMessage());
        } finally {
            imagen.close();
        }
    }

    private void procesar(Bitmap bmp) throws Exception {
        long t0 = SystemClock.elapsedRealtime();
        List<Face> caras = detector.detectar(bmp);
        Face cara = mayorCara(caras);

        switch (modo) {
            case DETECCION:
                etapaDeteccion(bmp, caras, cara, t0);
                break;
            case REGISTRO:
                etapaRegistro(bmp, caras, cara);
                break;
            case VERIFICACION:
                etapaVerificacion(bmp, caras, cara, t0);
                break;
            case LIVENESS:
                etapaLiveness(bmp, caras, cara);
                break;
        }
    }

    // Etapa 1
    private void etapaDeteccion(Bitmap bmp, List<Face> caras, Face cara, long t0) {
        superposicion.actualizar(bmp.getWidth(), bmp.getHeight(), true, caras, CIAN, null);
        if (cara == null) {
            mostrar("No se detecta ningún rostro");
            return;
        }
        long ms = SystemClock.elapsedRealtime() - t0;
        mostrar(String.format(Locale.US,
                "Rostros detectados: %d\nOjo izquierdo abierto: %s   derecho: %s\nSonrisa: %s\n"
                        + "Giro de cabeza: %.0f°\nTiempo de detección: %d ms",
                caras.size(),
                porcentaje(cara.getLeftEyeOpenProbability()),
                porcentaje(cara.getRightEyeOpenProbability()),
                porcentaje(cara.getSmilingProbability()),
                cara.getHeadEulerAngleY(), ms));
    }

    // Etapa 2
    private void etapaRegistro(Bitmap bmp, List<Face> caras, Face cara) throws Exception {
        String problema = problemaCalidad(bmp, caras, cara);
        superposicion.actualizar(bmp.getWidth(), bmp.getHeight(), true, caras,
                problema == null ? VERDE : AMARILLO, null);

        if (problema != null) {
            mostrar(problema);
            if (capturarAhora) {
                capturarAhora = false;
                mostrar("No se pudo registrar: " + problema + "\nVuelve a pulsar el botón.");
            }
            return;
        }
        mostrar("Rostro listo.\nPulsa \"Registrar mi rostro\".");

        if (capturarAhora) {
            capturarAhora = false;
            Bitmap rostro = FaceAligner.alinear(bmp, cara);
            float[] embedding = embedder.generar(rostro);
            almacen.guardar(embedding, rostro);
            runOnUiThread(() -> {
                Toast.makeText(this, "Rostro de referencia guardado", Toast.LENGTH_LONG).show();
                finish();
            });
        }
    }

    // Etapa 3
    private void etapaVerificacion(Bitmap bmp, List<Face> caras, Face cara, long t0) {
        if (referencia == null) {
            superposicion.actualizar(bmp.getWidth(), bmp.getHeight(), true, caras, Color.WHITE, null);
            mostrar("Aún no hay rostro de referencia.\nVe a la Etapa 2 y regístralo.");
            return;
        }
        String problema = problemaCalidad(bmp, caras, cara);
        if (problema != null) {
            superposicion.actualizar(bmp.getWidth(), bmp.getHeight(), true, caras, Color.WHITE, null);
            mostrar(problema);
            return;
        }
        float[] embedding = embedder.generar(FaceAligner.alinear(bmp, cara));
        float distancia = FaceComparator.distanciaCoseno(referencia, embedding);
        boolean mismaPersona = distancia <= umbral;
        superposicion.actualizar(bmp.getWidth(), bmp.getHeight(), true, caras,
                mismaPersona ? VERDE : ROJO, mismaPersona ? "MISMA PERSONA" : "DIFERENTE");
        long ms = SystemClock.elapsedRealtime() - t0;
        mostrar(String.format(Locale.US,
                "Distancia coseno: %.3f   (umbral %.2f)\nSimilitud coseno: %.0f%%\nDecisión: %s\nTiempo total: %d ms",
                distancia, umbral, (1f - distancia) * 100f,
                mismaPersona ? "MISMA PERSONA" : "PERSONA DIFERENTE", ms));
    }

    // Etapa 4
    private void etapaLiveness(Bitmap bmp, List<Face> caras, Face cara) throws Exception {
        long ahora = SystemClock.elapsedRealtime();
        if (referencia == null) {
            superposicion.actualizar(bmp.getWidth(), bmp.getHeight(), true, caras, Color.WHITE, null);
            mostrar("Aún no hay rostro de referencia.\nVe a la Etapa 2 y regístralo.");
            return;
        }
        if (reiniciarPedido) {
            reiniciarPedido = false;
            faseVida = FaseVida.PIDIENDO_PARPADEO;
            parpadeo.reiniciar();
            inicioFase = ahora;
        }
        if (faseVida == FaseVida.DECIDIDO) {
            superposicion.actualizar(bmp.getWidth(), bmp.getHeight(), true, caras,
                    colorResultado, etiquetaResultado);
            return;
        }

        if (cara == null || caras.size() > 1) {
            inicioFase = ahora; // el tiempo solo corre mientras haya un rostro
            parpadeo.reiniciar();
            superposicion.actualizar(bmp.getWidth(), bmp.getHeight(), true, caras, Color.WHITE, null);
            mostrar("Coloca tu rostro frente a la cámara (solo uno)");
            return;
        }

        long transcurrido = ahora - inicioFase;
        boolean completo = parpadeo.actualizar(
                cara.getLeftEyeOpenProbability(), cara.getRightEyeOpenProbability());

        if (!completo) {
            if (transcurrido > TIEMPO_PARPADEO_MS) {
                decidir("RECHAZADO\nNo se detectó parpadeo (posible foto o video).", ROJO, "SIN VIDA");
                superposicion.actualizar(bmp.getWidth(), bmp.getHeight(), true, caras, ROJO, "SIN VIDA");
                return;
            }
            long restante = (TIEMPO_PARPADEO_MS - transcurrido + 999) / 1000;
            superposicion.actualizar(bmp.getWidth(), bmp.getHeight(), true, caras, AMARILLO, "PARPADEA");
            mostrar("Prueba de vida: parpadea ahora, con los ojos bien abiertos antes y después.\n"
                    + "Tiempo restante: " + restante + " s");
            return;
        }

        // El parpadeo ya ocurrió: ahora se compara el rostro con la referencia.
        String problema = problemaCalidad(bmp, caras, cara);
        if (problema != null) {
            superposicion.actualizar(bmp.getWidth(), bmp.getHeight(), true, caras, AMARILLO, null);
            mostrar("Parpadeo detectado ✓\n" + problema);
            if (transcurrido > TIEMPO_PARPADEO_MS + TIEMPO_EXTRA_COMPARAR_MS) {
                decidir("RECHAZADO\nNo hubo una imagen válida para comparar.", ROJO, "RECHAZADO");
            }
            return;
        }
        float[] embedding = embedder.generar(FaceAligner.alinear(bmp, cara));
        float distancia = FaceComparator.distanciaCoseno(referencia, embedding);
        boolean mismaPersona = distancia <= umbral;
        String detalle = String.format(Locale.US, "\nDistancia coseno: %.3f (umbral %.2f)", distancia, umbral);
        if (mismaPersona) {
            decidir("ACEPTADO\nParpadeo ✓ y misma persona ✓" + detalle, VERDE, "ACEPTADO");
        } else {
            decidir("RECHAZADO\nParpadeo ✓ pero el rostro no coincide con la referencia." + detalle,
                    ROJO, "RECHAZADO");
        }
        superposicion.actualizar(bmp.getWidth(), bmp.getHeight(), true, caras, colorResultado, etiquetaResultado);
    }

    private void decidir(String texto, int color, String etiqueta) {
        faseVida = FaseVida.DECIDIDO;
        colorResultado = color;
        etiquetaResultado = etiqueta;
        mostrar(texto + "\n\nPulsa \"Reintentar\" para repetir.");
    }

    // ------------------------------------------------------------------ utilidades

    /** Devuelve null si el rostro sirve para registrar/comparar, o el motivo si no sirve. */
    private String problemaCalidad(Bitmap bmp, List<Face> caras, Face cara) {
        if (cara == null) {
            return "No se detecta ningún rostro";
        }
        if (caras.size() > 1) {
            return "Hay más de un rostro en la imagen";
        }
        if (cara.getBoundingBox().width() < 0.30f * bmp.getWidth()) {
            return "Acércate un poco a la cámara";
        }
        if (Math.abs(cara.getHeadEulerAngleY()) > 15f) {
            return "Mira de frente a la cámara";
        }
        Float izq = cara.getLeftEyeOpenProbability();
        Float der = cara.getRightEyeOpenProbability();
        if (izq != null && der != null && (izq + der) / 2f < 0.5f) {
            return "Abre los ojos";
        }
        return null;
    }

    private static Face mayorCara(List<Face> caras) {
        Face mayor = null;
        int areaMayor = -1;
        for (Face c : caras) {
            int area = c.getBoundingBox().width() * c.getBoundingBox().height();
            if (area > areaMayor) {
                areaMayor = area;
                mayor = c;
            }
        }
        return mayor;
    }

    private static String porcentaje(Float valor) {
        return valor == null ? "–" : Math.round(valor * 100) + "%";
    }

    private void mostrar(String texto) {
        runOnUiThread(() -> tvInfo.setText(texto));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Se cierran los modelos al final de la cola, para no cerrarlos mientras se analiza un fotograma.
        hiloAnalisis.execute(() -> {
            if (detector != null) {
                detector.cerrar();
            }
            if (embedder != null) {
                embedder.close();
            }
        });
        hiloAnalisis.shutdown();
    }
}
