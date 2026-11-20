package com.example.mistock.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.mistock.MainActivity;
import com.example.mistock.R;
import com.example.mistock.data.Producto;
import com.example.mistock.util.AppConfig;
import com.example.mistock.util.Fechas;

import java.util.List;

/** Crea y muestra las notificaciones de MiStock (stock bajo y vencimientos). Todo es local, sin internet. */
public final class NotificationHelper {

    private static final int ID_RESUMEN_STOCK = 1;
    private static final int ID_RESUMEN_VENCIMIENTO = 2;
    private static final int BASE_ID_PRODUCTO = 1000;

    private NotificationHelper() {
    }

    public static void crearCanal(Context contexto) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    AppConfig.CANAL_ALERTAS,
                    contexto.getString(R.string.canal_alertas_nombre),
                    NotificationManager.IMPORTANCE_DEFAULT);
            canal.setDescription(contexto.getString(R.string.canal_alertas_descripcion));
            NotificationManager nm = contexto.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(canal);
            }
        }
    }

    /** Aviso inmediato cuando una venta deja el producto con 2 unidades o menos. */
    public static void avisarStockBajo(Context contexto, Producto producto) {
        String titulo;
        String texto;
        if (producto.stock <= 0) {
            titulo = contexto.getString(R.string.notif_agotado_titulo);
            texto = contexto.getString(R.string.notif_agotado_texto, producto.nombre);
        } else {
            titulo = contexto.getString(R.string.notif_stock_bajo_titulo);
            texto = producto.stock == 1
                    ? contexto.getString(R.string.notif_queda_una, producto.nombre)
                    : contexto.getString(R.string.notif_quedan, producto.stock, producto.nombre);
        }
        int id = BASE_ID_PRODUCTO + (int) (producto.id % 50000L);
        mostrar(contexto, id, titulo, texto, texto);
    }

    /** Resumen diario (lo llama AlertasWorker). Si no hay nada que avisar, no molesta. */
    public static void resumenDiario(Context contexto, List<Producto> stockBajo, List<Producto> porVencer) {
        if (stockBajo != null && !stockBajo.isEmpty()) {
            StringBuilder detalle = new StringBuilder();
            for (Producto p : stockBajo) {
                if (detalle.length() > 0) {
                    detalle.append('\n');
                }
                detalle.append(contexto.getString(R.string.notif_linea_stock, p.nombre, p.stock));
            }
            String titulo = contexto.getString(R.string.notif_resumen_stock_titulo, stockBajo.size());
            String corto = stockBajo.get(0).nombre
                    + (stockBajo.size() > 1 ? contexto.getString(R.string.notif_y_mas, stockBajo.size() - 1) : "");
            mostrar(contexto, ID_RESUMEN_STOCK, titulo, corto, detalle.toString());
        }

        if (porVencer != null && !porVencer.isEmpty()) {
            StringBuilder detalle = new StringBuilder();
            for (Producto p : porVencer) {
                if (detalle.length() > 0) {
                    detalle.append('\n');
                }
                detalle.append(contexto.getString(R.string.notif_linea_vencimiento,
                        p.nombre, Fechas.textoVencimiento(p.fechaVencimiento)));
            }
            String titulo = contexto.getString(R.string.notif_resumen_vencimiento_titulo, porVencer.size());
            String corto = porVencer.get(0).nombre
                    + (porVencer.size() > 1 ? contexto.getString(R.string.notif_y_mas, porVencer.size() - 1) : "");
            mostrar(contexto, ID_RESUMEN_VENCIMIENTO, titulo, corto, detalle.toString());
        }
    }

    private static boolean puedeNotificar(Context contexto) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(contexto).areNotificationsEnabled();
    }

    private static PendingIntent abrirAlertas(Context contexto) {
        Intent intent = new Intent(contexto, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_DESTINO, MainActivity.DESTINO_ALERTAS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(contexto, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void mostrar(Context contexto, int id, String titulo, String texto, String detalle) {
        if (!puedeNotificar(contexto)) {
            return;
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(contexto, AppConfig.CANAL_ALERTAS)
                .setSmallIcon(R.drawable.ic_notifications)
                .setColor(ContextCompat.getColor(contexto, R.color.rosa_primario))
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(detalle))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(abrirAlertas(contexto))
                .setAutoCancel(true);
        try {
            NotificationManagerCompat.from(contexto).notify(id, builder.build());
        } catch (SecurityException ignorada) {
            // Sin permiso de notificaciones: la app sigue funcionando, solo no avisa.
        }
    }
}
