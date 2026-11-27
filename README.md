# ValidaID · Verificación de identidad facial en Android

Proyecto final de Programación Orientada a Objetos (Universidad Distrital Francisco José de Caldas).
App Android en Java que verifica identidad 1:1 con reconocimiento facial **100 % en el dispositivo**.

## Flujo
1. **Inicio de sesión** con contraseña (hash PBKDF2 con sal), cierre de sesión y bloqueo temporal tras intentos fallidos.
2. **Registro** con consentimiento informado. Los datos y los 3 embeddings de referencia se guardan en un único archivo cifrado (AES-256-GCM, llave en Android Keystore). No se guardan fotos.
3. **Captura guiada en 3 poses** (frente, izquierda, derecha) con dibujo guía; valida un solo rostro, luz, tamaño y ángulo.
4. **Verificación 1:1**: alineación → prueba de vida activa y aleatoria (parpadeo o giro) → embedding (MobileFaceNet TFLite) → distancia coseno mínima contra las 3 referencias → comparación con el umbral.
5. **Pruebas y resultados**: exactitud, FAR, FRR, EER, tiempos, tamaño del modelo, memoria, iluminación y ataques; curva FAR/FRR para calibrar el umbral.

## Estructura (`app/src/main/java/com/laura/verificacion`)
| Clase | Rol |
|---|---|
| `LoginActivity`, `RegisterActivity`, `MainActivity` | Acceso, registro y menú |
| `UserRepository`, `SecureStorage`, `Usuario`, `Sesion` | Datos cifrados y sesión |
| `PasswordHasher`, `LockoutPolicy` | Contraseña y bloqueo |
| `EnrollActivity`, `PoseValidator`, `LightAnalyzer`, `Pose` | Captura en 3 poses |
| `FaceDetectorHelper`, `FaceAligner`, `FaceEmbedder`, `FaceComparator` | Detección, alineación, embedding y distancia |
| `VerificarActivity`, `DesafioVida`, `Verificador`, `Umbral` | Verificación 1:1 y liveness |
| `ResultadosActivity`, `CurvaView`, `Calibrador`, `Intento`, `RegistroPruebas` | Pruebas, métricas y curva |

## Compilar
Android Studio, abrir la carpeta, **Sync**, **Run** con un celular con cámara frontal (permiso de cámara en el primer uso).
APK: **Build → Build APK(s)**. El modelo `mobile_face_net.tflite` va en `app/src/main/assets/`.

## Pruebas unitarias
`app/src/test/java/com/laura/verificacion`: clic derecho sobre la carpeta → **Run 'Tests in ...'**.
Clases de dominio cubiertas: `PasswordHasher`, `LockoutPolicy`, `PoseValidator`, `LightAnalyzer`, `FaceComparator`, `DesafioVida`, `Verificador`, `Calibrador`, `Intento`.

## Limitaciones
- Modelo preentrenado (no se entrenó uno propio); el umbral se calibra con pruebas propias.
- El liveness basado en ML Kit puede ser vulnerable a video; se documenta en los resultados de ataques.
- Pocos usuarios de prueba: FAR/FRR son preliminares y no equivalentes a los de LFW reportados en la literatura.
