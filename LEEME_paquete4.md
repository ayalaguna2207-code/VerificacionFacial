# Paquete 4 — Flujo reorganizado y sesión

## Cambios incluidos
- Menú principal simplificado: Verificación facial, Pruebas y resultados, Mi cuenta y Cerrar sesión.
- Recaptura de las 3 poses y cambio de guía quedan dentro de Mi cuenta, no en el menú principal.
- Recaptura protegida con confirmación.
- Eliminación de cuenta y datos protegida con confirmación.
- Al fallar la verificación facial (prueba de vida o comparación), se muestra el resultado y se cierra la sesión tras 3 segundos, regresando al inicio de sesión.
- Se conserva el bloqueo de inicio de sesión tras 3 credenciales incorrectas, durante 60 segundos (implementado en `LockoutPolicy`).
- Se conserva el proyecto base y el paquete 3, incluyendo el modelo TFLite y las pantallas de enrolamiento y resultados.

## Importante
Este paquete no debe presentarse como un sistema biométrico de seguridad de producción. El desafío de vida, los controles de iluminación y el detector facial necesitan pruebas reales con foto impresa, foto en pantalla, video, distintos dispositivos y distintas iluminaciones. La detección de un rostro por sí sola no demuestra que sea una persona viva.

## Compilación
Se intentó `assembleDebug`, pero este entorno no pudo descargar Gradle por falta de conexión. Abre el proyecto en Android Studio, deja que Gradle sincronice y compila desde Build > Build APK(s). Si aparece un error, envíame la captura o el texto del error para corregirlo.
