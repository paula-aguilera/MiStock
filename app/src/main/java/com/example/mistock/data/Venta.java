package com.example.mistock.data;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Una venta (compra de un cliente). Saldo pendiente = total - abonado.
 * Si se elimina el cliente, la venta se conserva (clienteId pasa a null).
 */
@Entity(
        tableName = "ventas",
        foreignKeys = @ForeignKey(
                entity = Cliente.class,
                parentColumns = "id",
                childColumns = "clienteId",
                onDelete = ForeignKey.SET_NULL),
        indices = {@Index("clienteId"), @Index("fecha")})
public class Venta {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @Nullable
    public Long clienteId;

    /** Fecha y hora real de la venta (milisegundos). */
    public long fecha;

    /** Suma de precio de venta x cantidad de todos los productos. */
    public long total;

    /** Suma de precio real (costo) x cantidad, congelado al momento de vender. */
    public long costoTotal;

    /** Suma de todos los abonos recibidos (incluye el pago al contado). */
    public long abonado;

    /** Bloc de notas de la compra. */
    @Nullable
    public String nota;

    public long saldo() {
        return total - abonado;
    }
}
