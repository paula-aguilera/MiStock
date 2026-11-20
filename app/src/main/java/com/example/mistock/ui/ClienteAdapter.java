package com.example.mistock.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mistock.R;
import com.example.mistock.data.ClienteConSaldo;
import com.example.mistock.databinding.ItemClienteBinding;
import com.example.mistock.util.Dinero;
import com.example.mistock.util.Texto;

import java.util.Objects;

public class ClienteAdapter extends ListAdapter<ClienteConSaldo, ClienteAdapter.Holder> {

    public interface OnClick {
        void alHacerClick(ClienteConSaldo cliente);
    }

    private static final DiffUtil.ItemCallback<ClienteConSaldo> DIFF = new DiffUtil.ItemCallback<ClienteConSaldo>() {
        @Override
        public boolean areItemsTheSame(@NonNull ClienteConSaldo a, @NonNull ClienteConSaldo b) {
            return a.cliente.id == b.cliente.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ClienteConSaldo a, @NonNull ClienteConSaldo b) {
            return a.saldo == b.saldo
                    && a.compras == b.compras
                    && a.cliente.nombre.equals(b.cliente.nombre)
                    && Objects.equals(a.cliente.telefono, b.cliente.telefono)
                    && Objects.equals(a.cliente.correo, b.cliente.correo);
        }
    };

    private final OnClick listener;

    public ClienteAdapter(OnClick listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemClienteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.enlazar(getItem(position), listener);
    }

    static class Holder extends RecyclerView.ViewHolder {
        private final ItemClienteBinding b;

        Holder(ItemClienteBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void enlazar(ClienteConSaldo item, OnClick listener) {
            Context ctx = b.getRoot().getContext();
            b.tvInicial.setText(Texto.inicial(item.cliente.nombre));
            b.tvNombre.setText(item.cliente.nombre);

            String telefono = item.cliente.telefono;
            String contacto = (telefono == null || telefono.trim().isEmpty())
                    ? ctx.getString(R.string.sin_telefono) : telefono;
            b.tvContacto.setText(contacto + " · " + ctx.getString(R.string.cliente_compras, item.compras));

            if (item.saldo > 0) {
                Insignias.aplicar(b.tvSaldo, Insignias.AVISO, ctx.getString(R.string.estado_debe, Dinero.formato(item.saldo)));
            } else {
                Insignias.aplicar(b.tvSaldo, Insignias.OK, ctx.getString(R.string.estado_al_dia));
            }
            b.getRoot().setOnClickListener(v -> listener.alHacerClick(item));
        }
    }
}
