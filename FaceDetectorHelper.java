package com.laura.verificacion;

import android.graphics.Bitmap;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

/**
 * Envuelve ML Kit Face Detection (modelo incluido en la app, funciona sin internet).
 * Devuelve rostros con cuadro delimitador, puntos de los ojos y probabilidad de ojo abierto.
 */
public class FaceDetectorHelper {

    private final FaceDetector detector;

    public FaceDetectorHelper() {
        FaceDetectorOptions opciones = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.25f)
                .build();
        detector = FaceDetection.getClient(opciones);
    }

    /** Bloquea el hilo que la llama: usarlo solo desde el hilo de análisis, nunca desde el principal. */
    public List<Face> detectar(Bitmap imagenVertical) throws Exception {
        InputImage imagen = InputImage.fromBitmap(imagenVertical, 0);
        return Tasks.await(detector.process(imagen));
    }

    public void cerrar() {
        detector.close();
    }
}
