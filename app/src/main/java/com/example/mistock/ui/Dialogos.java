package com.example.mistock.ui;

import android.content.Context;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mistock.R;
import com.example.mistock.data.Cliente;
import com.example.mistock.data.Producto;
import com.example.mistock.databinding.DialogAbonoBinding;
import com.example.mistock.databinding.DialogCantidadBinding;
import com.example.mistock.databinding.DialogClienteBinding;
import com.example.mistock.databinding.DialogLineaCarritoBinding;
import com.example.mistock.databinding.DialogSeleccionarProductoBinding;
import com.example.mistock.util.Dinero;
import com.example.mistock.util.Texto;
import com.example.mistock.viewmodel.CarritoVenta;
import com.example.mistock.viewmodel.StockViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Cuadros de diálogo reutilizables. Los que tienen campos validan antes de cerrarse:
 * si algo está mal, el cuadro se queda abierto mostrando el error.
 */
public final class Dialogos {

    private Dialogos() {
    }

    private static void mostrarTexto(Context ctx, String mensaje) {
        Toast.makeText(ctx, mensaje, Toast.LENGTH_SHORT).show();
    }

    /** Pregunta "¿estás segura?" antes de una acción que no se puede deshacer. */
    public static void confirmar(Context ctx, String titulo, String mensaje, String textoAceptar, Runnable alAceptar) {
        new MaterialAlertDialogBuilder(ctx)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setNegativeButton(R.string.btn_cancelar, null)
                .setPositiveButton(textoAceptar, (dialogo, boton) -> alAceptar.run())
                .show();
    }

    private static void mostrarConValidacion(AlertDialog dialogo, Runnable alAceptar) {
        dialogo.setOnShowListener(x -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> alAceptar.run()));
        if (dialogo.getWindow() != null) {
            dialogo.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dialogo.show();
    }

    // =====================================================================
    // Reponer stock de un producto que ya existe
    // =====================================================================
    public static void reponerStock(Context ctx, StockViewModel vm, Producto producto) {
        DialogCantidadBinding d = DialogCantidadBinding.inflate(LayoutInflater.from(ctx));
        d.tvProducto.setText(ctx.getString(R.string.reponer_producto, producto.nombre, producto.stock));

        AlertDialog dialogo = new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.reponer_titulo)
                .setView(d.getRoot())
                .setNegativeButton(R.string.btn_cancelar, null)
                .setPositiveButton(R.string.btn_agregar_stock, null)
                .create();

        mostrarConValidacion(dialogo, () -> {
            int cantidad = Dinero.parsearEntero(d.etCantidad.getText());
            if (cantidad <= 0) {
                d.tilCantidad.setError(ctx.getString(R.string.err_cantidad));
                return;
            }
            vm.reponerStock(producto.id, cantidad, r -> {
                if (r.exito) {
                    mostrarTexto(ctx, ctx.getString(R.string.msg_stock_actualizado, r.valor));
                } else {
                    mostrarTexto(ctx, r.mensaje);
                }
            });
            dialogo.dismiss();
        });
    }

    // =====================================================================
    // Crear / editar cliente
    // =====================================================================
    public static void cliente(Context ctx, StockViewModel vm, Cliente existente, Consumer<Long> alGuardar) {
        DialogClienteBinding d = DialogClienteBinding.inflate(LayoutInflater.from(ctx));
        if (existente != null) {
            d.etNombre.setText(existente.nombre);
            d.etTelefono.setText(existente.telefono == null ? "" : existente.telefono);
            d.etCorreo.setText(existente.correo == null ? "" : existente.correo);
            d.etNotas.setText(existente.notas == null ? "" : existente.notas);
        }

        AlertDialog dialogo = new MaterialAlertDialogBuilder(ctx)
                .setTitle(existente == null ? R.string.cliente_nuevo : R.string.cliente_editar)
                .setView(d.getRoot())
                .setNegativeButton(R.string.btn_cancelar, null)
                .setPositiveButton(R.string.btn_guardar, null)
                .create();

        mostrarConValidacion(dialogo, () -> {
            String nombre = d.etNombre.getText() == null ? "" : d.etNombre.getText().toString().trim();
            String correo = Texto.vacioANulo(d.etCorreo.getText());
            boolean valido = true;

            if (nombre.isEmpty()) {
                d.tilNombre.setError(ctx.getString(R.string.err_obligatorio));
                valido = false;
            } else {
                d.tilNombre.setError(null);
            }
            if (correo != null && !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                d.tilCorreo.setError(ctx.getString(R.string.err_correo));
                valido = false;
            } else {
                d.tilCorreo.setError(null);
            }
            if (!valido) {
                return;
            }

            Cliente c = new Cliente();
            if (existente != null) {
                c.id = existente.id;
                c.creadoEn = existente.creadoEn;
            }
            c.nombre = nombre;
            c.telefono = Texto.vacioANulo(d.etTelefono.getText());
            c.correo = correo;
            c.notas = Texto.vacioANulo(d.etNotas.getText());

            vm.guardarCliente(c, r -> {
                if (r.exito) {
                    mostrarTexto(ctx, ctx.getString(R.string.msg_cliente_guardado));
                    if (alGuardar != null) {
                        alGuardar.accept(r.valor);
                    }
                } else {
                    mostrarTexto(ctx, r.mensaje);
                }
            });
            dialogo.dismiss();
        });
    }

    // =====================================================================
    // Registrar abono (descuenta del saldo pendiente)
    // =====================================================================
    public static void abono(Context ctx, StockViewModel vm, long ventaId, long saldo) {
        DialogAbonoBinding d = DialogAbonoBinding.inflate(LayoutInflater.from(ctx));
        d.tvSaldo.setText(ctx.getString(R.string.abono_saldo_actual, Dinero.formato(saldo)));
        d.btnPagarTodo.setOnClickListener(v -> d.etMonto.setText(String.valueOf(saldo)));

        AlertDialog dialogo = new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.abono_titulo)
                .setView(d.getRoot())
                .setNegativeButton(R.string.btn_cancelar, null)
                .setPositiveButton(R.string.btn_registrar_abono, null)
                .create();

        mostrarConValidacion(dialogo, () -> {
            long monto = Dinero.parsear(d.etMonto.getText());
            if (monto <= 0) {
                d.tilMonto.setError(ctx.getString(R.string.err_monto));
                return;
            }
            if (monto > saldo) {
                d.tilMonto.setError(ctx.getString(R.string.err_abono_mayor_saldo, Dinero.formato(saldo)));
                return;
            }
            String nota = Texto.vacioANulo(d.etNota.getText());
            vm.registrarAbono(ventaId, monto, nota, r -> {
                if (!r.exito) {
                    mostrarTexto(ctx, r.mensaje);
                } else if (r.valor != null && r.valor == 0L) {
                    mostrarTexto(ctx, ctx.getString(R.string.msg_venta_finalizada));
                } else {
                    mostrarTexto(ctx, ctx.getString(R.string.msg_abono_registrado, Dinero.formato(r.valor == null ? 0L : r.valor)));
                }
            });
            dialogo.dismiss();
        });
    }

    // =====================================================================
    // Editar cantidad y precio de una línea del carrito
    // =====================================================================
    public static void editarLinea(Context ctx, CarritoVenta carrito, CarritoVenta.Linea linea) {
        DialogLineaCarritoBinding d = DialogLineaCarritoBinding.inflate(LayoutInflater.from(ctx));
        d.tvProducto.setText(linea.nombre);
        d.tvDisponible.setText(ctx.getString(R.string.linea_disponible, linea.stockDisponible));
        d.etCantidad.setText(String.valueOf(linea.cantidad));
        d.etPrecio.setText(String.valueOf(linea.precioUnitario));

        AlertDialog dialogo = new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.linea_titulo)
                .setView(d.getRoot())
                .setNegativeButton(R.string.btn_cancelar, null)
                .setPositiveButton(R.string.btn_aplicar, null)
                .create();

        mostrarConValidacion(dialogo, () -> {
            int cantidad = Dinero.parsearEntero(d.etCantidad.getText());
            long precio = Dinero.parsear(d.etPrecio.getText());
            if (cantidad < 1 || cantidad > linea.stockDisponible) {
                d.tilCantidad.setError(ctx.getString(R.string.err_cantidad_rango, linea.stockDisponible));
                return;
            }
            d.tilCantidad.setError(null);
            carrito.actualizar(linea, cantidad, precio);
            dialogo.dismiss();
        });
    }

    // =====================================================================
    // Elegir un producto para agregar a la venta
    // =====================================================================
    public static void seleccionarProducto(Context ctx, List<Producto> disponibles, ProductoAdapter.OnClick alElegir) {
        DialogSeleccionarProductoBinding d = DialogSeleccionarProductoBinding.inflate(LayoutInflater.from(ctx));
        final AlertDialog[] referencia = new AlertDialog[1];

        ProductoAdapter adaptador = new ProductoAdapter(producto -> {
            if (referencia[0] != null) {
                referencia[0].dismiss();
            }
            alElegir.alHacerClick(producto);
        });
        d.rvProductos.setLayoutManager(new LinearLayoutManager(ctx));
        d.rvProductos.setAdapter(adaptador);

        final List<Producto> base = new ArrayList<>(disponibles);
        adaptador.submitList(FiltroProductos.filtrar(base, "", null));
        d.tvVacio.setVisibility(base.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);

        Watchers.alCambiar(d.etBuscar, texto -> adaptador.submitList(FiltroProductos.filtrar(base, texto, null)));

        referencia[0] = new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.venta_agregar_producto)
                .setView(d.getRoot())
                .setNegativeButton(R.string.btn_cerrar, null)
                .create();
        referencia[0].show();
    }
}
