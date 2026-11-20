package com.example.mistock.util;

import android.content.Context;
import android.widget.Toast;

import com.example.mistock.R;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

/**
 * Lector de códigos de barras y QR usando el escáner de Google (viene con los servicios de Google Play,
 * no necesita permiso de cámara). La primera vez puede descargar el módulo y necesita internet.
 */
public final class Escaner {

    public interface AlLeer {
        void codigo(String valor);
    }

    private Escaner() {
    }

    /** Abre la cámara; cuando lee un código llama a {@code alLeer} (en el hilo principal). Si cancelas, no pasa nada. */
    public static void escanear(Context contexto, AlLeer alLeer) {
        final Context app = contexto.getApplicationContext();
        GmsBarcodeScanner escaner = GmsBarcodeScanning.getClient(contexto);
        escaner.startScan()
                .addOnSuccessListener(barcode -> {
                    String valor = barcode.getRawValue();
                    if (valor != null && !valor.trim().isEmpty()) {
                        alLeer.codigo(valor.trim());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(app, R.string.escaner_error, Toast.LENGTH_LONG).show());
    }
}
