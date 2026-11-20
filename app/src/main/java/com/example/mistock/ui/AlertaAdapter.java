package com.example.mistock.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mistock.data.Producto;
import com.example.mistock.databinding.ItemAlertaBinding;
import com.example.mistock.databinding.ItemAlertaEncabezadoBinding;

import java.util.ArrayList;
import java.util.List;

/** Lista de la pestaña Alertas: encabezados de sección y productos con su motivo. */
public class AlertaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /** Una fila: o es un encabezado (titulo != null) o es un producto con su motivo. */
    public static class Fila {
        public String titulo;
        public Producto producto;
        public String motivo;
        public int nivel;
        public boolean permiteReponer;

        public static Fila encabezado(String titulo) {
            Fila f = new Fila();
            f.titulo = titulo;
            return f;
        }

        public static Fila producto(Producto p, String motivo, int nivel, boolean permiteReponer) {
            Fila f = new Fila();
            f.producto = p;
            f.motivo = motivo;
            f.nivel = nivel;
            f.permiteReponer = permiteReponer;
            return f;
        }
    }

    public interface Acciones {
        void abrir(Producto producto);

        void reponer(Producto producto);
    }

    private static final int TIPO_ENCABEZADO = 0;
    private static final int TIPO_PRODUCTO = 1;

    private final Acciones acciones;
    private final List<Fila> filas = new ArrayList<>();

    public AlertaAdapter(Acciones acciones) {
        this.acciones = acciones;
    }

    public void actualizar(List<Fila> nuevas) {
        filas.clear();
        filas.addAll(nuevas);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return filas.get(position).titulo != null ? TIPO_ENCABEZADO : TIPO_PRODUCTO;
    }

    @Override
    public int getItemCount() {
        return filas.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TIPO_ENCABEZADO) {
            return new HolderEncabezado(ItemAlertaEncabezadoBinding.inflate(inflater, parent, false));
        }
        return new HolderProducto(ItemAlertaBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Fila fila = filas.get(position);
        if (holder instanceof HolderEncabezado) {
            ((HolderEncabezado) holder).b.tvTitulo.setText(fila.titulo);
        } else {
            ((HolderProducto) holder).enlazar(fila, acciones);
        }
    }

    static class HolderEncabezado extends RecyclerView.ViewHolder {
        final ItemAlertaEncabezadoBinding b;

        HolderEncabezado(ItemAlertaEncabezadoBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }
    }

    static class HolderProducto extends RecyclerView.ViewHolder {
        private final ItemAlertaBinding b;

        HolderProducto(ItemAlertaBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void enlazar(Fila fila, Acciones acciones) {
            Producto p = fila.producto;
            b.tvNombre.setText(p.nombre);
            Insignias.aplicar(b.tvMotivo, fila.nivel, fila.motivo);
            b.btnReponer.setVisibility(fila.permiteReponer ? android.view.View.VISIBLE : android.view.View.GONE);
            b.btnReponer.setOnClickListener(v -> acciones.reponer(p));
            b.getRoot().setOnClickListener(v -> acciones.abrir(p));
        }
    }
}
