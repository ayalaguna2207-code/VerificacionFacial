package com.laura.verificacion;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Pantalla de inicio de sesión: usuario y contraseña, con bloqueo tras intentos fallidos. */
public class LoginActivity extends AppCompatActivity {

    private UserRepository repositorio;
    private TextInputEditText etUsuario;
    private TextInputEditText etClave;
    private TextView tvMensaje;
    private Button btnEntrar;

    private final Handler manejador = new Handler(Looper.getMainLooper());
    private final ExecutorService hilo = Executors.newSingleThreadExecutor();
    private Runnable cuentaRegresiva;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        setContentView(R.layout.activity_login);
        Bordes.aplicar(this);
        Sesion.cerrar();

        repositorio = UserRepository.obtener(this);
        etUsuario = findViewById(R.id.etUsuario);
        etClave = findViewById(R.id.etClave);
        tvMensaje = findViewById(R.id.tvMensaje);
        btnEntrar = findViewById(R.id.btnEntrar);

        btnEntrar.setOnClickListener(v -> intentarEntrar());
        findViewById(R.id.btnCrearCuenta).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void intentarEntrar() {
        final String usuario = textoDe(etUsuario).trim();
        final String clave = textoDe(etClave);
        if (usuario.isEmpty() || clave.isEmpty()) {
            mostrarError("Escribe tu usuario y tu contraseña");
            return;
        }
        final LockoutPolicy politica = repositorio.politicaDe(usuario);
        if (politica.estaBloqueado(System.currentTimeMillis())) {
            iniciarCuentaRegresiva(politica);
            return;
        }

        final Usuario u = repositorio.buscar(usuario);
        btnEntrar.setEnabled(false);
        tvMensaje.setText("");
        hilo.execute(() -> {
            // Si el usuario no existe, se hace igual un hash para que tarde lo mismo.
            final boolean correcta;
            if (u != null) {
                correcta = PasswordHasher.verificar(clave, u.getHashClave());
            } else {
                PasswordHasher.hash(clave);
                correcta = false;
            }
            runOnUiThread(() -> terminarIntento(u, politica, correcta));
        });
    }

    private void terminarIntento(Usuario u, LockoutPolicy politica, boolean correcta) {
        btnEntrar.setEnabled(true);
        long ahora = System.currentTimeMillis();
        if (correcta) {
            politica.registrarExito();
            guardarSilencioso();
            etClave.setText("");
            Sesion.iniciar(u);
            Class<?> destino = u.tieneEnrolamiento() ? MainActivity.class : EnrollActivity.class;
            startActivity(new Intent(this, destino));
            finish();
            return;
        }
        politica.registrarFallo(ahora);
        guardarSilencioso();
        if (politica.estaBloqueado(ahora)) {
            iniciarCuentaRegresiva(politica);
        } else {
            mostrarError("Usuario o contraseña incorrectos. Intentos restantes: "
                    + politica.intentosRestantes());
        }
    }

    private void iniciarCuentaRegresiva(LockoutPolicy politica) {
        btnEntrar.setEnabled(false);
        if (cuentaRegresiva != null) {
            manejador.removeCallbacks(cuentaRegresiva);
        }
        cuentaRegresiva = new Runnable() {
            @Override
            public void run() {
                long restante = politica.segundosRestantes(System.currentTimeMillis());
                if (restante > 0) {
                    mostrarError("Demasiados intentos fallidos. Intenta de nuevo en " + restante + " s");
                    manejador.postDelayed(this, 1000);
                } else {
                    tvMensaje.setText("Ya puedes intentarlo de nuevo");
                    btnEntrar.setEnabled(true);
                }
            }
        };
        manejador.post(cuentaRegresiva);
    }

    private void guardarSilencioso() {
        try {
            repositorio.guardar();
        } catch (IOException e) {
            // Si no se pudo guardar el contador, el inicio de sesión sigue funcionando.
        }
    }

    private void mostrarError(String texto) {
        tvMensaje.setText(texto);
    }

    private static String textoDe(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        manejador.removeCallbacksAndMessages(null);
        hilo.shutdown();
    }
}
