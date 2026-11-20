package com.example.mistock.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ProductoDao {

    @Insert
    long insertar(Producto producto);

    @Update
    int actualizar(Producto producto);

    @Delete
    int eliminar(Producto producto);

    @Query("SELECT * FROM productos ORDER BY nombre COLLATE NOCASE ASC")
    LiveData<List<Producto>> observarTodos();

    @Query("SELECT * FROM productos WHERE id = :id")
    LiveData<Producto> observarPorId(long id);

    @Query("SELECT * FROM productos WHERE id = :id")
    Producto obtenerPorId(long id);

    @Query("SELECT * FROM productos WHERE codigoBarras = :codigo LIMIT 1")
    Producto buscarPorCodigo(String codigo);

    // ---- Alertas ----

    @Query("SELECT * FROM productos WHERE stock <= :umbral ORDER BY stock ASC, nombre COLLATE NOCASE ASC")
    LiveData<List<Producto>> observarStockBajo(int umbral);

    @Query("SELECT * FROM productos WHERE stock <= :umbral ORDER BY stock ASC, nombre COLLATE NOCASE ASC")
    List<Producto> obtenerStockBajo(int umbral);

    @Query("SELECT * FROM productos WHERE stock > 0 AND fechaVencimiento IS NOT NULL AND fechaVencimiento <= :limite ORDER BY fechaVencimiento ASC")
    LiveData<List<Producto>> observarPorVencer(long limite);

    @Query("SELECT * FROM productos WHERE stock > 0 AND fechaVencimiento IS NOT NULL AND fechaVencimiento <= :limite ORDER BY fechaVencimiento ASC")
    List<Producto> obtenerPorVencer(long limite);

    // ---- Movimientos de stock ----

    @Query("UPDATE productos SET stock = stock - :cantidad WHERE id = :id")
    int descontarStock(long id, int cantidad);

    @Query("UPDATE productos SET stock = stock + :cantidad WHERE id = :id")
    int sumarStock(long id, int cantidad);

    // ---- Sugerencias para el formulario ----

    @Query("SELECT DISTINCT linea FROM productos WHERE linea IS NOT NULL AND linea <> '' ORDER BY linea COLLATE NOCASE ASC")
    LiveData<List<String>> observarLineas();

    @Query("SELECT DISTINCT marca FROM productos WHERE marca IS NOT NULL AND marca <> '' ORDER BY marca COLLATE NOCASE ASC")
    LiveData<List<String>> observarMarcas();

    // ---- Resumen del inventario ----

    @Query("SELECT COUNT(*) FROM productos")
    LiveData<Integer> observarCantidad();

    @Query("SELECT COALESCE(SUM(stock * precioReal), 0) FROM productos")
    LiveData<Long> observarValorCosto();

    @Query("SELECT COALESCE(SUM(stock * precioVenta), 0) FROM productos")
    LiveData<Long> observarValorVenta();
}
