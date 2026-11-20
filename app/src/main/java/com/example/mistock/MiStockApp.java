package com.example.mistock;

import android.app.Application;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.mistock.notifications.AlertasWorker;
import com.example.mistock.notifications.NotificationHelper;
import com.example.mistock.util.AppConfig;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MiStockApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.crearCanal(this);
        programarAlertasDiarias();
    }

    /**
     * Programa la revisión diaria de alertas (aprox. a las 9:00). Con KEEP no se duplica
     * aunque se abra la app muchas veces; Android decide la hora exacta para cuidar la batería.
     */
    private void programarAlertasDiarias() {
        Calendar ahora = Calendar.getInstance();
        Calendar proxima = Calendar.getInstance();
        proxima.set(Calendar.HOUR_OF_DAY, AppConfig.HORA_AVISO);
        proxima.set(Calendar.MINUTE, 0);
        proxima.set(Calendar.SECOND, 0);
        proxima.set(Calendar.MILLISECOND, 0);
        if (!proxima.after(ahora)) {
            proxima.add(Calendar.DAY_OF_YEAR, 1);
        }
        long retrasoMs = proxima.getTimeInMillis() - ahora.getTimeInMillis();

        PeriodicWorkRequest solicitud = new PeriodicWorkRequest.Builder(AlertasWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(retrasoMs, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                AppConfig.TRABAJO_ALERTAS_DIARIAS, ExistingPeriodicWorkPolicy.KEEP, solicitud);
    }
}
