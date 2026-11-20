package com.example.mistock.viewmodel;

import androidx.lifecycle.MutableLiveData;

import com.example.mistock.data.Producto;

import java.util.ArrayList;
import java.util.List;

/** Productos que se están agregando a una venta nueva (todavía no se guardan en la base de datos). */
public class CarritoVenta {

    public static class Linea {
        public long productoId;
        public String nombre;
        public int stockDisponible;
        public int cantidad;
        public long precioUnitario;

        public long subtotal() {
            return precioUnitario * cantidad;
        }
    }

    private final List<Linea> lineas = new ArrayList<>();
    public final MutableLiveData<List<Linea>> lineasLive =
            new MutableLiveData<List<Linea>>(new ArrayList<Linea>());

    /** Cliente elegido (null = venta directa, sin cliente). */
    public Long clienteId;

    public List<Linea> lineas() {
        return lineas;
    }

    public void limpiar() {
        lineas.clear();
        clienteId = null;
        publicar();
    }

    /** Agrega una unidad del producto. Devuelve false si no queda stock suficiente. */
    public boolean agregar(Producto producto) {
        for (Linea l : lineas) {
            if (l.productoId == producto.id) {
                if (l.cantidad < l.stockDisponible) {
                    l.cantidad++;
                    publicar();
                    return true;
                }
                return false;
            }
        }
        if (producto.stock <= 0) {
            return false;
        }
        Linea nueva = new Linea();
        nueva.productoId = producto.id;
        nueva.nombre = producto.nombre;
        nueva.stockDisponible = producto.stock;
        nueva.cantidad = 1;
        nueva.precioUnitario = producto.precioVenta;
        lineas.add(nueva);
        publicar();
        return true;
    }

    public void actualizar(Linea linea, int cantidad, long precioUnitario) {
        linea.cantidad = cantidad;
        linea.precioUnitario = precioUnitario;
        publicar();
    }

    public void quitar(Linea linea) {
        lineas.remove(linea);
        publicar();
    }

    public long total() {
        long suma = 0L;
        for (Linea l : lineas) {
            suma += l.subtotal();
        }
        return suma;
    }

    private void publicar() {
        lineasLive.setValue(new ArrayList<Linea>(lineas));
    }
}
