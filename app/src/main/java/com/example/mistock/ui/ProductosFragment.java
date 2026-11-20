package com.example.mistock.ui;

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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mistock.R;
import com.example.mistock.data.Producto;
import com.example.mistock.databinding.FragmentProductosBinding;
import com.example.mistock.util.Args;
import com.example.mistock.util.Catalogo;
import com.example.mistock.util.Escaner;
import com.example.mistock.viewmodel.StockViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/** Lista de productos con buscador y filtro por categoría. */
public class ProductosFragment extends Fragment {

    private FragmentProductosBinding b;
    private StockViewModel vm;
    private ProductoAdapter adaptador;

    private List<Producto> todos = new ArrayList<>();
    private String consulta = "";
    private String categoria = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentProductosBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(StockViewModel.class);

        adaptador = new ProductoAdapter(producto -> NavHostFragment.findNavController(this)
                .navigate(R.id.productoDetalleFragment, Args.de(Args.PRODUCTO, producto.id)));
        b.rvProductos.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvProductos.setAdapter(adaptador);

        crearChips();
        Watchers.alCambiar(b.etBuscar, texto -> {
            consulta = texto;
            aplicarFiltro();
        });

        b.btnEscanear.setOnClickListener(v -> Escaner.escanear(requireContext(), this::alEscanear));

        b.fabNuevo.setOnClickListener(v -> NavHostFragment.findNavController(this)
                .navigate(R.id.productoFormFragment, Args.de(Args.PRODUCTO, 0L)));

        vm.productos().observe(getViewLifecycleOwner(), lista -> {
            todos = lista == null ? new ArrayList<>() : lista;
            aplicarFiltro();
        });
    }

    /** Se leyó un código con la cámara: si el producto existe se ofrecen acciones; si no, se ofrece crearlo. */
    private void alEscanear(String codigo) {
        if (b == null) {
            return;
        }
        vm.buscarPorCodigo(codigo, r -> {
            if (b == null) {
                return;
            }
            if (!r.exito) {
                Toast.makeText(requireContext(), r.mensaje, Toast.LENGTH_LONG).show();
            } else if (r.valor == null) {
                ofrecerCrear(codigo);
            } else {
                mostrarAcciones(r.valor);
            }
        });
    }

    private void ofrecerCrear(String codigo) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.escaner_no_existe_titulo)
                .setMessage(getString(R.string.escaner_no_existe_msg, codigo))
                .setNegativeButton(R.string.btn_cancelar, null)
                .setPositiveButton(R.string.btn_crear_producto, (d, w) -> {
                    Bundle args = Args.de(Args.PRODUCTO, 0L);
                    args.putString(Args.CODIGO, codigo);
                    NavHostFragment.findNavController(this).navigate(R.id.productoFormFragment, args);
                })
                .show();
    }

    private void mostrarAcciones(Producto p) {
        String[] opciones = {
                getString(R.string.escaner_opcion_sumar),
                getString(R.string.escaner_opcion_vender),
                getString(R.string.escaner_opcion_ficha)
        };
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(p.nombre)
                .setItems(opciones, (d, cual) -> {
                    if (cual == 0) {
                        Dialogos.reponerStock(requireContext(), vm, p);
                    } else if (cual == 1) {
                        if (p.stock <= 0) {
                            Toast.makeText(requireContext(), R.string.escaner_sin_stock, Toast.LENGTH_SHORT).show();
                        } else {
                            NavHostFragment.findNavController(this)
                                    .navigate(R.id.nuevaVentaFragment, Args.de(Args.PRODUCTO, p.id));
                        }
                    } else {
                        NavHostFragment.findNavController(this)
                                .navigate(R.id.productoDetalleFragment, Args.de(Args.PRODUCTO, p.id));
                    }
                })
                .show();
    }

    private void crearChips() {
        b.chipsCategorias.addView(crearChip(null, getString(R.string.chip_todas), true));
        for (String c : Catalogo.CATEGORIAS) {
            b.chipsCategorias.addView(crearChip(c, c, false));
        }
    }

    private Chip crearChip(String valor, String texto, boolean marcado) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_filtro, b.chipsCategorias, false);
        chip.setText(texto);
        chip.setChecked(marcado);
        chip.setOnCheckedChangeListener((vista, activado) -> {
            if (activado) {
                categoria = valor;
                aplicarFiltro();
            }
        });
        return chip;
    }

    private void aplicarFiltro() {
        if (b == null) {
            return;
        }
        List<Producto> filtrados = FiltroProductos.filtrar(todos, consulta, categoria);
        adaptador.submitList(filtrados);

        boolean hayFiltro = !consulta.trim().isEmpty() || categoria != null;
        b.tvResumen.setText(getResources().getQuantityString(R.plurals.productos_resumen, filtrados.size(), filtrados.size()));
        if (filtrados.isEmpty()) {
            b.tvVacio.setText(todos.isEmpty() && !hayFiltro
                    ? R.string.productos_vacio : R.string.productos_sin_resultados);
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
