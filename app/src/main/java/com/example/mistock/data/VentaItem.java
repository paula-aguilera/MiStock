package com.example.mistock.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Línea de una venta. Guarda una "foto" del nombre y del costo del producto al momento de vender,
 * así el historial y las ganancias no cambian aunque después edites o elimines el producto.
 */
@Entity(
        tableName = "venta_items",
        foreignKeys = @ForeignKey(
                entity = Venta.class,
                parentColumns = "id",
                childColumns = "ventaId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("ventaId")})
public class VentaItem {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long ventaId;

    /** Producto original (puede ser null si ya fue eliminado del inventario). */
    @Nullable
    public Long productoId;

    @NonNull
    public String nombre = "";

    public int cantidad;

    public long precioVenta;

    public long precioCosto;

    public long subtotal() {
        return precioVenta * cantidad;
    }
}
