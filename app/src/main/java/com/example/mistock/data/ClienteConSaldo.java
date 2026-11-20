package com.example.mistock.data;

import androidx.room.Embedded;

/** Cliente con lo que debe actualmente y cuántas compras tiene. */
public class ClienteConSaldo {

    @Embedded
    public Cliente cliente;

    public long saldo;

    public int compras;
}
