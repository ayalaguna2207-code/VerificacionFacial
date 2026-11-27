package com.laura.verificacion;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.face.Face;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Verificación 1:1 en tres fases:
 * 1) ALINEAR: un solo rostro, buena luz, de frente.
 * 2) VIDA: desafío activo y aleatorio (parpadeo o giro). Si no se cumple a tiempo, se rechaza.
 * 3) COMPARAR: foto frontal, embedding, distancia coseno contra los 3 de referencia y umbral.
 */
public class VerificarActivity extends AppCompatActivity {

    private enum Fase { ALINEAR, VIDA, COMPARAR, FIN }

    private static final int CUADROS_ESTABLES = 5;
    private static final long TIEMPO_VIDA_MS = 8000L;
    private static final long TIEMPO_COMPARAR_MS = 10000L;
    private static final int VERDE = Color.rgb(46, 204, 113);
    private static final int AMARILLO = Color.rgb(241, 196, 15);

    private PreviewView vistaPrevia;
    private FaceOverlayView superposicion;
    private PoseGuideView guia;
    private TextView tvFase;
    private TextView tvInstruccion;
    private TextView tvEstado;
    private TextView tvModoPrueba;
    private View panelResultado;
    private TextView tvResultado;
    private TextView tvDetalle;

    private final ExecutorService hiloAnalisis = Executors.newSingleThreadExecutor();
    private FaceDetectorHelper detector;
    private FaceEmbedder embedder;
    private final Random azar = new Random();

    // Estado: solo se toca desde el hilo de análisis
    private Fase fase = Fase.ALINEAR;
    private int estables = 0;
    private DesafioVida desafio;
    private boolean vidaSuperada = false;
    private long inicioComparar = 0L;
    private float luminanciaActual = 0f;
    private float umbral;

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
        Usuario usuario = Sesion.actual();
        if (usuario == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        if (!usuario.tieneEnrolamiento()) {
            Toast.makeText(this, "Primero registra tus 3 poses", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        setContentView(R.layout.activity_verify);

        final View barraSuperior = findViewById(R.id.barraSuperior);
        final View barraInferior = findViewById(R.id.barraInferior);
        final int relleno = barraSuperior.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.raiz), (vista, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            barraSuperior.setPadding(barraSuperior.getPaddingLeft(), relleno + b.top,
                    barraSuperior.getPaddingRight(), barraSuperior.getPaddingBottom());
            barraInferior.setPadding(barraInferior.getPaddingLeft(), barraInferior.getPaddingTop(),
                    barraInferior.getPaddingRight(), relleno + b.bottom);
            return insets;
        });

        vistaPrevia = findViewById(R.id.previewView);
        superposicion = findViewById(R.id.overlay);
        guia = findViewById(R.id.guia);
        guia.setGenero(Sesion.actual().getGenero());
        tvFase = findViewById(R.id.tvFase);
        tvInstruccion = findViewById(R.id.tvInstruccion);
        tvEstado = findViewById(R.id.tvEstado);
        tvModoPrueba = findViewById(R.id.tvModoPrueba);
        panelResultado = findViewById(R.id.panelResultado);
        tvResultado = findViewById(R.id.tvResultado);
        tvDetalle = findViewById(R.id.tvDetalle);

        findViewById(R.id.btnReintentar).setOnClickListener(v -> hiloAnalisis.execute(this::reiniciar));
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());

        umbral = Umbral.obtener(this);
        if (RegistroPruebas.activo(this)) {
            tvModoPrueba.setText("MODO PRUEBA: " + RegistroPruebas.etiqueta(this)
                    + " / luz " + RegistroPruebas.iluminacion(this));
        }

        try {
            detector = new FaceDetectorHelper();
            embedder = new FaceEmbedder(this);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo cargar el modelo: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mostrarFaseAlinear();

        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara();
        } else {
            pedirPermiso.launch(Manifest.permission.CAMERA);
        }
    }

    // ------------------------------------------------------------------ cámara

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
        Preview vista = new Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build();
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
            mostrarEstado("Error: " + e.getMessage());
        } finally {
            imagen.close();
        }
    }

    // ------------------------------------------------------------------ lógica por fases

    private void procesar(Bitmap bmp) throws Exception {
        if (fase == Fase.FIN) {
            return;
        }
        long ahora = SystemClock.elapsedRealtime();
        List<Face> caras = detector.detectar(bmp);
        Face cara = caras.size() == 1 ? caras.get(0) : null;
        String problema = problemaBase(bmp, caras, cara);

        switch (fase) {
            case ALINEAR:
                faseAlinear(bmp, caras, cara, problema);
                break;
            case VIDA:
                faseVida(bmp, caras, cara, problema, ahora);
                break;
            case COMPARAR:
                faseComparar(bmp, caras, cara, problema, ahora);
                break;
            default:
                break;
        }
    }

    private void faseAlinear(Bitmap bmp, List<Face> caras, Face cara, String problema) {
        if (problema == null) {
            problema = problemaFrente(cara);
        }
        if (problema != null) {
            estables = 0;
            superposicion.actualizar(bmp.getWidth(), bmp.getHeight(), true, caras, AMARILLO, null);
            mostrarEstado(problema);
            return;
        }
        estables++;
        superposicion.actualizar(bmp.getWidth(), bmp.getHeight(), true, caras, VERDE, null);
        mostrarEstado("Rostro correcto, no te muevas (" + estables + "/" + CUADROS_ESTABLES + ")");
        if (estables >= CUADROS_ESTABLES) {
            estables = 0;
            desafio = DesafioVida.aleatorio(azar, TIEMPO_VIDA_MS, SystemClock.elapsedRealtime());
            fase = Fase.VIDA;
            final DesafioVida d = desafio;
            runOnUiThread(() -> {
                tvFase.setText("Paso 2 de 3 · Prueba de vida");
                tvInstruccion.setText(d.instruccion());
                guia.setPose(d.poseGuia());
            });
        }
    }

    private void faseVida(Bitmap bmp, List<Face> caras, Face cara, String problema, long ahora) {
        if (desafio.revisarTiempo(ahora) == DesafioVida.Estado.EXPIRADO) {
            terminar(false, Intento.NO_COMPARADO, 0L, "No superaste la prueba de vida a tiempo");
            return;
        }
        if (problema != null) {
            superposicion.actualizar(bmp.getWidth(), bmp.getHeight(), true, caras, AMARILLO, null);
            mostrarEstado(problema + " · " + desafio.segundosRestantes(ahora) + " s");
            return;
        }
        float giro = PoseValidator.haciaIzquierda(cara.getHeadEulerAngleY());
        DesafioVida.Estado e = desafio.actualizar(cara.getLeftEyeOpenProbability(),
                cara.getRightEyeOpenProbability(), giro, ahora);
        superposicion.actualizar(bmp.getWidth(), bmp.getHeight(), true, caras, VERDE, null);
        if (e == DesafioVida.Estado.SUPERADO) {
            vidaSuperada = true;
            fase = Fase.COMPARAR;
            estables = 0;
            inicioComparar = ahora;
            runOnUiThread(() -> {
                tvFase.setText("Paso 3 de 3 · Comparación");
                tvInstruccion.setText("Mira de frente a la cámara");
                guia.setPose(Pose.FRENTE);
            });
            mostrarEstado("✓ Prueba de vida superada");
        } else if (e == DesafioVida.Estado.EXPIRADO) {
            terminar(false, Intento.NO_COMPARADO, 0L, "No superaste la prueba de vida a tiempo");
        } else {
            mostrarEstado(desafio.instruccion() + " · " + desafio.segundosRestantes(ahora) + " s");
        }
    }

    private void faseComparar(Bitmap bmp, List<Face> caras, Face cara, String problema, long ahora) throws Exception {
        if (ahora - inicioComparar > TIEMPO_COMPARAR_MS) {
            terminar(true, Intento.NO_COMPARADO, 0L, "No se logró una imagen frontal para comparar");
            return;
        }
        if (problema == null) {
            problema = problemaFrente(cara);
        }
        if (problema != null) {
            estables = 0;
            superposicion.actualizar(bmp.getWidth(), bmp.getHeight(), true, caras, AMARILLO, null);
            mostrarEstado(problema);
            return;
        }
        estables++;
        superposicion.actualizar(bmp.getWidth(), bmp.getHeight(), true, caras, VERDE, null);
        mostrarEstado("Comparando… (" + estables + "/" + CUADROS_ESTABLES + ")");
        if (estables < CUADROS_ESTABLES) {
            return;
        }

        // Tiempo de procesamiento: alinear + embedding + comparación con las 3 referencias.
        long t0 = System.nanoTime();
        Bitmap rostro = FaceAligner.alinear(bmp, cara);
        float[] embedding = embedder.generar(rostro);
        float distancia = Verificador.mejorDistancia(embedding, Sesion.actual().getEmbeddings());
        long ms = (System.nanoTime() - t0) / 1_000_000L;
        terminar(true, distancia, ms, null);
    }

    private void terminar(boolean vida, float distancia, long ms, String motivo) {
        fase = Fase.FIN;
        final boolean acepta = vida && Verificador.acepta(distancia, umbral);
        RegistroPruebas.registrar(this, distancia, ms, vida, luminanciaActual);

        final StringBuilder d = new StringBuilder();
        if (motivo != null) {
            d.append(motivo).append("\n\n");
        }
        d.append("Prueba de vida: ").append(vida ? "superada" : "NO superada").append('\n');
        if (distancia >= 0f) {
            d.append(String.format(Locale.US, "Distancia coseno: %.3f\nUmbral: %.2f\nTiempo de procesamiento: %d ms",
                    distancia, umbral, ms));
        }
        runOnUiThread(() -> {
            tvResultado.setText(acepta ? "✓ Identidad verificada" : "✗ Acceso denegado");
            tvResultado.setTextColor(acepta ? Color.rgb(27, 140, 80) : Color.rgb(192, 57, 43));
            tvDetalle.setText(d.toString());
            panelResultado.setVisibility(View.VISIBLE);
            if (!acepta) {
                tvDetalle.append("\n\nLa sesión se cerrará por seguridad.");
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Sesion.cerrar();
                    Intent salida = new Intent(this, LoginActivity.class);
                    salida.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(salida);
                    finish();
                }, 3000L);
            }
        });
    }

    private void reiniciar() {
        fase = Fase.ALINEAR;
        estables = 0;
        desafio = null;
        vidaSuperada = false;
        umbral = Umbral.obtener(this);
        mostrarFaseAlinear();
        runOnUiThread(() -> panelResultado.setVisibility(View.GONE));
    }

    private void mostrarFaseAlinear() {
        runOnUiThread(() -> {
            tvFase.setText("Paso 1 de 3 · Alineación");
            tvInstruccion.setText("Mira de frente a la cámara");
            guia.setPose(Pose.FRENTE);
        });
    }

    // ------------------------------------------------------------------ validaciones de imagen

    /** Null si la imagen sirve; si no, el motivo. */
    private String problemaBase(Bitmap bmp, List<Face> caras, Face cara) {
        if (caras.isEmpty()) {
            return "No se detecta ningún rostro";
        }
        if (caras.size() > 1) {
            return "Hay más de un rostro en la imagen: debe aparecer solo el tuyo";
        }
        Rect caja = cara.getBoundingBox();
        if (caja.width() < 0.30f * bmp.getWidth()) {
            return "Acércate un poco a la cámara";
        }
        if (caja.left < 0 || caja.top < 0 || caja.right > bmp.getWidth() || caja.bottom > bmp.getHeight()) {
            return "Centra tu rostro: se está saliendo de la imagen";
        }
        luminanciaActual = luminanciaDe(bmp, caja);
        return LightAnalyzer.problema(luminanciaActual);
    }

    /** Exige rostro de frente y ojos abiertos (para alinear y para comparar). */
    private String problemaFrente(Face cara) {
        float y = cara.getHeadEulerAngleY();
        float x = cara.getHeadEulerAngleX();
        if (!PoseValidator.cumple(Pose.FRENTE, y, x)) {
            return PoseValidator.sugerencia(Pose.FRENTE, y, x);
        }
        Float izq = cara.getLeftEyeOpenProbability();
        Float der = cara.getRightEyeOpenProbability();
        if (izq != null && der != null && (izq + der) / 2f < 0.5f) {
            return "Abre bien los ojos";
        }
        return null;
    }

    private static float luminanciaDe(Bitmap bmp, Rect caja) {
        int izq = Math.max(0, caja.left);
        int arr = Math.max(0, caja.top);
        int ancho = Math.min(bmp.getWidth() - izq, caja.width());
        int alto = Math.min(bmp.getHeight() - arr, caja.height());
        if (ancho < 1 || alto < 1) {
            return 0f;
        }
        Bitmap recorte = Bitmap.createBitmap(bmp, izq, arr, ancho, alto);
        Bitmap pequeno = Bitmap.createScaledBitmap(recorte, 24, 24, true);
        int[] pixeles = new int[24 * 24];
        pequeno.getPixels(pixeles, 0, 24, 0, 0, 24, 24);
        return LightAnalyzer.luminanciaMedia(pixeles);
    }

    private void mostrarEstado(String texto) {
        runOnUiThread(() -> tvEstado.setText(texto));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
