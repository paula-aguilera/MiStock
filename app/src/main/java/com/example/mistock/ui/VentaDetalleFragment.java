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
import com.example.mistock.data.Abono;
import com.example.mistock.data.Venta;
import com.example.mistock.data.VentaConCliente;
import com.example.mistock.data.VentaItem;
import com.example.mistock.databinding.FragmentVentaDetalleBinding;
import com.example.mistock.databinding.ItemFilaDetalleBinding;
import com.example.mistock.util.Args;
import com.example.mistock.util.Dinero;
import com.example.mistock.util.Fechas;
import com.example.mistock.viewmodel.StockViewModel;

import java.util.List;

/** Detalle de una venta: productos, notas, abonos con su fecha, saldo pendiente y ganancia. */
public class VentaDetalleFragment extends Fragment {

    private FragmentVentaDetalleBinding b;
    private StockViewModel vm;
    private long ventaId;
    private Venta actual;
    private boolean saliendo = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentVentaDetalleBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(StockViewModel.class);
        ventaId = Args.leer(this, Args.VENTA);

        vm.venta(ventaId).observe(getViewLifecycleOwner(), item -> {
            if (item == null) {
                if (!saliendo) {
                    saliendo = true;
                    NavHostFragment.findNavController(this).navigateUp();
                }
                return;
            }
            actual = item.venta;
            mostrarVenta(item);
        });
        vm.itemsVenta(ventaId).observe(getViewLifecycleOwner(), this::mostrarItems);
        vm.abonosVenta(ventaId).observe(getViewLifecycleOwner(), this::mostrarAbonos);

        b.btnAbono.setOnClickListener(v -> {
            if (actual != null && actual.saldo() > 0L) {
                Dialogos.abono(requireContext(), vm, ventaId, actual.saldo());
            }
        });
        b.btnAnular.setOnClickListener(v -> confirmarAnular());
    }

    private void mostrarVenta(VentaConCliente item) {
        Venta v = item.venta;
        b.tvCliente.setText(item.clienteNombre != null ? item.clienteNombre : getString(R.string.venta_directa));
        b.tvFecha.setText(Fechas.fechaHora(v.fecha));

        long saldo = v.saldo();
        if (saldo > 0L) {
            Insignias.aplicar(b.tvEstado, Insignias.AVISO, getString(R.string.estado_pendiente));
        } else {
            Insignias.aplicar(b.tvEstado, Insignias.OK, getString(R.string.estado_pagada));
        }

        if (v.nota == null || v.nota.trim().isEmpty()) {
            b.cardNota.setVisibility(View.GONE);
        } else {
            b.tvNota.setText(v.nota);
            b.cardNota.setVisibility(View.VISIBLE);
        }

        b.tvTotal.setText(Dinero.formato(v.total));
        b.tvAbonado.setText(Dinero.formato(v.abonado));
        b.tvSaldo.setText(Dinero.formato(Math.max(0L, saldo)));
        b.tvGananciaVenta.setText(Dinero.formato(v.total - v.costoTotal));
        b.btnAbono.setVisibility(saldo > 0L ? View.VISIBLE : View.GONE);
    }

    private void mostrarItems(List<VentaItem> items) {
        b.llItems.removeAllViews();
        if (items == null) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (VentaItem it : items) {
            ItemFilaDetalleBinding fila = ItemFilaDetalleBinding.inflate(inflater, b.llItems, false);
            fila.tvTitulo.setText(it.nombre);
            fila.tvSub.setText(getString(R.string.linea_detalle, it.cantidad, Dinero.formato(it.precioVenta)));
            fila.tvValor.setText(Dinero.formato(it.subtotal()));
            b.llItems.addView(fila.getRoot());
        }
    }

    private void mostrarAbonos(List<Abono> abonos) {
        b.llAbonos.removeAllViews();
        boolean hay = abonos != null && !abonos.isEmpty();
        b.tvSinAbonos.setVisibility(hay ? View.GONE : View.VISIBLE);
        if (!hay) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Abono a : abonos) {
            ItemFilaDetalleBinding fila = ItemFilaDetalleBinding.inflate(inflater, b.llAbonos, false);
            fila.tvTitulo.setText(Fechas.fechaHora(a.fecha));
            if (a.nota == null || a.nota.trim().isEmpty()) {
                fila.tvSub.setVisibility(View.GONE);
            } else {
                fila.tvSub.setText(a.nota);
            }
            fila.tvValor.setText(Dinero.formato(a.monto));
            b.llAbonos.addView(fila.getRoot());
        }
    }

    private void confirmarAnular() {
        Dialogos.confirmar(requireContext(),
                getString(R.string.anular_venta_titulo),
                getString(R.string.anular_venta_msg),
                getString(R.string.btn_anular_venta),
                () -> {
                    final Context app = requireContext().getApplicationContext();
                    vm.anularVenta(ventaId, r -> Toast.makeText(app,
                            r.exito ? app.getString(R.string.msg_venta_anulada) : r.mensaje,
                            r.exito ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show());
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
