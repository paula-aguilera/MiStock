package com.example.mistock.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import com.example.mistock.data.Cliente;
import com.example.mistock.databinding.FragmentClienteDetalleBinding;
import com.example.mistock.util.Args;
import com.example.mistock.util.Dinero;
import com.example.mistock.viewmodel.StockViewModel;

/** Ficha del cliente: contacto, cuánto debe y su historial de compras (con notas y abonos en cada una). */
public class ClienteDetalleFragment extends Fragment {

    private FragmentClienteDetalleBinding b;
    private StockViewModel vm;
    private long clienteId;
    private Cliente actual;
    private boolean saliendo = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentClienteDetalleBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(StockViewModel.class);
        clienteId = Args.leer(this, Args.CLIENTE);

        VentaAdapter adaptador = new VentaAdapter(item -> NavHostFragment.findNavController(this)
                .navigate(R.id.ventaDetalleFragment, Args.de(Args.VENTA, item.venta.id)));
        b.rvVentas.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvVentas.setAdapter(adaptador);

        vm.cliente(clienteId).observe(getViewLifecycleOwner(), cliente -> {
            if (cliente == null) {
                if (!saliendo) {
                    saliendo = true;
                    NavHostFragment.findNavController(this).navigateUp();
                }
                return;
            }
            actual = cliente;
            mostrar(cliente);
        });
        vm.saldoCliente(clienteId).observe(getViewLifecycleOwner(), saldo ->
                b.tvSaldo.setText(Dinero.formato(saldo == null ? 0L : saldo)));
        vm.ventasDeCliente(clienteId).observe(getViewLifecycleOwner(), ventas -> {
            adaptador.submitList(ventas);
            b.tvSinCompras.setVisibility(ventas == null || ventas.isEmpty() ? View.VISIBLE : View.GONE);
        });

        b.btnNuevaVenta.setOnClickListener(v -> NavHostFragment.findNavController(this)
                .navigate(R.id.nuevaVentaFragment, Args.de(Args.CLIENTE, clienteId)));
        b.btnEditar.setOnClickListener(v -> {
            if (actual != null) {
                Dialogos.cliente(requireContext(), vm, actual, null);
            }
        });
        b.btnEliminar.setOnClickListener(v -> confirmarEliminar());
        b.btnLlamar.setOnClickListener(v -> {
            if (actual != null && actual.telefono != null) {
                abrir(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(actual.telefono))));
            }
        });
        b.btnCorreo.setOnClickListener(v -> {
            if (actual != null && actual.correo != null) {
                abrir(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + Uri.encode(actual.correo))));
            }
        });
    }

    private void mostrar(Cliente c) {
        b.tvNombre.setText(c.nombre);

        boolean tieneTelefono = c.telefono != null;
        b.tvTelefono.setText(tieneTelefono ? c.telefono : getString(R.string.sin_telefono));
        b.btnLlamar.setVisibility(tieneTelefono ? View.VISIBLE : View.GONE);

        boolean tieneCorreo = c.correo != null;
        b.tvCorreo.setText(tieneCorreo ? c.correo : getString(R.string.sin_correo));
        b.btnCorreo.setVisibility(tieneCorreo ? View.VISIBLE : View.GONE);

        if (c.notas == null) {
            b.tvNotas.setVisibility(View.GONE);
        } else {
            b.tvNotas.setText(c.notas);
            b.tvNotas.setVisibility(View.VISIBLE);
        }
    }

    private void abrir(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.err_sin_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmarEliminar() {
        if (actual == null) {
            return;
        }
        final Cliente cliente = actual;
        Dialogos.confirmar(requireContext(),
                getString(R.string.eliminar_cliente_titulo),
                getString(R.string.eliminar_cliente_msg, cliente.nombre),
                getString(R.string.btn_eliminar),
                () -> {
                    final Context app = requireContext().getApplicationContext();
                    vm.eliminarCliente(cliente, r -> Toast.makeText(app,
                            r.exito ? app.getString(R.string.msg_cliente_eliminado) : r.mensaje,
                            r.exito ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show());
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
