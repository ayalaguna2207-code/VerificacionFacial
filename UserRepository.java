package com.laura.verificacion;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Todos los usuarios se guardan en UN archivo cifrado (datos del usuario y sus 3 embeddings).
 * Se carga en memoria al inicio y se reescribe completo en cada cambio.
 */
public class UserRepository {

    private static final String ARCHIVO = "datos_usuarios.enc";
    private static UserRepository instancia;

    private final SecureStorage almacen;
    private final List<Usuario> usuarios = new ArrayList<>();
    // Para que los intentos fallidos con un usuario inexistente se vean igual que con uno real.
    private final Map<String, LockoutPolicy> politicasDesconocidas = new HashMap<>();

    public static synchronized UserRepository obtener(Context contexto) {
        if (instancia == null) {
            instancia = new UserRepository(contexto.getApplicationContext());
        }
        return instancia;
    }

    private UserRepository(Context contexto) {
        almacen = new SecureStorage(contexto, ARCHIVO);
        cargar();
    }

    private void cargar() {
        try {
            byte[] datos = almacen.leer();
            if (datos == null) {
                return;
            }
            JSONObject raiz = new JSONObject(new String(datos, StandardCharsets.UTF_8));
            JSONArray lista = raiz.getJSONArray("usuarios");
            for (int i = 0; i < lista.length(); i++) {
                usuarios.add(Usuario.deJson(lista.getJSONObject(i)));
            }
        } catch (Exception e) {
            usuarios.clear(); // archivo ilegible: se empieza vacío
        }
    }

    public synchronized Usuario buscar(String nombreUsuario) {
        String clave = normalizar(nombreUsuario);
        for (Usuario u : usuarios) {
            if (normalizar(u.getUsuario()).equals(clave)) {
                return u;
            }
        }
        return null;
    }

    /** Política de bloqueo del usuario, exista o no, para no revelar qué usuarios existen. */
    public synchronized LockoutPolicy politicaDe(String nombreUsuario) {
        Usuario u = buscar(nombreUsuario);
        if (u != null) {
            return u.getBloqueo();
        }
        String clave = normalizar(nombreUsuario);
        LockoutPolicy p = politicasDesconocidas.get(clave);
        if (p == null) {
            p = new LockoutPolicy();
            politicasDesconocidas.put(clave, p);
        }
        return p;
    }

    public synchronized void agregar(Usuario nuevo) throws IOException {
        if (buscar(nuevo.getUsuario()) != null) {
            throw new IOException("Ese usuario ya existe");
        }
        usuarios.add(nuevo);
        guardar();
    }

    public synchronized void eliminar(Usuario u) throws IOException {
        usuarios.remove(u);
        guardar();
    }

    public synchronized void guardar() throws IOException {
        try {
            JSONArray lista = new JSONArray();
            for (Usuario u : usuarios) {
                lista.put(u.aJson());
            }
            JSONObject raiz = new JSONObject();
            raiz.put("version", 1);
            raiz.put("usuarios", lista);
            almacen.escribir(raiz.toString().getBytes(StandardCharsets.UTF_8));
        } catch (org.json.JSONException e) {
            throw new IOException("No se pudo guardar", e);
        }
    }

    private static String normalizar(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
