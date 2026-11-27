package com.laura.verificacion;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Guarda contraseñas como hash PBKDF2 con sal aleatoria; la contraseña nunca se almacena.
 * Formato guardado: algoritmo$iteraciones$salHex$hashHex
 */
public final class PasswordHasher {

    private static final String[] ALGORITMOS = {"PBKDF2WithHmacSHA256", "PBKDF2WithHmacSHA1"};
    private static final int ITERACIONES = 120_000;
    private static final int BYTES_SAL = 16;
    private static final int BITS_HASH = 256;

    private PasswordHasher() {
    }

    public static String hash(String clave) {
        byte[] sal = new byte[BYTES_SAL];
        new SecureRandom().nextBytes(sal);
        for (String algoritmo : ALGORITMOS) {
            try {
                byte[] h = derivar(algoritmo, clave, sal, ITERACIONES, BITS_HASH);
                return algoritmo + "$" + ITERACIONES + "$" + aHex(sal) + "$" + aHex(h);
            } catch (NoSuchAlgorithmException e) {
                // se prueba el siguiente algoritmo (teléfonos antiguos)
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("No hay algoritmo PBKDF2 disponible");
    }

    public static boolean verificar(String clave, String guardado) {
        if (clave == null || guardado == null) {
            return false;
        }
        String[] partes = guardado.split("\\$");
        if (partes.length != 4) {
            return false;
        }
        try {
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] sal = deHex(partes[2]);
            byte[] esperado = deHex(partes[3]);
            byte[] calculado = derivar(partes[0], clave, sal, iteraciones, esperado.length * 8);
            return MessageDigest.isEqual(esperado, calculado); // comparación en tiempo constante
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] derivar(String algoritmo, String clave, byte[] sal, int iteraciones, int bits)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(clave.toCharArray(), sal, iteraciones, bits);
        try {
            return SecretKeyFactory.getInstance(algoritmo).generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private static String aHex(byte[] datos) {
        StringBuilder sb = new StringBuilder(datos.length * 2);
        for (byte b : datos) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] deHex(String hex) {
        byte[] r = new byte[hex.length() / 2];
        for (int i = 0; i < r.length; i++) {
            r[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return r;
    }
}
