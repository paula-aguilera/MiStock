package com.example.mistock.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.mistock.R;
import com.example.mistock.data.Producto;
import com.example.mistock.databinding.FragmentProductoDetalleBinding;
import com.example.mistock.util.AppConfig;
import com.example.mistock.util.Args;
import com.example.mistock.util.Dinero;
import com.example.mistock.util.Fechas;
import com.example.mistock.viewmodel.StockViewModel;

import java.util.Locale;

/** Ficha de un producto: precios, ganancia, stock, vencimiento y acciones (vender, reponer, editar, eliminar). */
public class ProductoDetalleFragment extends Fragment {

    private FragmentProductoDetalleBinding b;
    private StockViewModel vm;
    private long productoId;
    private Producto actual;
    private boolean saliendo = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentProductoDetalleBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(StockViewModel.class);
        productoId = Args.leer(this, Args.PRODUCTO);

        vm.producto(productoId).observe(getViewLifecycleOwner(), producto -> {
            if (producto == null) {
                if (!saliendo) {
                    saliendo = true;
                    NavHostFragment.findNavController(this).navigateUp();
                }
                return;
            }
            actual = producto;
            mostrar(producto);
        });

        b.btnVender.setOnClickListener(v -> {
            Bundle args = Args.de(Args.PRODUCTO, productoId);
            NavHostFragment.findNavController(this).navigate(R.id.nuevaVentaFragment, args);
        });
        b.btnReponer.setOnClickListener(v -> {
            if (actual != null) {
                Dialogos.reponerStock(requireContext(), vm, actual);
            }
        });
        b.btnEditar.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.productoFormFragment, Args.de(Args.PRODUCTO, productoId)));
        b.btnEliminar.setOnClickListener(v -> confirmarEliminar());
    }

    private void mostrar(Producto p) {
        b.tvNombre.setText(p.nombre);

        StringBuilder detalle = new StringBuilder();
        if (p.marca != null) {
            detalle.append(p.marca);
        }
        if (p.linea != null) {
            if (detalle.length() > 0) {
                detalle.append(" · ");
            }
            detalle.append(p.linea);
        }
        b.tvDetalle.setText(detalle.length() == 0 ? getString(R.string.sin_detalle) : detalle.toString());

        StringBuilder extra = new StringBuilder();
        if (p.categoria != null) {
            extra.append(p.categoria);
        }
        if (p.codigoBarras != null) {
            if (extra.length() > 0) {
                extra.append(" · ");
            }
            extra.append(getString(R.string.codigo_valor, p.codigoBarras));
        }
        if (extra.length() == 0) {
            b.tvCategoria.setVisibility(View.GONE);
        } else {
            b.tvCategoria.setText(extra.toString());
            b.tvCategoria.setVisibility(View.VISIBLE);
        }

        b.tvPrecioReal.setText(Dinero.formato(p.precioReal));
        b.tvPrecioVenta.setText(Dinero.formato(p.precioVenta));
        long ganancia = p.gananciaUnitaria();
        b.tvGanancia.setText(Dinero.formato(ganancia));
        long margen = p.precioVenta > 0 ? Math.round(ganancia * 100.0 / p.precioVenta) : 0L;
        b.tvMargen.setText(String.format(Locale.forLanguageTag("es-CL"), "%d%%", margen));

        b.tvStock.setText(String.valueOf(p.stock));
        if (p.stock <= 0) {
            Insignias.aplicar(b.tvEstadoStock, Insignias.PELIGRO, getString(R.string.estado_agotado));
        } else if (p.stock <= AppConfig.UMBRAL_STOCK_BAJO) {
            Insignias.aplicar(b.tvEstadoStock, Insignias.AVISO, getString(R.string.estado_stock_bajo, p.stock));
        } else {
            Insignias.aplicar(b.tvEstadoStock, Insignias.OK, getString(R.string.estado_stock_ok));
        }
        b.btnVender.setEnabled(p.stock > 0);

        int estado = Fechas.estadoVencimiento(p.fechaVencimiento);
        if (estado == Fechas.SIN_FECHA) {
            b.tvVencimiento.setText(R.string.sin_vencimiento);
            b.tvEstadoVencimiento.setVisibility(View.GONE);
        } else {
            b.tvVencimiento.setText(Fechas.fechaUtc(p.fechaVencimiento));
            int nivel = estado == Fechas.VENCIDO ? Insignias.PELIGRO
                    : estado == Fechas.POR_VENCER ? Insignias.AVISO : Insignias.OK;
            Insignias.aplicar(b.tvEstadoVencimiento, nivel, Fechas.textoVencimiento(p.fechaVencimiento));
        }
    }

    private void confirmarEliminar() {
        if (actual == null) {
            return;
        }
        final Producto producto = actual;
        Dialogos.confirmar(requireContext(),
                getString(R.string.eliminar_producto_titulo),
                getString(R.string.eliminar_producto_msg, producto.nombre),
                getString(R.string.btn_eliminar),
                () -> {
                    // Al borrarse, la ficha detecta que el producto ya no existe y vuelve a la lista sola.
                    final Context app = requireContext().getApplicationContext();
                    vm.eliminarProducto(producto, r ->
                            Toast.makeText(app,
                                    r.exito ? app.getString(R.string.msg_producto_eliminado) : r.mensaje,
                                    r.exito ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show());
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
