package com.example.mistock.util;

/**
 * Parámetros de negocio de MiStock. Si algún día quieres cambiar cuándo avisa la app,
 * este es el único lugar que hay que modificar.
 */
public final class AppConfig {

    /** Se considera "stock bajo" cuando quedan esta cantidad de unidades o menos (RF-ALE-01). */
    public static final int UMBRAL_STOCK_BAJO = 2;

    /** Se avisa de un vencimiento cuando faltan estos días o menos (RF-ALE-02). */
    public static final int DIAS_AVISO_VENCIMIENTO = 30;

    /** Hora aproximada (0-23) a la que llega el resumen diario de alertas. */
    public static final int HORA_AVISO = 9;

    public static final String CANAL_ALERTAS = "alertas_stock";
    public static final String TRABAJO_ALERTAS_DIARIAS = "alertas_diarias";

    private AppConfig() {
    }
}
