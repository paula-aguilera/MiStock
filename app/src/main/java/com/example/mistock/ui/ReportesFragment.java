package com.example.mistock.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mistock.R;
import com.example.mistock.data.ResumenVentas;
import com.example.mistock.data.TopProducto;
import com.example.mistock.data.VentaConCliente;
import com.example.mistock.databinding.FragmentReportesBinding;
import com.example.mistock.databinding.ItemFilaDetalleBinding;
import com.example.mistock.util.Args;
import com.example.mistock.util.Dinero;
import com.example.mistock.util.Fechas;
import com.example.mistock.viewmodel.StockViewModel;

import java.util.List;

/** Reporte de ganancias por período (hoy, últimos 7 días, este mes o todo), con los productos más vendidos. */
public class ReportesFragment extends Fragment {

    private FragmentReportesBinding b;
    private StockViewModel vm;
    private VentaAdapter adaptador;

    private LiveData<ResumenVentas> resumenLive;
    private LiveData<List<TopProducto>> topLive;
    private LiveData<List<VentaConCliente>> ventasLive;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentReportesBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(StockViewModel.class);

        adaptador = new VentaAdapter(item -> NavHostFragment.findNavController(this)
                .navigate(R.id.ventaDetalleFragment, Args.de(Args.VENTA, item.venta.id)));
        b.rvVentas.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvVentas.setAdapter(adaptador);

        b.chipsPeriodo.setOnCheckedStateChangeListener((grupo, ids) -> cargarPeriodo());
        cargarPeriodo();
    }

    /** Vuelve a pedir los datos según el chip elegido (se sueltan los observadores del período anterior). */
    private void cargarPeriodo() {
        if (b == null) {
            return;
        }
        long ahora = System.currentTimeMillis();
        long desde;
        long hasta;
        int elegido = b.chipsPeriodo.getCheckedChipId();
        if (elegido == R.id.chipHoy) {
            desde = Fechas.inicioDia(ahora);
            hasta = Fechas.finDia(ahora);
        } else if (elegido == R.id.chipSemana) {
            desde = Fechas.inicioDiaHaceDias(6);
            hasta = Fechas.finDia(ahora);
        } else if (elegido == R.id.chipTodo) {
            desde = 0L;
            hasta = Long.MAX_VALUE;
        } else {
            desde = Fechas.inicioMes(ahora);
            hasta = Fechas.finMes(ahora);
        }

        soltar();
        resumenLive = vm.resumen(desde, hasta);
        topLive = vm.masVendidos(desde, hasta);
        ventasLive = vm.ventasEntre(desde, hasta);
        resumenLive.observe(getViewLifecycleOwner(), this::mostrarResumen);
        topLive.observe(getViewLifecycleOwner(), this::mostrarTop);
        ventasLive.observe(getViewLifecycleOwner(), adaptador::submitList);
    }

    private void soltar() {
        if (resumenLive != null) {
            resumenLive.removeObservers(getViewLifecycleOwner());
        }
        if (topLive != null) {
            topLive.removeObservers(getViewLifecycleOwner());
        }
        if (ventasLive != null) {
            ventasLive.removeObservers(getViewLifecycleOwner());
        }
    }

    private void mostrarResumen(ResumenVentas r) {
        if (b == null) {
            return;
        }
        if (r == null) {
            r = new ResumenVentas();
        }
        b.tvGanancia.setText(Dinero.formato(r.ganancia()));
        long margen = r.totalVentas > 0 ? Math.round(r.ganancia() * 100.0 / r.totalVentas) : 0L;
        b.tvMargen.setText(getString(R.string.rep_margen, (int) margen));
        b.tvVentas.setText(Dinero.formato(r.totalVentas));
        b.tvCosto.setText(Dinero.formato(r.totalCosto));
        b.tvCobrado.setText(Dinero.formato(r.totalCobrado));
        b.tvPorCobrar.setText(Dinero.formato(r.porCobrar()));
        b.tvUnidades.setText(String.valueOf(r.unidades));
    }

    private void mostrarTop(List<TopProducto> lista) {
        if (b == null) {
            return;
        }
        b.llTop.removeAllViews();
        boolean hay = lista != null && !lista.isEmpty();
        b.tvSinTop.setVisibility(hay ? View.GONE : View.VISIBLE);
        if (!hay) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int puesto = 1;
        for (TopProducto t : lista) {
            ItemFilaDetalleBinding fila = ItemFilaDetalleBinding.inflate(inflater, b.llTop, false);
            fila.tvTitulo.setText(getString(R.string.rep_top_titulo, puesto, t.nombre));
            fila.tvSub.setText(getString(R.string.rep_top_unidades, (int) t.unidades));
            fila.tvValor.setText(Dinero.formato(t.ganancia));
            b.llTop.addView(fila.getRoot());
            puesto++;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        resumenLive = null;
        topLive = null;
        ventasLive = null;
        b = null;
    }
}
