package com.laura.verificacion;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Guarda bytes cifrados con AES-256-GCM. La llave vive en Android Keystore:
 * no sale del teléfono y la app nunca la ve en texto.
 * Formato del archivo: [12 bytes de IV][datos cifrados + etiqueta de autenticación]
 */
public class SecureStorage {

    private static final String ALIAS = "verificacion_facial_llave_aes";
    private static final int BYTES_IV = 12;
    private static final int BITS_ETIQUETA = 128;

    private final File archivo;

    public SecureStorage(Context contexto, String nombreArchivo) {
        this.archivo = new File(contexto.getFilesDir(), nombreArchivo);
    }

    private SecretKey obtenerLlave() throws GeneralSecurityException, IOException {
        KeyStore almacen = KeyStore.getInstance("AndroidKeyStore");
        almacen.load(null);
        if (!almacen.containsAlias(ALIAS)) {
            KeyGenerator generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generador.init(new KeyGenParameterSpec.Builder(ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build());
            return generador.generateKey();
        }
        return (SecretKey) almacen.getKey(ALIAS, null);
    }

    public synchronized void escribir(byte[] datos) throws IOException {
        try {
            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(Cipher.ENCRYPT_MODE, obtenerLlave());
            byte[] iv = cifrador.getIV();
            byte[] cifrado = cifrador.doFinal(datos);

            File temporal = new File(archivo.getPath() + ".tmp");
            try (FileOutputStream out = new FileOutputStream(temporal)) {
                out.write(iv);
                out.write(cifrado);
                out.getFD().sync();
            }
            if (!temporal.renameTo(archivo)) {
                archivo.delete();
                if (!temporal.renameTo(archivo)) {
                    throw new IOException("No se pudo reemplazar el archivo de datos");
                }
            }
        } catch (GeneralSecurityException e) {
            throw new IOException("No se pudo cifrar", e);
        }
    }

    /** Devuelve null si todavía no existe el archivo. */
    public synchronized byte[] leer() throws IOException {
        if (!archivo.exists()) {
            return null;
        }
        byte[] todo = new byte[(int) archivo.length()];
        try (FileInputStream in = new FileInputStream(archivo)) {
            int leidos = 0;
            while (leidos < todo.length) {
                int n = in.read(todo, leidos, todo.length - leidos);
                if (n < 0) {
                    break;
                }
                leidos += n;
            }
        }
        if (todo.length <= BYTES_IV) {
            throw new IOException("Archivo de datos dañado");
        }
        try {
            byte[] iv = Arrays.copyOfRange(todo, 0, BYTES_IV);
            byte[] cifrado = Arrays.copyOfRange(todo, BYTES_IV, todo.length);
            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(Cipher.DECRYPT_MODE, obtenerLlave(), new GCMParameterSpec(BITS_ETIQUETA, iv));
            return cifrador.doFinal(cifrado);
        } catch (GeneralSecurityException e) {
            throw new IOException("No se pudo descifrar", e);
        }
    }

    public synchronized void borrar() {
        archivo.delete();
    }
}
