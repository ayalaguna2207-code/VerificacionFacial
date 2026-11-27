package com.laura.verificacion;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Bundle;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Captura guiada en tres poses (frente, izquierda, derecha).
 * En cada fotograma valida: un solo rostro, tamaño, rostro completo en imagen, iluminación,
 * ojos abiertos (de frente) y ángulo de la cabeza. Si todo está bien durante varios cuadros
 * seguidos, captura el embedding de esa pose y pasa a la siguiente.
 */
public class EnrollActivity extends AppCompatActivity {

    private static final int CUADROS_ESTABLES = 6;
    private static final long PAUSA_ENTRE_POSES_MS = 1500L;
    private static final int VERDE = Color.rgb(46, 204, 113);
    private static final int AMARILLO = Color.rgb(241, 196, 15);

    private PreviewView vistaPrevia;
    private FaceOverlayView superposicion;
    private PoseGuideView guia;
    private TextView tvPose;
    private TextView tvInstruccion;
    private TextView tvEstado;
    private TextView tvAngulo;

    private final ExecutorService hiloAnalisis = Executors.newSingleThreadExecutor();
    private FaceDetectorHelper detector;
    private FaceEmbedder embedder;

    // Estado: solo se toca desde el hilo de análisis
    private final Pose[] poses = Pose.values();
    private int indice = 0;
    private int estables = 0;
    private long pausaHasta = 0L;
    private boolean terminado = false;
    private final List<float[]> capturados = new ArrayList<>();

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
        if (Sesion.actual() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_enroll);

        // Las barras de arriba y abajo se separan de la barra de estado y de navegación.
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
        tvPose = findViewById(R.id.tvPose);
        tvInstruccion = findViewById(R.id.tvInstruccion);
        tvEstado = findViewById(R.id.tvEstado);
        tvAngulo = findViewById(R.id.tvAngulo);

        try {
            detector = new FaceDetectorHelper();
            embedder = new FaceEmbedder(this);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo cargar el modelo: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mostrarPose();

        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara();
        } else {
            pedirPermiso.launch(Manifest.permission.CAMERA);
        }
    }

    private void mostrarPose() {
        final Pose pose = poses[indice];
        final int numero = indice + 1;
        runOnUiThread(() -> {
            guia.setPose(pose);
            tvPose.setText("Pose " + numero + " de " + poses.length);
            tvInstruccion.setText(pose.instruccion);
        });
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
            mostrarEstado("Error: " + e.getMessage());
        } finally {
            imagen.close();
        }
    }

    private void procesar(Bitmap bmp) throws Exception {
        if (terminado) {
            return;
        }
        long ahora = SystemClock.elapsedRealtime();
        List<Face> caras = detector.detectar(bmp);

        if (ahora < pausaHasta) {
            superposicion.actualizar(bmp.getWidth(), bmp.getHeight(), true, caras, VERDE, "✓ Guardada");
            return;
        }

        Pose pose = poses[indice];
        Face cara = caras.size() == 1 ? caras.get(0) : null;
        String problema = problemaImagen(bmp, caras, cara, pose);

        float giro = 0f;
        if (cara != null) {
            giro = PoseValidator.haciaIzquierda(cara.getHeadEulerAngleY());
            final String angulo = String.format(Locale.US, "Giro: %+.0f° (positivo = a tu izquierda)", giro);
            runOnUiThread(() -> tvAngulo.setText(angulo));
        }
        if (problema == null) {
            float y = cara.getHeadEulerAngleY();
            float x = cara.getHeadEulerAngleX();
            if (!PoseValidator.cumple(pose, y, x)) {
                problema = PoseValidator.sugerencia(pose, y, x);
            }
        }

        if (problema != null) {
            estables = 0;
            superposicion.actualizar(bmp.getWidth(), bmp.getHeight(), true, caras, AMARILLO, null);
            mostrarEstado(problema);
            return;
        }

        estables++;
        superposicion.actualizar(bmp.getWidth(), bmp.getHeight(), true, caras, VERDE, null);
        mostrarEstado("Pose correcta, no te muevas (" + estables + "/" + CUADROS_ESTABLES + ")");
        if (estables < CUADROS_ESTABLES) {
            return;
        }

        // Pose estable: se captura.
        Bitmap rostro = FaceAligner.alinear(bmp, cara);
        capturados.add(embedder.generar(rostro));
        estables = 0;
        indice++;
        if (indice >= poses.length) {
            finalizar();
        } else {
            pausaHasta = ahora + PAUSA_ENTRE_POSES_MS;
            mostrarPose();
            mostrarEstado("✓ Pose guardada. Prepárate para la siguiente.");
        }
    }

    /** Devuelve null si la imagen sirve, o el motivo si no sirve. */
    private String problemaImagen(Bitmap bmp, List<Face> caras, Face cara, Pose pose) {
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
        String luz = LightAnalyzer.problema(luminanciaDe(bmp, caja));
        if (luz != null) {
            return luz;
        }
        if (pose == Pose.FRENTE) {
            Float izq = cara.getLeftEyeOpenProbability();
            Float der = cara.getRightEyeOpenProbability();
            if (izq != null && der != null && (izq + der) / 2f < 0.5f) {
                return "Abre bien los ojos";
            }
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

    private void finalizar() {
        terminado = true;
        Usuario usuario = Sesion.actual();
        if (usuario == null) {
            return;
        }
        usuario.setEmbeddings(capturados);
        try {
            UserRepository.obtener(this).guardar();
            runOnUiThread(() -> {
                Toast.makeText(this, "Registro completo: 3 poses guardadas", Toast.LENGTH_LONG).show();
                Intent menu = new Intent(this, MainActivity.class);
                menu.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(menu);
                finish();
            });
        } catch (IOException e) {
            terminado = false;
            capturados.clear();
            indice = 0;
            mostrarPose();
            mostrarEstado("No se pudieron guardar los datos: " + e.getMessage());
        }
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
