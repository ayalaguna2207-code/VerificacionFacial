# Verificación facial móvil · instrucciones (5 pasos)

## 1. Copiar los archivos al proyecto
1. Cierra nada: deja Android Studio abierto.
2. Haz clic derecho en este zip → **Extraer todo…**
3. En "Seleccionar un destino" pega la carpeta de tu proyecto:
   `C:\Users\USO PERSONAL\AndroidStudioProjects\VerificacionFacial`
4. Cuando Windows pregunte por archivos que ya existen (`MainActivity.java`, `activity_main.xml`) elige **Reemplazar los archivos en el destino**.

Esto agrega: 8 clases Java, 2 layouts y el modelo `mobile_face_net.tflite` (carpeta `assets`).

## 2. Dependencias (`build.gradle.kts (Module :app)`)
Dentro del bloque `dependencies { ... }`, antes de la llave de cierre, pega:

```kotlin
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("com.google.mlkit:face-detection:16.1.7")
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
```

## 3. Manifiesto (`app/manifests/AndroidManifest.xml`)
a) Justo ANTES de la línea `<application`, pega:

```xml
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />
```

b) DENTRO de `<application ...>`, justo debajo de la etiqueta `<activity ...> ... </activity>` que ya existe (MainActivity), pega:

```xml
        <activity
            android:name=".CameraActivity"
            android:exported="false"
            android:screenOrientation="portrait" />
```

c) En la línea de `<application`, cambia `android:allowBackup="true"` por `android:allowBackup="false"` (así el rostro guardado no se respalda en la nube de Google).

## 4. Sincronizar
Arriba aparece una barra con **Sync Now**. Dale clic y espera a que termine (descarga las librerías; necesita internet SOLO esta vez).

## 5. Instalar
Celular conectado por USB → botón verde **Run**. La primera vez pedirá permiso de cámara en el celular.

## Si algo falla
- `Could not find com.google.mlkit:face-detection:16.1.7` → cambia `16.1.7` por `16.1.6`.
- Cualquier otro error: copia el mensaje completo (pestaña **Build** abajo) y mándalo.

## Guion para mostrar mañana
1. Etapa 1: se ve el recuadro y los ojos; muestra el % de ojos abiertos y sonrisa.
2. Etapa 2: acepta el consentimiento y registra tu rostro (aparece la miniatura en el menú).
3. Etapa 3: la distancia coseno en vivo. Tú: baja de ~0.45. Otra persona: sube. Mueve el umbral con la barra.
4. Etapa 4: parpadea para pasar la prueba de vida. Prueba con una foto en otra pantalla: no debería pasar.

## Límites que debes decir (están en tu propuesta)
- El umbral inicial (0.45) es provisional; se calibra con tus pruebas.
- El parpadeo con ML Kit no detiene un video de ti mismo parpadeando.
- El modelo es MobileFaceNet preentrenado (no es la red de [1] ni de [6]); sigue el mismo principio: embedding ligero + distancia + umbral.
