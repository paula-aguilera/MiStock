package com.example.mistock.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Producto del inventario. Todos los valores en pesos chilenos (sin decimales). */
@Entity(tableName = "productos")
public class Producto {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String nombre = "";

    @Nullable
    public String categoria;

    @Nullable
    public String linea;

    @Nullable
    public String marca;

    /** Lo que te costó a ti (precio real / de compra). */
    public long precioReal;

    /** Lo que cobras al cliente. */
    public long precioVenta;

    public int stock;

    /** Código de barras (EAN, etc.) para encontrar el producto con la cámara. Único entre productos. */
    @Nullable
    public String codigoBarras;

    /** Vencimiento como medianoche UTC (ver Fechas). Null = sin vencimiento. */
    @Nullable
    public Long fechaVencimiento;

    public long creadoEn;

    public long gananciaUnitaria() {
        return precioVenta - precioReal;
    }
}
