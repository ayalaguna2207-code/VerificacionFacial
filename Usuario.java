package com.laura.verificacion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Un usuario registrado: datos, hash de la contraseña, consentimiento y sus 3 embeddings de referencia. */
public class Usuario {

    public static final int EMBEDDINGS_REQUERIDOS = 3;

    private final String nombre;
    private final String usuario;
    private final String hashClave;
    private final long consentimientoMs;
    private final LockoutPolicy bloqueo;
    private final List<float[]> embeddings = new ArrayList<>();
    private Genero genero = Genero.MUJER;

    public Usuario(String nombre, String usuario, String hashClave, long consentimientoMs, LockoutPolicy bloqueo) {
        this.nombre = nombre;
        this.usuario = usuario;
        this.hashClave = hashClave;
        this.consentimientoMs = consentimientoMs;
        this.bloqueo = bloqueo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getHashClave() {
        return hashClave;
    }

    public long getConsentimientoMs() {
        return consentimientoMs;
    }

    public LockoutPolicy getBloqueo() {
        return bloqueo;
    }

    public Genero getGenero() {
        return genero;
    }

    public void setGenero(Genero genero) {
        this.genero = genero;
    }

    public List<float[]> getEmbeddings() {
        return new ArrayList<>(embeddings);
    }

    public void setEmbeddings(List<float[]> nuevos) {
        embeddings.clear();
        embeddings.addAll(nuevos);
    }

    public boolean tieneEnrolamiento() {
        return embeddings.size() >= EMBEDDINGS_REQUERIDOS;
    }

    JSONObject aJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("nombre", nombre);
        o.put("usuario", usuario);
        o.put("hash", hashClave);
        o.put("consentimiento", consentimientoMs);
        o.put("genero", genero.name());
        o.put("fallos", bloqueo.getFallos());
        o.put("bloqueadoHasta", bloqueo.getBloqueadoHasta());
        JSONArray lista = new JSONArray();
        for (float[] e : embeddings) {
            JSONArray v = new JSONArray();
            for (float x : e) {
                v.put((double) x);
            }
            lista.put(v);
        }
        o.put("embeddings", lista);
        return o;
    }

    static Usuario deJson(JSONObject o) throws JSONException {
        LockoutPolicy politica = new LockoutPolicy(o.optInt("fallos", 0), o.optLong("bloqueadoHasta", 0L));
        Usuario u = new Usuario(o.getString("nombre"), o.getString("usuario"), o.getString("hash"),
                o.optLong("consentimiento", 0L), politica);
        JSONArray lista = o.optJSONArray("embeddings");
        List<float[]> cargados = new ArrayList<>();
        if (lista != null) {
            for (int i = 0; i < lista.length(); i++) {
                JSONArray v = lista.getJSONArray(i);
                float[] e = new float[v.length()];
                for (int j = 0; j < e.length; j++) {
                    e[j] = (float) v.getDouble(j);
                }
                cargados.add(e);
            }
        }
        u.setGenero(Genero.deTexto(o.optString("genero", "")));
        u.setEmbeddings(cargados);
        return u;
    }
}
