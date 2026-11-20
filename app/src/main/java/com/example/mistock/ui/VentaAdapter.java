package com.example.mistock.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mistock.R;
import com.example.mistock.data.Venta;
import com.example.mistock.data.VentaConCliente;
import com.example.mistock.databinding.ItemVentaBinding;
import com.example.mistock.util.Dinero;
import com.example.mistock.util.Fechas;

import java.util.Objects;

/** Lista de ventas (historial del reporte y compras de un cliente). */
public class VentaAdapter extends ListAdapter<VentaConCliente, VentaAdapter.Holder> {

    public interface OnClick {
        void alHacerClick(VentaConCliente venta);
    }

    private static final DiffUtil.ItemCallback<VentaConCliente> DIFF = new DiffUtil.ItemCallback<VentaConCliente>() {
        @Override
        public boolean areItemsTheSame(@NonNull VentaConCliente a, @NonNull VentaConCliente b) {
            return a.venta.id == b.venta.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull VentaConCliente a, @NonNull VentaConCliente b) {
            return a.venta.total == b.venta.total
                    && a.venta.abonado == b.venta.abonado
                    && a.venta.fecha == b.venta.fecha
                    && Objects.equals(a.clienteNombre, b.clienteNombre);
        }
    };

    private final OnClick listener;

    public VentaAdapter(OnClick listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemVentaBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.enlazar(getItem(position), listener);
    }

    static class Holder extends RecyclerView.ViewHolder {
        private final ItemVentaBinding b;

        Holder(ItemVentaBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void enlazar(VentaConCliente item, OnClick listener) {
            Context ctx = b.getRoot().getContext();
            Venta v = item.venta;
            b.tvFecha.setText(Fechas.fechaHora(v.fecha));
            b.tvCliente.setText(item.clienteNombre != null ? item.clienteNombre : ctx.getString(R.string.venta_directa));
            b.tvTotal.setText(Dinero.formato(v.total));

            long saldo = v.total - v.abonado;
            if (saldo > 0) {
                Insignias.aplicar(b.tvEstado, Insignias.AVISO, ctx.getString(R.string.estado_saldo, Dinero.formato(saldo)));
            } else {
                Insignias.aplicar(b.tvEstado, Insignias.OK, ctx.getString(R.string.estado_pagada));
            }
            b.getRoot().setOnClickListener(x -> listener.alHacerClick(item));
        }
    }
}
