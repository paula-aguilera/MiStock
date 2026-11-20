package com.example.mistock.data;

import androidx.annotation.Nullable;
import androidx.room.Embedded;

/** Venta junto al nombre de su cliente (resultado de un JOIN). */
public class VentaConCliente {

    @Embedded
    public Venta venta;

    @Nullable
    public String clienteNombre;
}
