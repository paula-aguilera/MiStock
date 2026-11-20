package com.example.mistock.data;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.example.mistock.notifications.NotificationHelper;
import com.example.mistock.util.AppConfig;
import com.example.mistock.util.Dinero;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Única puerta de entrada a los datos. Aquí viven las reglas del negocio:
 * descontar stock al vender, registrar abonos, anular ventas, etc.
 * Todas las escrituras corren en un hilo de fondo y avisan el resultado en el hilo principal.
 */
public class StockRepository {

    private static volatile StockRepository instancia;

    public static StockRepository obtener(Context contexto) {
        if (instancia == null) {
            synchronized (StockRepository.class) {
                if (instancia == null) {
                    instancia = new StockRepository(contexto.getApplicationContext());
                }
            }
        }
        return instancia;
    }

    /** Datos mínimos de una línea de venta que envía la pantalla. */
    public static class LineaVenta {
        public final long productoId;
        public final int cantidad;
        public final long precioUnitario;

        public LineaVenta(long productoId, int cantidad, long precioUnitario) {
            this.productoId = productoId;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
        }
    }

    private final Context contexto;
    private final AppDatabase db;
    private final ProductoDao productoDao;
    private final ClienteDao clienteDao;
    private final VentaDao ventaDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler principal = new Handler(Looper.getMainLooper());

    private StockRepository(Context contexto) {
        this.contexto = contexto;
        this.db = AppDatabase.obtener(contexto);
        this.productoDao = db.productoDao();
        this.clienteDao = db.clienteDao();
        this.ventaDao = db.ventaDao();
    }

    private <T> void ejecutar(Callable<T> tarea, Callback<T> callback) {
        io.execute(() -> {
            Resultado<T> resultado;
            try {
                resultado = Resultado.ok(tarea.call());
            } catch (ReglaNegocioException e) {
                resultado = Resultado.error(e.getMessage());
            } catch (Exception e) {
                resultado = Resultado.error("No se pudo completar la operación (" + e.getMessage() + ")");
            }
            if (callback != null) {
                final Resultado<T> finalResultado = resultado;
                principal.post(() -> callback.alTerminar(finalResultado));
            }
        });
    }

    // =====================================================================
    // PRODUCTOS
    // =====================================================================

    public LiveData<List<Producto>> productos() {
        return productoDao.observarTodos();
    }

    public LiveData<Producto> producto(long id) {
        return productoDao.observarPorId(id);
    }

    public void obtenerProducto(long id, Callback<Producto> callback) {
        ejecutar(() -> productoDao.obtenerPorId(id), callback);
    }

    /** Busca un producto por su código de barras (el resultado trae null si no existe). */
    public void buscarPorCodigo(String codigo, Callback<Producto> callback) {
        ejecutar(() -> productoDao.buscarPorCodigo(codigo == null ? "" : codigo.trim()), callback);
    }

    public LiveData<List<String>> lineas() {
        return productoDao.observarLineas();
    }

    public LiveData<List<String>> marcas() {
        return productoDao.observarMarcas();
    }

    public LiveData<List<Producto>> stockBajo() {
        return productoDao.observarStockBajo(AppConfig.UMBRAL_STOCK_BAJO);
    }

    public LiveData<List<Producto>> porVencer(long limiteUtc) {
        return productoDao.observarPorVencer(limiteUtc);
    }

    public LiveData<Integer> cantidadProductos() {
        return productoDao.observarCantidad();
    }

    public LiveData<Long> valorInventarioCosto() {
        return productoDao.observarValorCosto();
    }

    public LiveData<Long> valorInventarioVenta() {
        return productoDao.observarValorVenta();
    }

    public void guardarProducto(Producto producto, Callback<Long> callback) {
        ejecutar(() -> {
            if (producto.nombre.trim().isEmpty()) {
                throw new ReglaNegocioException("El nombre del producto es obligatorio.");
            }
            if (producto.stock < 0) {
                throw new ReglaNegocioException("El stock no puede ser negativo.");
            }
            producto.nombre = producto.nombre.trim();
            if (producto.codigoBarras != null) {
                producto.codigoBarras = producto.codigoBarras.trim();
                if (producto.codigoBarras.isEmpty()) {
                    producto.codigoBarras = null;
                }
            }
            if (producto.codigoBarras != null) {
                Producto otro = productoDao.buscarPorCodigo(producto.codigoBarras);
                if (otro != null && otro.id != producto.id) {
                    throw new ReglaNegocioException("Ese código de barras ya pertenece a «" + otro.nombre + "».");
                }
            }
            if (producto.id == 0L) {
                producto.creadoEn = System.currentTimeMillis();
                producto.id = productoDao.insertar(producto);
            } else {
                productoDao.actualizar(producto);
            }
            return producto.id;
        }, callback);
    }

    public void eliminarProducto(Producto producto, Callback<Boolean> callback) {
        ejecutar(() -> {
            productoDao.eliminar(producto);
            return true;
        }, callback);
    }

    /** Suma unidades a un producto que ya existe (compra de reposición). Devuelve el stock nuevo. */
    public void reponerStock(long productoId, int cantidad, Callback<Integer> callback) {
        ejecutar(() -> {
            if (cantidad <= 0) {
                throw new ReglaNegocioException("La cantidad debe ser mayor a cero.");
            }
            productoDao.sumarStock(productoId, cantidad);
            Producto p = productoDao.obtenerPorId(productoId);
            return p == null ? 0 : p.stock;
        }, callback);
    }

    // =====================================================================
    // CLIENTES
    // =====================================================================

    public LiveData<List<Cliente>> clientes() {
        return clienteDao.observarTodos();
    }

    public LiveData<List<ClienteConSaldo>> clientesConSaldo() {
        return clienteDao.observarConSaldo();
    }

    public LiveData<Cliente> cliente(long id) {
        return clienteDao.observarPorId(id);
    }

    public LiveData<Long> saldoCliente(long clienteId) {
        return ventaDao.observarSaldoCliente(clienteId);
    }

    public void guardarCliente(Cliente cliente, Callback<Long> callback) {
        ejecutar(() -> {
            if (cliente.nombre.trim().isEmpty()) {
                throw new ReglaNegocioException("El nombre del cliente es obligatorio.");
            }
            cliente.nombre = cliente.nombre.trim();
            if (cliente.id == 0L) {
                cliente.creadoEn = System.currentTimeMillis();
                cliente.id = clienteDao.insertar(cliente);
            } else {
                clienteDao.actualizar(cliente);
            }
            return cliente.id;
        }, callback);
    }

    public void eliminarCliente(Cliente cliente, Callback<Boolean> callback) {
        ejecutar(() -> {
            long saldo = ventaDao.saldoDeCliente(cliente.id);
            if (saldo > 0) {
                throw new ReglaNegocioException("No se puede eliminar: " + cliente.nombre
                        + " aún debe " + Dinero.formato(saldo) + ".");
            }
            clienteDao.eliminar(cliente);
            return true;
        }, callback);
    }

    // =====================================================================
    // VENTAS Y ABONOS
    // =====================================================================

    public LiveData<VentaConCliente> venta(long id) {
        return ventaDao.observarVenta(id);
    }

    public LiveData<List<VentaItem>> itemsVenta(long ventaId) {
        return ventaDao.observarItems(ventaId);
    }

    public LiveData<List<Abono>> abonosVenta(long ventaId) {
        return ventaDao.observarAbonos(ventaId);
    }

    public LiveData<List<VentaConCliente>> ventasDeCliente(long clienteId) {
        return ventaDao.observarVentasDeCliente(clienteId);
    }

    public LiveData<List<VentaConCliente>> ventasEntre(long desde, long hasta) {
        return ventaDao.observarVentasEntre(desde, hasta);
    }

    public LiveData<ResumenVentas> resumen(long desde, long hasta) {
        return ventaDao.observarResumen(desde, hasta);
    }

    public LiveData<List<TopProducto>> masVendidos(long desde, long hasta) {
        return ventaDao.observarMasVendidos(desde, hasta);
    }

    public LiveData<Long> totalPorCobrar() {
        return ventaDao.observarTotalPorCobrar();
    }

    /**
     * Registra una venta completa en UNA sola transacción: crea la venta y sus líneas, descuenta el stock
     * y guarda el pago inicial. Si algo falla, no se guarda nada.
     *
     * @param abonoInicial lo que paga ahora; igual al total = pago al contado, menor = queda saldo pendiente
     * @return el id de la venta creada
     */
    public void registrarVenta(final Long clienteId, final List<LineaVenta> lineas, final long abonoInicial,
                               final String nota, Callback<Long> callback) {
        ejecutar(() -> {
            final List<Producto> conPocoStock = new ArrayList<>();

            Callable<Long> transaccion = () -> {
                if (lineas == null || lineas.isEmpty()) {
                    throw new ReglaNegocioException("Agrega al menos un producto a la venta.");
                }

                long total = 0L;
                long costo = 0L;
                List<VentaItem> items = new ArrayList<>();

                for (LineaVenta linea : lineas) {
                    Producto p = productoDao.obtenerPorId(linea.productoId);
                    if (p == null) {
                        throw new ReglaNegocioException("Uno de los productos ya no existe en el inventario.");
                    }
                    if (linea.cantidad <= 0) {
                        throw new ReglaNegocioException("La cantidad de " + p.nombre + " debe ser mayor a cero.");
                    }
                    if (linea.precioUnitario < 0) {
                        throw new ReglaNegocioException("El precio de " + p.nombre + " no es válido.");
                    }
                    if (linea.cantidad > p.stock) {
                        throw new ReglaNegocioException("No hay stock suficiente de " + p.nombre
                                + " (disponible: " + p.stock + ").");
                    }
                    VentaItem item = new VentaItem();
                    item.productoId = p.id;
                    item.nombre = p.nombre;
                    item.cantidad = linea.cantidad;
                    item.precioVenta = linea.precioUnitario;
                    item.precioCosto = p.precioReal;
                    items.add(item);

                    total += linea.precioUnitario * linea.cantidad;
                    costo += p.precioReal * linea.cantidad;
                }

                if (abonoInicial < 0 || abonoInicial > total) {
                    throw new ReglaNegocioException("El abono inicial no puede ser mayor al total ("
                            + Dinero.formato(total) + ").");
                }
                if (abonoInicial < total && clienteId == null) {
                    throw new ReglaNegocioException("Para dejar saldo pendiente debes elegir un cliente.");
                }

                long ahora = System.currentTimeMillis();
                Venta venta = new Venta();
                venta.clienteId = clienteId;
                venta.fecha = ahora;
                venta.total = total;
                venta.costoTotal = costo;
                venta.abonado = abonoInicial;
                venta.nota = (nota == null || nota.trim().isEmpty()) ? null : nota.trim();
                long ventaId = ventaDao.insertarVenta(venta);

                for (VentaItem item : items) {
                    item.ventaId = ventaId;
                }
                ventaDao.insertarItems(items);

                // Descuenta el stock y comprueba que no quedó negativo.
                for (VentaItem item : items) {
                    long productoId = item.productoId;
                    productoDao.descontarStock(productoId, item.cantidad);
                    Producto despues = productoDao.obtenerPorId(productoId);
                    if (despues == null || despues.stock < 0) {
                        throw new ReglaNegocioException("No hay stock suficiente de " + item.nombre + ".");
                    }
                    if (despues.stock <= AppConfig.UMBRAL_STOCK_BAJO) {
                        conPocoStock.add(despues);
                    }
                }

                if (abonoInicial > 0) {
                    Abono abono = new Abono();
                    abono.ventaId = ventaId;
                    abono.fecha = ahora;
                    abono.monto = abonoInicial;
                    abono.nota = (abonoInicial == total) ? "Pago al contado" : "Abono inicial";
                    ventaDao.insertarAbono(abono);
                }
                return ventaId;
            };

            long id = db.runInTransaction(transaccion);

            // Alerta inmediata: "te quedan 2 unidades, ¡hora de comprar!"
            for (Producto p : conPocoStock) {
                NotificationHelper.avisarStockBajo(contexto, p);
            }
            return id;
        }, callback);
    }

    /**
     * Registra un abono y lo descuenta del saldo pendiente. Devuelve el saldo que queda
     * (0 = la venta quedó totalmente pagada y finalizada).
     */
    public void registrarAbono(final long ventaId, final long monto, final String nota, Callback<Long> callback) {
        ejecutar(() -> {
            Callable<Long> transaccion = () -> {
                Venta venta = ventaDao.obtenerVenta(ventaId);
                if (venta == null) {
                    throw new ReglaNegocioException("La venta ya no existe.");
                }
                long saldo = venta.total - venta.abonado;
                if (saldo <= 0) {
                    throw new ReglaNegocioException("Esta venta ya está pagada por completo.");
                }
                if (monto <= 0) {
                    throw new ReglaNegocioException("Ingresa un monto mayor a cero.");
                }
                if (monto > saldo) {
                    throw new ReglaNegocioException("El abono (" + Dinero.formato(monto)
                            + ") supera el saldo pendiente (" + Dinero.formato(saldo) + ").");
                }
                Abono abono = new Abono();
                abono.ventaId = ventaId;
                abono.fecha = System.currentTimeMillis();
                abono.monto = monto;
                abono.nota = (nota == null || nota.trim().isEmpty()) ? null : nota.trim();
                ventaDao.insertarAbono(abono);

                venta.abonado += monto;
                ventaDao.actualizarVenta(venta);
                return venta.total - venta.abonado;
            };
            return db.runInTransaction(transaccion);
        }, callback);
    }

    /** Anula una venta (por error de registro): devuelve las unidades al stock y borra la venta y sus abonos. */
    public void anularVenta(final long ventaId, Callback<Boolean> callback) {
        ejecutar(() -> {
            Callable<Boolean> transaccion = () -> {
                Venta venta = ventaDao.obtenerVenta(ventaId);
                if (venta == null) {
                    throw new ReglaNegocioException("La venta ya no existe.");
                }
                for (VentaItem item : ventaDao.obtenerItems(ventaId)) {
                    if (item.productoId != null) {
                        productoDao.sumarStock(item.productoId, item.cantidad);
                    }
                }
                ventaDao.eliminarVenta(venta);
                return true;
            };
            return db.runInTransaction(transaccion);
        }, callback);
    }

    // =====================================================================
    // RESPALDO
    // =====================================================================

    public void exportarRespaldo(Uri destino, Callback<Boolean> callback) {
        ejecutar(() -> {
            RespaldoManager.exportar(contexto, destino);
            return true;
        }, callback);
    }

    public void restaurarRespaldo(Uri origen, Callback<Boolean> callback) {
        ejecutar(() -> {
            RespaldoManager.restaurar(contexto, origen);
            return true;
        }, callback);
    }
}
