package com.example.mistock;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.mistock.databinding.ActivityMainBinding;
import com.example.mistock.viewmodel.StockViewModel;
import com.google.android.material.badge.BadgeDrawable;

import java.util.HashSet;
import java.util.Set;

/**
 * Única actividad de la app: barra superior + contenedor de pantallas + barra inferior con 5 pestañas.
 */
public class MainActivity extends AppCompatActivity {

    /** Extra que usan las notificaciones para abrir directamente la pestaña Alertas. */
    public static final String EXTRA_DESTINO = "destino";
    public static final String DESTINO_ALERTAS = "alertas";

    private ActivityMainBinding binding;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private final Set<Integer> pestanas = new HashSet<>();

    private final ActivityResultLauncher<String> permisoNotificaciones =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                // Si no la acepta, la app funciona igual; solo no llegarán avisos al teléfono.
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.navHost);
        navController = host.getNavController();

        pestanas.add(R.id.inicioFragment);
        pestanas.add(R.id.productosFragment);
        pestanas.add(R.id.clientesFragment);
        pestanas.add(R.id.reportesFragment);
        pestanas.add(R.id.alertasFragment);

        appBarConfiguration = new AppBarConfiguration.Builder(pestanas).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.bottomNav, navController);

        // La barra inferior solo se ve en las 5 pestañas; en formularios y detalles se esconde.
        navController.addOnDestinationChangedListener((controller, destino, argumentos) ->
                binding.bottomNav.setVisibility(pestanas.contains(destino.getId()) ? View.VISIBLE : View.GONE));

        StockViewModel vm = new ViewModelProvider(this).get(StockViewModel.class);
        vm.totalAlertas().observe(this, total -> {
            if (total != null && total > 0) {
                BadgeDrawable insignia = binding.bottomNav.getOrCreateBadge(R.id.alertasFragment);
                insignia.setVisible(true);
                insignia.setNumber(total);
            } else {
                binding.bottomNav.removeBadge(R.id.alertasFragment);
            }
        });

        pedirPermisoNotificaciones();
        if (savedInstanceState == null) {
            manejarIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        manejarIntent(intent);
    }

    private void manejarIntent(Intent intent) {
        if (intent != null && DESTINO_ALERTAS.equals(intent.getStringExtra(EXTRA_DESTINO))) {
            intent.removeExtra(EXTRA_DESTINO);
            seleccionarTab(R.id.alertasFragment);
        }
    }

    /** Cambia a una de las 5 pestañas (lo usan las pantallas, por ejemplo el aviso de alertas en Inicio). */
    public void seleccionarTab(int idPestana) {
        binding.bottomNav.setSelectedItemId(idPestana);
    }

    private void pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
