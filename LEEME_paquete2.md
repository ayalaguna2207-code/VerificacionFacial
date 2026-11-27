# Paquete 2 · Verificación 1:1, prueba de vida aleatoria, resultados y curva FAR/FRR

Cubre los puntos 4, 5, 6 (la app que genera los datos) y suma pruebas unitarias para el punto 8.
Va encima del paquete 1. Usa tus clases ya existentes: `FaceDetectorHelper`, `FaceEmbedder`,
`FaceAligner`, `FaceOverlayView`, `FaceComparator`, `PoseValidator`, `PoseGuideView`.

## Paso 1 · Copiar
1. Clic derecho en `paquete2.zip` → **Extraer todo…**
2. Destino: `C:\Users\USO PERSONAL\AndroidStudioProjects\VerificacionFacial`
3. Si pregunta por archivos existentes → **Reemplazar** (cambia `MainActivity.java`,
   `activity_main.xml` y `AndroidManifest.xml`; el manifiesto ya trae las 2 pantallas nuevas).
4. En Android Studio: **File → Sync Project with Gradle Files**. No hay dependencias nuevas.

## Paso 2 · Qué se agrega
- `VerificarActivity` + `activity_verify.xml`: alinear → prueba de vida (parpadeo, giro izq. o giro der., al azar, 8 s) → comparar.
- `DesafioVida`, `Verificador`, `Calibrador`, `Umbral`: lógica del dominio.
- `Intento`, `RegistroPruebas`: guardan cada verificación del modo prueba (solo números).
- `ResultadosActivity` + `CurvaView`: curva FAR/FRR, EER, exactitud, tiempos, tamaño del modelo, memoria, iluminación y ataques.
- 4 clases de prueba nuevas (19 pruebas).

## Paso 3 · Correr
Celular por USB → **Run**. Si sale rojo, copia el mensaje de la pestaña **Build**.
Posible choque: si ya tenías una clase llamada `Verificador`, `Umbral`, `Intento` o `Calibrador`, avísame el nombre.

## Paso 4 · Cómo sacar los datos del punto 6 (en este orden)
1. Menú → **Pruebas y resultados** → activa **Modo prueba**, etiqueta `GENUINO`, luz `ALTA` → **Aplicar**.
2. Atrás → **Verificar mi identidad**. Hazlo unas 10 veces tú (cada intento queda guardado solo).
3. Etiqueta `IMPOSTOR`: otras personas (varias) se verifican con TU cuenta.
4. Repite 2 y 3 con luz `MEDIA` y `BAJA`.
5. Ataques, 10 intentos cada uno: `ATAQUE_FOTO_IMPRESA`, `ATAQUE_FOTO_PANTALLA`, `ATAQUE_VIDEO` (foto tuya impresa, foto en otra pantalla, video tuyo).
6. Resultados → botón **EER** → **Guardar este umbral en la app**. Ese umbral es el calibrado con datos.
7. Cuando termines, desactiva el Modo prueba.

## Si el giro sale al revés
Es el mismo ajuste de antes: `PoseValidator.SIGNO_IZQUIERDA` de `1f` a `-1f`. Afecta registro y liveness a la vez.

## Límites que debes decir (y que salen de los datos, no los escondas)
- El parpadeo/giro con ML Kit puede dejar pasar un video tuyo; el azar del desafío lo dificulta pero no lo elimina.
- Tras la prueba de vida se toma una foto frontal: alguien podría cambiar de cara en ese momento. Es una limitación conocida.
- El umbral 0.45 es inicial; el válido es el que sale de tus pruebas.
- Con pocas personas, FAR/FRR son preliminares y no comparables con LFW ([1], [6]).
