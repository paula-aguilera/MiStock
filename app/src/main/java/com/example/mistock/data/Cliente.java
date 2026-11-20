package com.example.mistock.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "clientes")
public class Cliente {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String nombre = "";

    @Nullable
    public String telefono;

    @Nullable
    public String correo;

    /** "Bloc de notas" general del cliente. */
    @Nullable
    public String notas;

    public long creadoEn;
}
