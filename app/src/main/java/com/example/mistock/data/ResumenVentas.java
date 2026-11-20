package com.example.mistock.data;

/** Totales de un período para el reporte de ganancias. */
public class ResumenVentas {

    public long totalVentas;
    public long totalCosto;
    public long totalCobrado;
    public int numVentas;
    public long unidades;

    public long ganancia() {
        return totalVentas - totalCosto;
    }

    public long porCobrar() {
        return totalVentas - totalCobrado;
    }
}
