package com.example.mistock.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utilidades de fechas.
 *
 * Los vencimientos se guardan como "medianoche UTC" del día elegido (es lo que entrega el selector de
 * fechas de Material), así el día no se corre por la zona horaria. Las ventas y abonos se guardan con la
 * hora real del teléfono.
 */
public final class Fechas {

    public static final long DIA_MS = 24L * 60L * 60L * 1000L;

    public static final int SIN_FECHA = 0;
    public static final int VIGENTE = 1;
    public static final int POR_VENCER = 2;
    public static final int VENCIDO = 3;

    private static final Locale ES_CL = Locale.forLanguageTag("es-CL");
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private Fechas() {
    }

    /** Medianoche UTC del día de hoy (según la fecha local del teléfono). */
    public static long hoyUtc() {
        Calendar local = Calendar.getInstance();
        Calendar utc = Calendar.getInstance(UTC);
        utc.clear();
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
        return utc.getTimeInMillis();
    }

    /** Formatea un vencimiento guardado en UTC como dd/MM/yyyy. */
    public static String fechaUtc(long utcMillis) {
        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy", ES_CL);
        f.setTimeZone(UTC);
        return f.format(new Date(utcMillis));
    }

    public static String fecha(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy", ES_CL).format(new Date(millis));
    }

    public static String fechaHora(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", ES_CL).format(new Date(millis));
    }

    public static long inicioDia(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static long finDia(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(inicioDia(millis));
        c.add(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis() - 1L;
    }

    public static long inicioMes(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(inicioDia(millis));
        c.set(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
    }

    public static long finMes(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(inicioMes(millis));
        c.add(Calendar.MONTH, 1);
        return c.getTimeInMillis() - 1L;
    }

    /** Inicio del día de hace {@code dias} días (0 = hoy). */
    public static long inicioDiaHaceDias(int dias) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(inicioDia(System.currentTimeMillis()));
        c.add(Calendar.DAY_OF_MONTH, -dias);
        return c.getTimeInMillis();
    }

    /** Días que faltan para el vencimiento. Negativo = ya venció. */
    public static int diasHasta(long vencimientoUtc) {
        return (int) ((vencimientoUtc - hoyUtc()) / DIA_MS);
    }

    public static int estadoVencimiento(Long vencimientoUtc) {
        if (vencimientoUtc == null) {
            return SIN_FECHA;
        }
        int dias = diasHasta(vencimientoUtc);
        if (dias < 0) {
            return VENCIDO;
        }
        if (dias <= AppConfig.DIAS_AVISO_VENCIMIENTO) {
            return POR_VENCER;
        }
        return VIGENTE;
    }

    /** Texto corto para mostrar junto al producto: "Vence en 12 días", "Vencido hace 3 días". */
    public static String textoVencimiento(Long vencimientoUtc) {
        if (vencimientoUtc == null) {
            return "";
        }
        int dias = diasHasta(vencimientoUtc);
        if (dias < 0) {
            int n = -dias;
            return "Vencido hace " + n + (n == 1 ? " día" : " días");
        }
        if (dias == 0) {
            return "Vence hoy";
        }
        if (dias == 1) {
            return "Vence mañana";
        }
        if (dias <= AppConfig.DIAS_AVISO_VENCIMIENTO) {
            return "Vence en " + dias + " días";
        }
        return "Vence " + fechaUtc(vencimientoUtc);
    }
}
