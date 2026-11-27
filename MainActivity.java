package com.laura.verificacion;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;

/** Panel principal simplificado: verificación, resultados y administración desde Mi cuenta. */
public class MainActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        if (Sesion.actual() == null) { irALogin(); return; }
        setContentView(R.layout.activity_main);
        Bordes.aplicar(this);
        findViewById(R.id.btnVerificar).setOnClickListener(v -> {
            Usuario u = Sesion.actual();
            if (u != null && u.tieneEnrolamiento()) startActivity(new Intent(this, VerificarActivity.class));
            else Toast.makeText(this, "Primero completa el registro facial", Toast.LENGTH_LONG).show();
        });
        findViewById(R.id.btnResultados).setOnClickListener(v -> startActivity(new Intent(this, ResultadosActivity.class)));
        findViewById(R.id.btnMiCuenta).setOnClickListener(v -> mostrarMiCuenta());
        findViewById(R.id.btnCerrarSesion).setOnClickListener(v -> irALogin());
    }

    @Override protected void onResume() {
        super.onResume();
        Usuario u = Sesion.actual();
        if (u == null) { irALogin(); return; }
        android.widget.TextView bienvenida = findViewById(R.id.tvBienvenida);
        android.widget.TextView estado = findViewById(R.id.tvEstadoRostro);
        android.widget.TextView umbral = findViewById(R.id.tvUmbralActual);
        if (bienvenida != null) bienvenida.setText("Hola, " + u.getNombre());
        if (estado != null) estado.setText(u.tieneEnrolamiento()
                ? "Registro facial completado · 3 referencias protegidas"
                : "Registro facial pendiente");
        if (umbral != null) umbral.setText(String.format(java.util.Locale.US,
                "Umbral de verificación: %.2f", Umbral.obtener(this)));
    }

    private void mostrarMiCuenta() {
        String[] opciones = {"Volver a registrar las 3 poses", "Cambiar guía de referencia", "Eliminar mi cuenta y datos"};
        new AlertDialog.Builder(this).setTitle("Mi cuenta").setItems(opciones, (dialog, opcion) -> {
            if (opcion == 0) confirmarRecaptura();
            else if (opcion == 1) cambiarGenero();
            else confirmarEliminacion();
        }).setNegativeButton("Cerrar", null).show();
    }

    private void confirmarRecaptura() {
        new AlertDialog.Builder(this).setTitle("Actualizar referencias faciales")
                .setMessage("Se reemplazarán las tres referencias guardadas. ¿Deseas continuar?")
                .setPositiveButton("Continuar", (d, w) -> startActivity(new Intent(this, EnrollActivity.class)))
                .setNegativeButton("Cancelar", null).show();
    }

    private void cambiarGenero() {
        Usuario u = Sesion.actual();
        if (u == null) return;
        u.setGenero(u.getGenero().otro());
        try { UserRepository.obtener(this).guardar(); }
        catch (IOException e) { Toast.makeText(this, "No se pudo guardar el cambio", Toast.LENGTH_LONG).show(); }
        Toast.makeText(this, "Guía de referencia actualizada", Toast.LENGTH_SHORT).show();
    }

    private void confirmarEliminacion() {
        new AlertDialog.Builder(this).setTitle("Eliminar mi cuenta")
                .setMessage("Se borrarán los datos de usuario y las referencias faciales almacenadas en este dispositivo. Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (d, w) -> {
                    try { UserRepository.obtener(this).eliminar(Sesion.actual());
                        Toast.makeText(this, "Cuenta eliminada", Toast.LENGTH_LONG).show(); irALogin();
                    } catch (IOException e) { Toast.makeText(this, "No se pudo eliminar: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
                }).setNegativeButton("Cancelar", null).show();
    }

    private void irALogin() {
        Sesion.cerrar();
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i); finish();
    }
}
