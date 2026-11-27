# Paquete 3 · Fotos de referencia por género (hombre / mujer)

Va encima del paquete 2. Reemplaza: Usuario, RegisterActivity, EnrollActivity, VerificarActivity,
MainActivity, PoseGuideView, activity_register.xml, activity_enroll.xml, activity_verify.xml y activity_main.xml.
Agrega: Genero.java y 6 fotos en `res/drawable-nodpi/` (guia_mujer_frente/izquierda/derecha, guia_hombre_...).

## Cómo funciona
1. En **Crear cuenta** hay que elegir Mujer u Hombre (obligatorio).
2. En la captura de las 3 poses y en la verificación (prueba de vida de giro), arriba sale la foto de referencia
   de la pose pedida, con una flecha amarilla hacia el lado en que debes girar.
3. Las cuentas ya creadas quedan como Mujer; en el menú hay un botón para cambiarlo.

## Instalar
1. Extrae `paquete3.zip` sobre la carpeta del proyecto (sin agregar `\paquete3` a la ruta) y elige **Reemplazar**.
2. **File → Sync Project with Gradle Files** y **Run**.
3. Si el giro sale al revés: `PoseValidator.SIGNO_IZQUIERDA` de `1f` a `-1f`.
