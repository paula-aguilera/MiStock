package com.example.mistock.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mistock.R;
import com.example.mistock.data.Producto;
import com.example.mistock.databinding.ItemProductoBinding;
import com.example.mistock.util.AppConfig;
import com.example.mistock.util.Dinero;
import com.example.mistock.util.Fechas;
import com.example.mistock.util.Texto;

import java.util.Objects;

/** Lista de productos en tarjetas (se usa en Productos y en el selector de la venta). */
public class ProductoAdapter extends ListAdapter<Producto, ProductoAdapter.Holder> {

    public interface OnClick {
        void alHacerClick(Producto producto);
    }

    private static final DiffUtil.ItemCallback<Producto> DIFF = new DiffUtil.ItemCallback<Producto>() {
        @Override
        public boolean areItemsTheSame(@NonNull Producto a, @NonNull Producto b) {
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Producto a, @NonNull Producto b) {
            return a.stock == b.stock
                    && a.precioVenta == b.precioVenta
                    && a.precioReal == b.precioReal
                    && a.nombre.equals(b.nombre)
                    && Objects.equals(a.categoria, b.categoria)
                    && Objects.equals(a.linea, b.linea)
                    && Objects.equals(a.marca, b.marca)
                    && Objects.equals(a.fechaVencimiento, b.fechaVencimiento);
        }
    };

    private final OnClick listener;

    public ProductoAdapter(OnClick listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemProductoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.enlazar(getItem(position), listener);
    }

    static class Holder extends RecyclerView.ViewHolder {
        private final ItemProductoBinding b;

        Holder(ItemProductoBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void enlazar(Producto p, OnClick listener) {
            Context ctx = b.getRoot().getContext();
            b.tvInicial.setText(Texto.inicial(p.nombre));
            b.tvNombre.setText(p.nombre);
            b.tvDetalle.setText(detalle(ctx, p));
            b.tvPrecio.setText(Dinero.formato(p.precioVenta));

            if (p.stock <= 0) {
                Insignias.aplicar(b.tvStock, Insignias.PELIGRO, ctx.getString(R.string.estado_agotado));
            } else if (p.stock <= AppConfig.UMBRAL_STOCK_BAJO) {
                Insignias.aplicar(b.tvStock, Insignias.AVISO, ctx.getString(R.string.estado_stock_bajo, p.stock));
            } else {
                Insignias.aplicar(b.tvStock, Insignias.NEUTRO, ctx.getString(R.string.estado_stock, p.stock));
            }

            int estado = Fechas.estadoVencimiento(p.fechaVencimiento);
            if (estado == Fechas.SIN_FECHA) {
                b.tvVence.setVisibility(android.view.View.GONE);
            } else {
                int nivel = estado == Fechas.VENCIDO ? Insignias.PELIGRO
                        : estado == Fechas.POR_VENCER ? Insignias.AVISO : Insignias.NEUTRO;
                Insignias.aplicar(b.tvVence, nivel, Fechas.textoVencimiento(p.fechaVencimiento));
            }

            b.getRoot().setOnClickListener(v -> listener.alHacerClick(p));
        }

        private static String detalle(Context ctx, Producto p) {
            StringBuilder sb = new StringBuilder();
            agregar(sb, p.marca);
            agregar(sb, p.linea);
            agregar(sb, p.categoria);
            return sb.length() == 0 ? ctx.getString(R.string.sin_detalle) : sb.toString();
        }

        private static void agregar(StringBuilder sb, String valor) {
            if (valor != null && !valor.trim().isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(" · ");
                }
                sb.append(valor.trim());
            }
        }
    }
}
