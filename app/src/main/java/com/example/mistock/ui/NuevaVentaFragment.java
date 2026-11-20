package com.example.mistock.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.mistock.R;
import com.example.mistock.data.Cliente;
import com.example.mistock.data.Producto;
import com.example.mistock.data.StockRepository;
import com.example.mistock.databinding.FragmentNuevaVentaBinding;
import com.example.mistock.databinding.ItemCarritoLineaBinding;
import com.example.mistock.util.Args;
import com.example.mistock.util.Dinero;
import com.example.mistock.util.Escaner;
import com.example.mistock.util.Texto;
import com.example.mistock.viewmodel.CarritoVenta;
import com.example.mistock.viewmodel.StockViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Registrar una venta: cliente (opcional), productos, forma de pago (contado o con saldo pendiente) y notas.
 * Al guardar se descuenta el stock y, si queda saldo, se registra la deuda del cliente.
 */
public class NuevaVentaFragment extends Fragment {

    private FragmentNuevaVentaBinding b;
    private StockViewModel vm;
    private CarritoVenta carrito;

    private List<Cliente> clientes = new ArrayList<>();
    private List<Producto> disponibles = new ArrayList<>();
    private ArrayAdapter<String> adaptadorClientes;
    private boolean guardando = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentNuevaVentaBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(StockViewModel.class);
        carrito = vm.carrito;

        long clienteArg = Args.leer(this, Args.CLIENTE);
        long productoArg = Args.leer(this, Args.PRODUCTO);
        if (savedInstanceState == null && (clienteArg > 0L || productoArg > 0L)) {
            // Se llegó desde un producto o un cliente: se parte con una venta limpia.
            carrito.limpiar();
            if (clienteArg > 0L) {
                carrito.clienteId = clienteArg;
            }
            if (productoArg > 0L) {
                vm.obtenerProducto(productoArg, r -> {
                    if (b != null && r.exito && r.valor != null) {
                        carrito.agregar(r.valor);
                    }
                });
            }
        }

        configurarClientes();
        configurarProductos();
        configurarPago();

        b.btnRegistrarVenta.setOnClickListener(v -> registrar());
    }

    // ---- Cliente ----

    private void configurarClientes() {
        adaptadorClientes = new ArrayAdapter<>(requireContext(), R.layout.item_dropdown, new ArrayList<String>());
        b.acCliente.setAdapter(adaptadorClientes);
        b.acCliente.setOnItemClickListener((padre, vista, posicion, id) -> {
            carrito.clienteId = posicion == 0 ? null : clientes.get(posicion - 1).id;
            b.tilCliente.setError(null);
            actualizarVistaPrevia();
        });

        vm.clientes().observe(getViewLifecycleOwner(), lista -> {
            clientes = lista == null ? new ArrayList<>() : lista;
            adaptadorClientes.clear();
            adaptadorClientes.add(getString(R.string.venta_directa_opcion));
            for (Cliente c : clientes) {
                adaptadorClientes.add(c.nombre);
            }
            adaptadorClientes.notifyDataSetChanged();
            mostrarClienteElegido();
        });

        b.btnNuevoCliente.setOnClickListener(v ->
                Dialogos.cliente(requireContext(), vm, null, idNuevo -> {
                    if (idNuevo != null) {
                        carrito.clienteId = idNuevo;
                        if (b != null) {
                            mostrarClienteElegido();
                        }
                    }
                }));
    }

    private void mostrarClienteElegido() {
        String texto = getString(R.string.venta_directa_opcion);
        if (carrito.clienteId != null) {
            for (Cliente c : clientes) {
                if (c.id == carrito.clienteId) {
                    texto = c.nombre;
                    break;
                }
            }
        }
        b.acCliente.setText(texto, false);
    }

    // ---- Productos ----

    private void configurarProductos() {
        vm.productos().observe(getViewLifecycleOwner(), lista -> {
            disponibles = new ArrayList<>();
            if (lista != null) {
                for (Producto p : lista) {
                    if (p.stock > 0) {
                        disponibles.add(p);
                    }
                }
            }
        });

        b.btnAgregarProducto.setOnClickListener(v -> {
            if (disponibles.isEmpty()) {
                Toast.makeText(requireContext(), R.string.venta_sin_productos_disponibles, Toast.LENGTH_LONG).show();
                return;
            }
            Dialogos.seleccionarProducto(requireContext(), disponibles, producto -> {
                if (!carrito.agregar(producto)) {
                    Toast.makeText(requireContext(), R.string.venta_sin_mas_stock, Toast.LENGTH_SHORT).show();
                }
            });
        });

        b.btnEscanearVenta.setOnClickListener(v -> Escaner.escanear(requireContext(), this::agregarPorCodigo));

        carrito.lineasLive.observe(getViewLifecycleOwner(), this::dibujarCarrito);
    }

    /** Escanear un producto lo agrega a la venta (cada lectura suma una unidad). */
    private void agregarPorCodigo(String codigo) {
        if (b == null) {
            return;
        }
        vm.buscarPorCodigo(codigo, r -> {
            if (b == null) {
                return;
            }
            if (!r.exito) {
                Toast.makeText(requireContext(), r.mensaje, Toast.LENGTH_LONG).show();
            } else if (r.valor == null) {
                Toast.makeText(requireContext(), R.string.venta_codigo_no_existe, Toast.LENGTH_LONG).show();
            } else if (!carrito.agregar(r.valor)) {
                Toast.makeText(requireContext(), R.string.venta_sin_mas_stock, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void dibujarCarrito(List<CarritoVenta.Linea> lineas) {
        if (b == null) {
            return;
        }
        b.llCarrito.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (CarritoVenta.Linea linea : lineas) {
            ItemCarritoLineaBinding item = ItemCarritoLineaBinding.inflate(inflater, b.llCarrito, false);
            item.tvNombre.setText(linea.nombre);
            item.tvDetalle.setText(getString(R.string.linea_detalle, linea.cantidad, Dinero.formato(linea.precioUnitario)));
            item.tvSubtotal.setText(Dinero.formato(linea.subtotal()));
            item.getRoot().setOnClickListener(v -> Dialogos.editarLinea(requireContext(), carrito, linea));
            item.btnQuitar.setOnClickListener(v -> carrito.quitar(linea));
            b.llCarrito.addView(item.getRoot());
        }
        b.tvCarritoVacio.setVisibility(lineas.isEmpty() ? View.VISIBLE : View.GONE);
        b.tvTotal.setText(Dinero.formato(carrito.total()));
        actualizarVistaPrevia();
    }

    // ---- Pago ----

    private boolean esFiado() {
        return b.toggPago.getCheckedButtonId() == R.id.btnFiado;
    }

    private void configurarPago() {
        if (b.toggPago.getCheckedButtonId() == View.NO_ID) {
            b.toggPago.check(R.id.btnContado);
        }
        b.toggPago.addOnButtonCheckedListener((grupo, idBoton, marcado) -> {
            if (marcado) {
                b.tilAbono.setError(null);
                actualizarVistaPrevia();
            }
        });
        Watchers.alCambiar(b.etAbono, t -> {
            b.tilAbono.setError(null);
            actualizarVistaPrevia();
        });
        actualizarVistaPrevia();
    }

    private void actualizarVistaPrevia() {
        if (b == null) {
            return;
        }
        boolean fiado = esFiado();
        b.tilAbono.setVisibility(fiado ? View.VISIBLE : View.GONE);
        long total = carrito.total();
        if (fiado) {
            long abono = Dinero.parsear(b.etAbono.getText());
            long saldo = Math.max(0L, total - abono);
            b.tvSaldoPreview.setText(getString(R.string.venta_saldo_preview, Dinero.formato(saldo)));
        } else {
            b.tvSaldoPreview.setText(R.string.venta_pago_completo);
        }
    }

    // ---- Guardar ----

    private void registrar() {
        if (guardando) {
            return;
        }
        List<CarritoVenta.Linea> lineasCarrito = carrito.lineas();
        if (lineasCarrito.isEmpty()) {
            Toast.makeText(requireContext(), R.string.err_carrito_vacio, Toast.LENGTH_SHORT).show();
            return;
        }

        long total = carrito.total();
        boolean fiado = esFiado();
        long abono = total;

        if (fiado) {
            if (carrito.clienteId == null) {
                b.tilCliente.setError(getString(R.string.venta_sin_cliente));
                return;
            }
            abono = Dinero.parsear(b.etAbono.getText());
            if (abono > total) {
                b.tilAbono.setError(getString(R.string.err_abono_mayor_total, Dinero.formato(total)));
                return;
            }
        }

        List<StockRepository.LineaVenta> lineas = new ArrayList<>();
        for (CarritoVenta.Linea l : lineasCarrito) {
            lineas.add(new StockRepository.LineaVenta(l.productoId, l.cantidad, l.precioUnitario));
        }
        String nota = Texto.vacioANulo(b.etNota.getText());

        guardando = true;
        b.btnRegistrarVenta.setEnabled(false);
        final Context app = requireContext().getApplicationContext();
        vm.registrarVenta(carrito.clienteId, lineas, abono, nota, r -> {
            guardando = false;
            if (r.exito) {
                carrito.limpiar();
                Toast.makeText(app, app.getString(R.string.msg_venta_registrada), Toast.LENGTH_SHORT).show();
                if (b != null && r.valor != null) {
                    NavOptions opciones = new NavOptions.Builder()
                            .setPopUpTo(R.id.nuevaVentaFragment, true)
                            .build();
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.ventaDetalleFragment, Args.de(Args.VENTA, r.valor), opciones);
                }
            } else {
                Toast.makeText(app, r.mensaje, Toast.LENGTH_LONG).show();
                if (b != null) {
                    b.btnRegistrarVenta.setEnabled(true);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
