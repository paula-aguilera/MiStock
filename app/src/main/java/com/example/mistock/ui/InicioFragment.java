package com.example.mistock.ui;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.mistock.MainActivity;
import com.example.mistock.R;
import com.example.mistock.databinding.FragmentInicioBinding;
import com.example.mistock.util.Args;
import com.example.mistock.util.Dinero;
import com.example.mistock.util.Fechas;
import com.example.mistock.viewmodel.StockViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Pantalla de inicio: resumen del mes, aviso de alertas, accesos rápidos y respaldo. */
public class InicioFragment extends Fragment {

    private FragmentInicioBinding b;
    private StockViewModel vm;
    private int numStockBajo = 0;
    private int numPorVencer = 0;

    private final ActivityResultLauncher<String> crearRespaldo =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/octet-stream"), uri -> {
                if (uri != null) {
                    exportar(uri);
                }
            });

    private final ActivityResultLauncher<String[]> elegirRespaldo =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    confirmarRestauracion(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentInicioBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(StockViewModel.class);

        String hoy = new SimpleDateFormat("EEEE d 'de' MMMM", Locale.forLanguageTag("es-CL")).format(new Date());
        b.tvFechaHoy.setText(hoy.substring(0, 1).toUpperCase(Locale.ROOT) + hoy.substring(1));

        long ahora = System.currentTimeMillis();
        vm.resumen(Fechas.inicioMes(ahora), Fechas.finMes(ahora)).observe(getViewLifecycleOwner(), r -> {
            if (r == null) {
                return;
            }
            b.tvGananciaMes.setText(Dinero.formato(r.ganancia()));
            b.tvVentasMes.setText(Dinero.formato(r.totalVentas));
        });
        vm.totalPorCobrar().observe(getViewLifecycleOwner(), total ->
                b.tvPorCobrar.setText(Dinero.formato(total == null ? 0L : total)));
        vm.cantidadProductos().observe(getViewLifecycleOwner(), n ->
                b.tvProductos.setText(String.valueOf(n == null ? 0 : n)));
        vm.valorInventarioCosto().observe(getViewLifecycleOwner(), v ->
                b.tvValorCosto.setText(Dinero.formato(v == null ? 0L : v)));
        vm.valorInventarioVenta().observe(getViewLifecycleOwner(), v ->
                b.tvValorVenta.setText(Dinero.formato(v == null ? 0L : v)));

        vm.stockBajo().observe(getViewLifecycleOwner(), lista -> {
            numStockBajo = lista == null ? 0 : lista.size();
            actualizarAviso();
        });
        vm.porVencer().observe(getViewLifecycleOwner(), lista -> {
            numPorVencer = lista == null ? 0 : lista.size();
            actualizarAviso();
        });

        b.cardAlertas.setOnClickListener(v -> ((MainActivity) requireActivity()).seleccionarTab(R.id.alertasFragment));
        b.btnNuevaVenta.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.nuevaVentaFragment));
        b.btnNuevoProducto.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.productoFormFragment, Args.de(Args.PRODUCTO, 0L)));

        b.btnExportar.setOnClickListener(v -> {
            String nombre = "MiStock_respaldo_" + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.ROOT).format(new Date()) + ".db";
            crearRespaldo.launch(nombre);
        });
        b.btnRestaurar.setOnClickListener(v -> elegirRespaldo.launch(new String[]{"*/*"}));
    }

    private void actualizarAviso() {
        if (b == null) {
            return;
        }
        if (numStockBajo + numPorVencer == 0) {
            b.cardAlertas.setVisibility(View.GONE);
        } else {
            b.tvAlertasTexto.setText(getString(R.string.inicio_alertas_texto, numStockBajo, numPorVencer));
            b.cardAlertas.setVisibility(View.VISIBLE);
        }
    }

    private void exportar(Uri destino) {
        vm.exportarRespaldo(destino, r -> {
            if (getContext() == null) {
                return;
            }
            Toast.makeText(requireContext(),
                    r.exito ? getString(R.string.respaldo_exportado) : r.mensaje, Toast.LENGTH_LONG).show();
        });
    }

    private void confirmarRestauracion(Uri origen) {
        Dialogos.confirmar(requireContext(),
                getString(R.string.respaldo_restaurar_titulo),
                getString(R.string.respaldo_restaurar_msg),
                getString(R.string.btn_restaurar),
                () -> vm.restaurarRespaldo(origen, r -> {
                    if (getContext() == null) {
                        return;
                    }
                    if (r.exito) {
                        avisarCierre();
                    } else {
                        Toast.makeText(requireContext(), r.mensaje, Toast.LENGTH_LONG).show();
                    }
                }));
    }

    /** Después de restaurar, la app debe reiniciarse para leer los datos nuevos. */
    private void avisarCierre() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.respaldo_restaurar_titulo)
                .setMessage(R.string.respaldo_restaurado_msg)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_cerrar_app, (d, w) -> {
                    requireActivity().finishAffinity();
                    new Handler(Looper.getMainLooper()).postDelayed(
                            () -> Process.killProcess(Process.myPid()), 400L);
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
