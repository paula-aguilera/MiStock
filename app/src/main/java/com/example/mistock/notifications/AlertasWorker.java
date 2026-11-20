package com.example.mistock.notifications;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.mistock.data.AppDatabase;
import com.example.mistock.data.Producto;
import com.example.mistock.util.AppConfig;
import com.example.mistock.util.Fechas;

import java.util.List;

/** Tarea diaria en segundo plano: revisa el inventario y avisa de stock bajo y vencimientos. */
public class AlertasWorker extends Worker {

    public AlertasWorker(@NonNull Context contexto, @NonNull WorkerParameters parametros) {
        super(contexto, parametros);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context contexto = getApplicationContext();
        AppDatabase db = AppDatabase.obtener(contexto);

        List<Producto> stockBajo = db.productoDao().obtenerStockBajo(AppConfig.UMBRAL_STOCK_BAJO);
        long limite = Fechas.hoyUtc() + AppConfig.DIAS_AVISO_VENCIMIENTO * Fechas.DIA_MS;
        List<Producto> porVencer = db.productoDao().obtenerPorVencer(limite);

        NotificationHelper.resumenDiario(contexto, stockBajo, porVencer);
        return Result.success();
    }
}
