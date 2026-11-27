package com.laura.verificacion;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/** Registro: datos, contraseña y consentimiento informado. Después sigue la captura del rostro. */
public class RegisterActivity extends AppCompatActivity {

    private static final Pattern PATRON_USUARIO = Pattern.compile("[A-Za-z0-9_.]{3,20}");

    private TextInputEditText etNombre;
    private TextInputEditText etUsuario;
    private TextInputEditText etClave;
    private TextInputEditText etClave2;
    private CheckBox cbConsentimiento;
    private RadioGroup rgGenero;
    private TextView tvError;
    private Button btnContinuar;

    private final ExecutorService hilo = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        setContentView(R.layout.activity_register);
        Bordes.aplicar(this);

        etNombre = findViewById(R.id.etNombre);
        etUsuario = findViewById(R.id.etUsuarioRegistro);
        etClave = findViewById(R.id.etClaveRegistro);
        etClave2 = findViewById(R.id.etClaveRegistro2);
        cbConsentimiento = findViewById(R.id.cbConsentimiento);
        rgGenero = findViewById(R.id.rgGenero);
        tvError = findViewById(R.id.tvErrorRegistro);
        btnContinuar = findViewById(R.id.btnContinuar);

        btnContinuar.setOnClickListener(v -> registrar());
    }

    private void registrar() {
        final String nombre = textoDe(etNombre).trim();
        final String usuario = textoDe(etUsuario).trim();
        final String clave = textoDe(etClave);
        final String clave2 = textoDe(etClave2);

        String error = validar(nombre, usuario, clave, clave2);
        if (error != null) {
            tvError.setText(error);
            return;
        }
        if (UserRepository.obtener(this).buscar(usuario) != null) {
            tvError.setText("Ese usuario ya existe, elige otro");
            return;
        }
        tvError.setText("");
        final Genero genero = rgGenero.getCheckedRadioButtonId() == R.id.rbHombre ? Genero.HOMBRE : Genero.MUJER;
        btnContinuar.setEnabled(false);

        hilo.execute(() -> {
            String hash = PasswordHasher.hash(clave);
            Usuario nuevo = new Usuario(nombre, usuario, hash, System.currentTimeMillis(), new LockoutPolicy());
            nuevo.setGenero(genero);
            try {
                UserRepository.obtener(this).agregar(nuevo);
                runOnUiThread(() -> {
                    Sesion.iniciar(nuevo);
                    startActivity(new Intent(this, EnrollActivity.class));
                    finish();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    btnContinuar.setEnabled(true);
                    tvError.setText("No se pudo guardar el registro: " + e.getMessage());
                });
            }
        });
    }

    private String validar(String nombre, String usuario, String clave, String clave2) {
        if (nombre.length() < 3) {
            return "Escribe tu nombre completo";
        }
        if (!PATRON_USUARIO.matcher(usuario).matches()) {
            return "El usuario debe tener de 3 a 20 caracteres: letras, números, punto o guion bajo";
        }
        if (clave.length() < 8) {
            return "La contraseña debe tener al menos 8 caracteres";
        }
        if (!clave.matches(".*[A-Za-z].*") || !clave.matches(".*[0-9].*")) {
            return "La contraseña debe combinar letras y números";
        }
        if (!clave.equals(clave2)) {
            return "Las contraseñas no coinciden";
        }
        if (rgGenero.getCheckedRadioButtonId() == -1) {
            return "Elige Mujer u Hombre: define las fotos de referencia de la captura";
        }
        if (!cbConsentimiento.isChecked()) {
            return "Debes aceptar el consentimiento informado para continuar";
        }
        return null;
    }

    private static String textoDe(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hilo.shutdown();
    }
}
