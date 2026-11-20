package com.example.mistock.util;

import java.text.NumberFormat;
import java.util.Locale;

/** Formato y lectura de pesos chilenos (sin decimales). */
public final class Dinero {

    private static final Locale ES_CL = Locale.forLanguageTag("es-CL");

    private Dinero() {
    }

    /** 12990 -> "$12.990"; -500 -> "-$500". */
    public static String formato(long valor) {
        NumberFormat nf = NumberFormat.getIntegerInstance(ES_CL);
        String texto = nf.format(Math.abs(valor));
        return (valor < 0 ? "-$" : "$") + texto;
    }

    /** Convierte "12.990", "$ 12990" o "12990" en 12990. Devuelve 0 si está vacío o es inválido. */
    public static long parsear(CharSequence texto) {
        if (texto == null) {
            return 0L;
        }
        String limpio = texto.toString().replaceAll("[^0-9]", "");
        if (limpio.isEmpty() || limpio.length() > 12) {
            return 0L;
        }
        try {
            return Long.parseLong(limpio);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** Igual que {@link #parsear(CharSequence)} pero acotado a un entero razonable (unidades de stock). */
    public static int parsearEntero(CharSequence texto) {
        long valor = parsear(texto);
        return valor > 1_000_000L ? 1_000_000 : (int) valor;
    }
}
