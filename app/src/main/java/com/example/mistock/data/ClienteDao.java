package com.example.mistock.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ClienteDao {

    @Insert
    long insertar(Cliente cliente);

    @Update
    int actualizar(Cliente cliente);

    @Delete
    int eliminar(Cliente cliente);

    @Query("SELECT * FROM clientes ORDER BY nombre COLLATE NOCASE ASC")
    LiveData<List<Cliente>> observarTodos();

    @Query("SELECT * FROM clientes WHERE id = :id")
    LiveData<Cliente> observarPorId(long id);

    @Query("SELECT * FROM clientes WHERE id = :id")
    Cliente obtenerPorId(long id);

    /** Clientes con lo que deben ahora (primero los que más deben). */
    @Query("SELECT c.*, COALESCE(SUM(v.total - v.abonado), 0) AS saldo, COUNT(v.id) AS compras "
            + "FROM clientes c LEFT JOIN ventas v ON v.clienteId = c.id "
            + "GROUP BY c.id "
            + "ORDER BY saldo DESC, c.nombre COLLATE NOCASE ASC")
    LiveData<List<ClienteConSaldo>> observarConSaldo();
}
