package com.laura.verificacion;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Las versiones recientes de Android dibujan la app debajo de la barra de estado y de navegación.
 * Esto agrega el relleno necesario (y deja espacio al teclado) para que nada quede tapado.
 */
public final class Bordes {

    private Bordes() {
    }

    public static void aplicar(Activity actividad) {
        View raiz = actividad.findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (vista, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            vista.setPadding(b.left, b.top, b.right, b.bottom);
            return insets;
        });
    }
}
