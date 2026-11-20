package com.example.mistock.util;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

/** Claves de los argumentos de navegación (deben coincidir con res/navigation/nav_graph.xml). */
public final class Args {

    public static final String PRODUCTO = "productoId";
    public static final String CLIENTE = "clienteId";
    public static final String VENTA = "ventaId";
    public static final String CODIGO = "codigo";

    private Args() {
    }

    public static Bundle de(String clave, long valor) {
        Bundle b = new Bundle();
        b.putLong(clave, valor);
        return b;
    }

    /** Texto opcional de los argumentos (null si no viene). */
    public static String leerTexto(Fragment fragment, String clave) {
        Bundle args = fragment.getArguments();
        return args == null ? null : args.getString(clave);
    }

    public static long leer(Fragment fragment, String clave) {
        Bundle args = fragment.getArguments();
        return args == null ? 0L : args.getLong(clave, 0L);
    }
}
