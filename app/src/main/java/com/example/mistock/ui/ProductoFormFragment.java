package com.example.mistock.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.mistock.R;
import com.example.mistock.data.Producto;
import com.example.mistock.databinding.FragmentProductoFormBinding;
import com.example.mistock.util.Args;
import com.example.mistock.util.Catalogo;
import com.example.mistock.util.Dinero;
import com.example.mistock.util.Escaner;
import com.example.mistock.util.Fechas;
import com.example.mistock.util.Texto;
import com.example.mistock.viewmodel.StockViewModel;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Formulario para crear o editar un producto. */
public class ProductoFormFragment extends Fragment {

    private static final String CLAVE_VENCIMIENTO = "vencimiento";

    private FragmentProductoFormBinding b;
    private StockViewModel vm;

    private long productoId = 0L;
    private Producto original = null;
    private Long vencimiento = null;
    private boolean guardando = false;

    private ArrayAdapter<String> adaptadorLineas;
    private ArrayAdapter<String> adaptadorMarcas;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentProductoFormBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(StockViewModel.class);
        productoId = Args.leer(this, Args.PRODUCTO);

        if (savedInstanceState != null && savedInstanceState.containsKey(CLAVE_VENCIMIENTO)) {
            vencimiento = savedInstanceState.getLong(CLAVE_VENCIMIENTO);
        }

        configurarListas();
        configurarGanancia();
        configurarFecha();

        b.btnGuardar.setOnClickListener(v -> guardar());
        b.tilCodigo.setEndIconOnClickListener(v -> Escaner.escanear(requireContext(), codigo -> {
            if (b != null) {
                b.etCodigo.setText(codigo);
            }
        }));

        if (productoId > 0L) {
            if (((AppCompatActivity) requireActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(R.string.titulo_producto_editar);
            }
            final boolean primeraVez = savedInstanceState == null;
            vm.obtenerProducto(productoId, r -> {
                if (b == null) {
                    return;
                }
                if (r.exito && r.valor != null) {
                    original = r.valor;
                    if (primeraVez) {
                        llenar(r.valor);
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.err_producto_no_existe, Toast.LENGTH_LONG).show();
                    NavHostFragment.findNavController(this).navigateUp();
                }
            });
        } else if (savedInstanceState == null) {
            b.acMarca.setText(Catalogo.MARCAS[0], false);
            String codigoNuevo = Args.leerTexto(this, Args.CODIGO);
            if (codigoNuevo != null) {
                b.etCodigo.setText(codigoNuevo);
            }
        }
    }

    // ---- Listas desplegables ----

    private void configurarListas() {
        b.acCategoria.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.item_dropdown, Catalogo.CATEGORIAS));

        adaptadorLineas = new ArrayAdapter<>(requireContext(), R.layout.item_dropdown, new ArrayList<String>());
        adaptadorMarcas = new ArrayAdapter<>(requireContext(), R.layout.item_dropdown, new ArrayList<String>());
        b.acLinea.setAdapter(adaptadorLineas);
        b.acMarca.setAdapter(adaptadorMarcas);
        reemplazar(adaptadorLineas, Catalogo.LINEAS, null);
        reemplazar(adaptadorMarcas, Catalogo.MARCAS, null);

        // Además de las sugerencias del catálogo, aprende las líneas y marcas que ya escribiste antes.
        vm.lineas().observe(getViewLifecycleOwner(), lista -> reemplazar(adaptadorLineas, Catalogo.LINEAS, lista));
        vm.marcas().observe(getViewLifecycleOwner(), lista -> reemplazar(adaptadorMarcas, Catalogo.MARCAS, lista));
    }

    private static void reemplazar(ArrayAdapter<String> adaptador, String[] base, List<String> extra) {
        Set<String> unicos = new LinkedHashSet<>();
        for (String s : base) {
            unicos.add(s);
        }
        if (extra != null) {
            for (String s : extra) {
                if (s != null && !s.trim().isEmpty()) {
                    unicos.add(s.trim());
                }
            }
        }
        adaptador.clear();
        adaptador.addAll(unicos);
        adaptador.notifyDataSetChanged();
    }

    // ---- Ganancia por unidad en vivo ----

    private void configurarGanancia() {
        Watchers.alCambiar(b.etPrecioReal, t -> actualizarGanancia());
        Watchers.alCambiar(b.etPrecioVenta, t -> actualizarGanancia());
        actualizarGanancia();
    }

    private void actualizarGanancia() {
        if (b == null) {
            return;
        }
        CharSequence real = b.etPrecioReal.getText();
        CharSequence venta = b.etPrecioVenta.getText();
        if (real == null || real.length() == 0 || venta == null || venta.length() == 0) {
            b.tvGanancia.setText(R.string.form_ganancia_vacia);
            return;
        }
        long ganancia = Dinero.parsear(venta) - Dinero.parsear(real);
        b.tvGanancia.setText(getString(R.string.form_ganancia, Dinero.formato(ganancia)));
    }

    // ---- Fecha de vencimiento ----

    private void configurarFecha() {
        View.OnClickListener abrir = v -> elegirFecha();
        b.etVencimiento.setOnClickListener(abrir);
        b.tilVencimiento.setEndIconOnClickListener(abrir);
        b.btnQuitarFecha.setOnClickListener(v -> {
            vencimiento = null;
            mostrarFecha();
        });
        mostrarFecha();
    }

    private void elegirFecha() {
        MaterialDatePicker<Long> selector = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.hint_vencimiento)
                .setSelection(vencimiento != null ? vencimiento : Fechas.hoyUtc())
                .build();
        selector.addOnPositiveButtonClickListener(seleccion -> {
            vencimiento = seleccion;
            mostrarFecha();
        });
        selector.show(getChildFragmentManager(), "fecha_vencimiento");
    }

    private void mostrarFecha() {
        if (b == null) {
            return;
        }
        b.etVencimiento.setText(vencimiento == null ? "" : Fechas.fechaUtc(vencimiento));
        b.btnQuitarFecha.setVisibility(vencimiento == null ? View.GONE : View.VISIBLE);
    }

    // ---- Edición: cargar datos ----

    private void llenar(Producto p) {
        b.etNombre.setText(p.nombre);
        b.acCategoria.setText(p.categoria == null ? "" : p.categoria, false);
        b.acLinea.setText(p.linea == null ? "" : p.linea, false);
        b.acMarca.setText(p.marca == null ? "" : p.marca, false);
        b.etCodigo.setText(p.codigoBarras == null ? "" : p.codigoBarras);
        b.etPrecioReal.setText(String.valueOf(p.precioReal));
        b.etPrecioVenta.setText(String.valueOf(p.precioVenta));
        b.etStock.setText(String.valueOf(p.stock));
        vencimiento = p.fechaVencimiento;
        mostrarFecha();
    }

    // ---- Guardar ----

    private void guardar() {
        if (guardando) {
            return;
        }
        String nombre = b.etNombre.getText() == null ? "" : b.etNombre.getText().toString().trim();
        String categoria = Texto.vacioANulo(b.acCategoria.getText());
        CharSequence textoReal = b.etPrecioReal.getText();
        CharSequence textoVenta = b.etPrecioVenta.getText();
        CharSequence textoStock = b.etStock.getText();

        boolean valido = true;
        valido &= marcar(b.tilNombre, nombre.isEmpty(), R.string.err_obligatorio);
        valido &= marcar(b.tilCategoria, categoria == null, R.string.err_elige_categoria);
        valido &= marcar(b.tilPrecioReal, textoReal == null || textoReal.length() == 0, R.string.err_obligatorio);
        valido &= marcar(b.tilPrecioVenta,
                textoVenta == null || textoVenta.length() == 0 || Dinero.parsear(textoVenta) <= 0L,
                R.string.err_precio_venta);
        valido &= marcar(b.tilStock, textoStock == null || textoStock.length() == 0, R.string.err_obligatorio);
        if (!valido) {
            return;
        }

        Producto p = new Producto();
        if (original != null) {
            p.id = original.id;
            p.creadoEn = original.creadoEn;
        }
        p.nombre = nombre;
        p.categoria = categoria;
        p.linea = Texto.vacioANulo(b.acLinea.getText());
        p.marca = Texto.vacioANulo(b.acMarca.getText());
        p.codigoBarras = Texto.vacioANulo(b.etCodigo.getText());
        p.precioReal = Dinero.parsear(textoReal);
        p.precioVenta = Dinero.parsear(textoVenta);
        p.stock = Dinero.parsearEntero(textoStock);
        p.fechaVencimiento = vencimiento;

        guardando = true;
        b.btnGuardar.setEnabled(false);
        vm.guardarProducto(p, r -> {
            guardando = false;
            if (b == null) {
                return;
            }
            b.btnGuardar.setEnabled(true);
            if (r.exito) {
                Toast.makeText(requireContext(), R.string.msg_producto_guardado, Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).navigateUp();
            } else {
                Toast.makeText(requireContext(), r.mensaje, Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Muestra u oculta el error de un campo. Devuelve true si el campo está bien. */
    private boolean marcar(com.google.android.material.textfield.TextInputLayout campo, boolean hayError, int mensaje) {
        campo.setError(hayError ? getString(mensaje) : null);
        return !hayError;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle salida) {
        super.onSaveInstanceState(salida);
        if (vencimiento != null) {
            salida.putLong(CLAVE_VENCIMIENTO, vencimiento);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
