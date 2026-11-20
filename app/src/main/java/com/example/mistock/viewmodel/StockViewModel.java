package com.example.mistock.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.mistock.data.Abono;
import com.example.mistock.data.Callback;
import com.example.mistock.data.Cliente;
import com.example.mistock.data.ClienteConSaldo;
import com.example.mistock.data.Producto;
import com.example.mistock.data.ResumenVentas;
import com.example.mistock.data.StockRepository;
import com.example.mistock.data.TopProducto;
import com.example.mistock.data.VentaConCliente;
import com.example.mistock.data.VentaItem;
import com.example.mistock.util.AppConfig;
import com.example.mistock.util.Fechas;

import java.util.List;

/**
 * ViewModel compartido por toda la app (se obtiene con el Activity como dueño).
 * Reparte los datos a las pantallas y mantiene el carrito de la venta en curso.
 */
public class StockViewModel extends AndroidViewModel {

    private final StockRepository repo;

    /** Venta que se está armando en la pantalla "Nueva venta". */
    public final CarritoVenta carrito = new CarritoVenta();

    private final MediatorLiveData<Integer> totalAlertas = new MediatorLiveData<>();
    private int numStockBajo = 0;
    private int numPorVencer = 0;

    public StockViewModel(@NonNull Application application) {
        super(application);
        repo = StockRepository.obtener(application);

        totalAlertas.addSource(repo.stockBajo(), lista -> {
            numStockBajo = lista == null ? 0 : lista.size();
            totalAlertas.setValue(numStockBajo + numPorVencer);
        });
        totalAlertas.addSource(porVencer(), lista -> {
            numPorVencer = lista == null ? 0 : lista.size();
            totalAlertas.setValue(numStockBajo + numPorVencer);
        });
    }

    // ---- Productos ----

    public LiveData<List<Producto>> productos() {
        return repo.productos();
    }

    public LiveData<Producto> producto(long id) {
        return repo.producto(id);
    }

    public void obtenerProducto(long id, Callback<Producto> callback) {
        repo.obtenerProducto(id, callback);
    }

    public void buscarPorCodigo(String codigo, Callback<Producto> callback) {
        repo.buscarPorCodigo(codigo, callback);
    }

    public LiveData<List<String>> lineas() {
        return repo.lineas();
    }

    public LiveData<List<String>> marcas() {
        return repo.marcas();
    }

    public LiveData<List<Producto>> stockBajo() {
        return repo.stockBajo();
    }

    /** Productos que vencen dentro del plazo de aviso (o ya vencidos), calculado con la fecha de hoy. */
    public LiveData<List<Producto>> porVencer() {
        long limite = Fechas.hoyUtc() + AppConfig.DIAS_AVISO_VENCIMIENTO * Fechas.DIA_MS;
        return repo.porVencer(limite);
    }

    /** Suma de productos con stock bajo + productos por vencer (para la insignia de la barra inferior). */
    public LiveData<Integer> totalAlertas() {
        return totalAlertas;
    }

    public LiveData<Integer> cantidadProductos() {
        return repo.cantidadProductos();
    }

    public LiveData<Long> valorInventarioCosto() {
        return repo.valorInventarioCosto();
    }

    public LiveData<Long> valorInventarioVenta() {
        return repo.valorInventarioVenta();
    }

    public void guardarProducto(Producto producto, Callback<Long> callback) {
        repo.guardarProducto(producto, callback);
    }

    public void eliminarProducto(Producto producto, Callback<Boolean> callback) {
        repo.eliminarProducto(producto, callback);
    }

    public void reponerStock(long productoId, int cantidad, Callback<Integer> callback) {
        repo.reponerStock(productoId, cantidad, callback);
    }

    // ---- Clientes ----

    public LiveData<List<Cliente>> clientes() {
        return repo.clientes();
    }

    public LiveData<List<ClienteConSaldo>> clientesConSaldo() {
        return repo.clientesConSaldo();
    }

    public LiveData<Cliente> cliente(long id) {
        return repo.cliente(id);
    }

    public LiveData<Long> saldoCliente(long clienteId) {
        return repo.saldoCliente(clienteId);
    }

    public void guardarCliente(Cliente cliente, Callback<Long> callback) {
        repo.guardarCliente(cliente, callback);
    }

    public void eliminarCliente(Cliente cliente, Callback<Boolean> callback) {
        repo.eliminarCliente(cliente, callback);
    }

    // ---- Ventas y abonos ----

    public LiveData<VentaConCliente> venta(long id) {
        return repo.venta(id);
    }

    public LiveData<List<VentaItem>> itemsVenta(long ventaId) {
        return repo.itemsVenta(ventaId);
    }

    public LiveData<List<Abono>> abonosVenta(long ventaId) {
        return repo.abonosVenta(ventaId);
    }

    public LiveData<List<VentaConCliente>> ventasDeCliente(long clienteId) {
        return repo.ventasDeCliente(clienteId);
    }

    public LiveData<List<VentaConCliente>> ventasEntre(long desde, long hasta) {
        return repo.ventasEntre(desde, hasta);
    }

    public LiveData<ResumenVentas> resumen(long desde, long hasta) {
        return repo.resumen(desde, hasta);
    }

    public LiveData<List<TopProducto>> masVendidos(long desde, long hasta) {
        return repo.masVendidos(desde, hasta);
    }

    public LiveData<Long> totalPorCobrar() {
        return repo.totalPorCobrar();
    }

    public void registrarVenta(Long clienteId, List<StockRepository.LineaVenta> lineas, long abonoInicial,
                               String nota, Callback<Long> callback) {
        repo.registrarVenta(clienteId, lineas, abonoInicial, nota, callback);
    }

    public void registrarAbono(long ventaId, long monto, String nota, Callback<Long> callback) {
        repo.registrarAbono(ventaId, monto, nota, callback);
    }

    public void anularVenta(long ventaId, Callback<Boolean> callback) {
        repo.anularVenta(ventaId, callback);
    }

    // ---- Respaldo ----

    public void exportarRespaldo(Uri destino, Callback<Boolean> callback) {
        repo.exportarRespaldo(destino, callback);
    }

    public void restaurarRespaldo(Uri origen, Callback<Boolean> callback) {
        repo.restaurarRespaldo(origen, callback);
    }
}
