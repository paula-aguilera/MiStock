package com.example.mistock.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mistock.R;
import com.example.mistock.data.ClienteConSaldo;
import com.example.mistock.databinding.FragmentClientesBinding;
import com.example.mistock.util.Args;
import com.example.mistock.util.Dinero;
import com.example.mistock.util.Texto;
import com.example.mistock.viewmodel.StockViewModel;

import java.util.ArrayList;
import java.util.List;

/** Lista de clientes con lo que cada uno debe. */
public class ClientesFragment extends Fragment {

    private FragmentClientesBinding b;
    private StockViewModel vm;
    private ClienteAdapter adaptador;

    private List<ClienteConSaldo> todos = new ArrayList<>();
    private String consulta = "";
    private boolean soloConDeuda = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentClientesBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(StockViewModel.class);

        adaptador = new ClienteAdapter(item -> abrirCliente(item.cliente.id));
        b.rvClientes.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvClientes.setAdapter(adaptador);

        Watchers.alCambiar(b.etBuscar, texto -> {
            consulta = texto;
            aplicarFiltro();
        });
        b.chipsFiltro.setOnCheckedStateChangeListener((grupo, ids) -> {
            soloConDeuda = ids.contains(R.id.chipConDeuda);
            aplicarFiltro();
        });

        b.fabNuevo.setOnClickListener(v -> Dialogos.cliente(requireContext(), vm, null, id -> {
            if (id != null && isAdded()) {
                abrirCliente(id);
            }
        }));

        vm.totalPorCobrar().observe(getViewLifecycleOwner(), total ->
                b.tvTotalPorCobrar.setText(Dinero.formato(total == null ? 0L : total)));
        vm.clientesConSaldo().observe(getViewLifecycleOwner(), lista -> {
            todos = lista == null ? new ArrayList<>() : lista;
            aplicarFiltro();
        });
    }

    private void abrirCliente(long id) {
        NavHostFragment.findNavController(this).navigate(R.id.clienteDetalleFragment, Args.de(Args.CLIENTE, id));
    }

    private void aplicarFiltro() {
        if (b == null) {
            return;
        }
        String q = Texto.normalizar(consulta);
        List<ClienteConSaldo> resultado = new ArrayList<>();
        for (ClienteConSaldo c : todos) {
            if (soloConDeuda && c.saldo <= 0L) {
                continue;
            }
            if (!q.isEmpty()) {
                String texto = Texto.normalizar(c.cliente.nombre + " "
                        + (c.cliente.telefono == null ? "" : c.cliente.telefono) + " "
                        + (c.cliente.correo == null ? "" : c.cliente.correo));
                if (!texto.contains(q)) {
                    continue;
                }
            }
            resultado.add(c);
        }
        adaptador.submitList(resultado);

        if (resultado.isEmpty()) {
            int mensaje;
            if (todos.isEmpty()) {
                mensaje = R.string.clientes_vacio;
            } else if (soloConDeuda && q.isEmpty()) {
                mensaje = R.string.clientes_nadie_debe;
            } else {
                mensaje = R.string.clientes_sin_resultados;
            }
            b.tvVacio.setText(mensaje);
            b.tvVacio.setVisibility(View.VISIBLE);
        } else {
            b.tvVacio.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
