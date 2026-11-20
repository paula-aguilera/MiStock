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
import com.example.mistock.data.Producto;
import com.example.mistock.databinding.FragmentAlertasBinding;
import com.example.mistock.util.Args;
import com.example.mistock.util.Fechas;
import com.example.mistock.viewmodel.StockViewModel;

import java.util.ArrayList;
import java.util.List;

/** Pestaña Alertas: productos con stock bajo (para volver a comprar) y productos por vencer o vencidos. */
public class AlertasFragment extends Fragment {

    private FragmentAlertasBinding b;
    private StockViewModel vm;
    private AlertaAdapter adaptador;

    private List<Producto> stockBajo = new ArrayList<>();
    private List<Producto> porVencer = new ArrayList<>();
    private boolean cargoStock = false;
    private boolean cargoVencimiento = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentAlertasBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(StockViewModel.class);

        adaptador = new AlertaAdapter(new AlertaAdapter.Acciones() {
            @Override
            public void abrir(Producto producto) {
                NavHostFragment.findNavController(AlertasFragment.this)
                        .navigate(R.id.productoDetalleFragment, Args.de(Args.PRODUCTO, producto.id));
            }

            @Override
            public void reponer(Producto producto) {
                Dialogos.reponerStock(requireContext(), vm, producto);
            }
        });
        b.rvAlertas.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvAlertas.setAdapter(adaptador);

        vm.stockBajo().observe(getViewLifecycleOwner(), lista -> {
            stockBajo = lista == null ? new ArrayList<>() : lista;
            cargoStock = true;
            reconstruir();
        });
        vm.porVencer().observe(getViewLifecycleOwner(), lista -> {
            porVencer = lista == null ? new ArrayList<>() : lista;
            cargoVencimiento = true;
            reconstruir();
        });
    }

    private void reconstruir() {
        if (b == null || !cargoStock || !cargoVencimiento) {
            return;
        }
        List<AlertaAdapter.Fila> filas = new ArrayList<>();

        if (!stockBajo.isEmpty()) {
            filas.add(AlertaAdapter.Fila.encabezado(getString(R.string.alertas_seccion_stock, stockBajo.size())));
            for (Producto p : stockBajo) {
                if (p.stock <= 0) {
                    filas.add(AlertaAdapter.Fila.producto(p, getString(R.string.estado_agotado), Insignias.PELIGRO, true));
                } else {
                    filas.add(AlertaAdapter.Fila.producto(p, getString(R.string.alertas_quedan, p.stock), Insignias.AVISO, true));
                }
            }
        }

        if (!porVencer.isEmpty()) {
            filas.add(AlertaAdapter.Fila.encabezado(getString(R.string.alertas_seccion_vencimiento, porVencer.size())));
            for (Producto p : porVencer) {
                int nivel = Fechas.estadoVencimiento(p.fechaVencimiento) == Fechas.VENCIDO
                        ? Insignias.PELIGRO : Insignias.AVISO;
                filas.add(AlertaAdapter.Fila.producto(p, Fechas.textoVencimiento(p.fechaVencimiento), nivel, false));
            }
        }

        adaptador.actualizar(filas);
        b.tvVacio.setVisibility(filas.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
