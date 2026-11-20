package com.example.mistock.data;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Pago parcial (o total) recibido por una venta, con su fecha. */
@Entity(
        tableName = "abonos",
        foreignKeys = @ForeignKey(
                entity = Venta.class,
                parentColumns = "id",
                childColumns = "ventaId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("ventaId")})
public class Abono {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long ventaId;

    public long fecha;

    public long monto;

    @Nullable
    public String nota;
}
