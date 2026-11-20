package com.example.mistock.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface VentaDao {

    // ---- Escritura ----

    @Insert
    long insertarVenta(Venta venta);

    @Insert
    void insertarItems(List<VentaItem> items);

    @Insert
    long insertarAbono(Abono abono);

    @Update
    int actualizarVenta(Venta venta);

    @Delete
    int eliminarVenta(Venta venta);

    // ---- Lectura puntual (se usa dentro de transacciones) ----

    @Query("SELECT * FROM ventas WHERE id = :id")
    Venta obtenerVenta(long id);

    @Query("SELECT * FROM venta_items WHERE ventaId = :ventaId ORDER BY id ASC")
    List<VentaItem> obtenerItems(long ventaId);

    @Query("SELECT COALESCE(SUM(total - abonado), 0) FROM ventas WHERE clienteId = :clienteId")
    long saldoDeCliente(long clienteId);

    // ---- Lectura observable ----

    @Query("SELECT v.*, c.nombre AS clienteNombre FROM ventas v "
            + "LEFT JOIN clientes c ON c.id = v.clienteId WHERE v.id = :id")
    LiveData<VentaConCliente> observarVenta(long id);

    @Query("SELECT * FROM venta_items WHERE ventaId = :ventaId ORDER BY id ASC")
    LiveData<List<VentaItem>> observarItems(long ventaId);

    @Query("SELECT * FROM abonos WHERE ventaId = :ventaId ORDER BY fecha ASC, id ASC")
    LiveData<List<Abono>> observarAbonos(long ventaId);

    @Query("SELECT v.*, c.nombre AS clienteNombre FROM ventas v "
            + "LEFT JOIN clientes c ON c.id = v.clienteId "
            + "WHERE v.fecha BETWEEN :desde AND :hasta ORDER BY v.fecha DESC")
    LiveData<List<VentaConCliente>> observarVentasEntre(long desde, long hasta);

    @Query("SELECT v.*, c.nombre AS clienteNombre FROM ventas v "
            + "LEFT JOIN clientes c ON c.id = v.clienteId "
            + "WHERE v.clienteId = :clienteId ORDER BY v.fecha DESC")
    LiveData<List<VentaConCliente>> observarVentasDeCliente(long clienteId);

    @Query("SELECT COALESCE(SUM(total - abonado), 0) FROM ventas WHERE clienteId = :clienteId")
    LiveData<Long> observarSaldoCliente(long clienteId);

    @Query("SELECT COALESCE(SUM(total - abonado), 0) FROM ventas")
    LiveData<Long> observarTotalPorCobrar();

    // ---- Reportes ----

    @Query("SELECT COALESCE(SUM(total), 0) AS totalVentas, "
            + "COALESCE(SUM(costoTotal), 0) AS totalCosto, "
            + "COALESCE(SUM(abonado), 0) AS totalCobrado, "
            + "COUNT(id) AS numVentas, "
            + "(SELECT COALESCE(SUM(i.cantidad), 0) FROM venta_items i "
            + "INNER JOIN ventas v2 ON v2.id = i.ventaId "
            + "WHERE v2.fecha BETWEEN :desde AND :hasta) AS unidades "
            + "FROM ventas WHERE fecha BETWEEN :desde AND :hasta")
    LiveData<ResumenVentas> observarResumen(long desde, long hasta);

    @Query("SELECT i.nombre AS nombre, SUM(i.cantidad) AS unidades, "
            + "SUM((i.precioVenta - i.precioCosto) * i.cantidad) AS ganancia "
            + "FROM venta_items i INNER JOIN ventas v ON v.id = i.ventaId "
            + "WHERE v.fecha BETWEEN :desde AND :hasta "
            + "GROUP BY i.nombre ORDER BY unidades DESC, ganancia DESC LIMIT 5")
    LiveData<List<TopProducto>> observarMasVendidos(long desde, long hasta);
}
