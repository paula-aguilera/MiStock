package com.example.mistock.util;

import java.text.Normalizer;
import java.util.Locale;

public final class Texto {

    private Texto() {
    }

    /** Minúsculas y sin tildes, para que "crémé" coincida con "creme" al buscar. */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sinTildes.toLowerCase(Locale.ROOT).trim();
    }

    /** Devuelve null si el texto está vacío (para no guardar cadenas en blanco en la base de datos). */
    public static String vacioANulo(CharSequence texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.toString().trim();
        return limpio.isEmpty() ? null : limpio;
    }

    public static String inicial(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return "?";
        }
        return texto.trim().substring(0, 1).toUpperCase(Locale.ROOT);
    }
}
