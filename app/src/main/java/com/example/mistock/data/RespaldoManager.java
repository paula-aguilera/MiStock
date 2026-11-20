package com.example.mistock.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Respaldo manual de toda la información: copia el archivo de la base de datos a la carpeta
 * (Drive, Descargas, etc.) que elijas, y permite restaurarlo después.
 */
public final class RespaldoManager {

    private static final String CABECERA_SQLITE = "SQLite format 3\u0000";
    private static final String[] TABLAS = {"productos", "clientes", "ventas", "venta_items", "abonos"};

    private RespaldoManager() {
    }

    public static void exportar(Context contexto, Uri destino) throws IOException {
        AppDatabase db = AppDatabase.obtener(contexto);

        // Room trabaja en modo WAL: este comando pasa todo lo pendiente al archivo principal antes de copiarlo.
        Cursor cursor = db.getOpenHelper().getWritableDatabase().query("PRAGMA wal_checkpoint(FULL)");
        try {
            cursor.moveToFirst();
        } finally {
            cursor.close();
        }

        File archivo = contexto.getDatabasePath(AppDatabase.NOMBRE);
        try (InputStream entrada = new FileInputStream(archivo);
             OutputStream salida = contexto.getContentResolver().openOutputStream(destino)) {
            if (salida == null) {
                throw new IOException("No se pudo abrir el destino del respaldo.");
            }
            copiar(entrada, salida);
        }
    }

    public static void restaurar(Context contexto, Uri origen) throws IOException {
        File temporal = new File(contexto.getCacheDir(), "restaurar.tmp");
        try {
            try (InputStream entrada = contexto.getContentResolver().openInputStream(origen);
                 OutputStream salida = new FileOutputStream(temporal)) {
                if (entrada == null) {
                    throw new IOException("No se pudo abrir el archivo elegido.");
                }
                copiar(entrada, salida);
            }

            validar(temporal);

            AppDatabase.cerrar();
            File destino = contexto.getDatabasePath(AppDatabase.NOMBRE);
            new File(destino.getPath() + "-wal").delete();
            new File(destino.getPath() + "-shm").delete();
            try (InputStream entrada = new FileInputStream(temporal);
                 OutputStream salida = new FileOutputStream(destino, false)) {
                copiar(entrada, salida);
            }
        } finally {
            //noinspection ResultOfMethodCallIgnored
            temporal.delete();
        }
    }

    private static void validar(File archivo) throws IOException {
        byte[] cabecera = new byte[16];
        try (InputStream entrada = new FileInputStream(archivo)) {
            int leidos = 0;
            while (leidos < cabecera.length) {
                int n = entrada.read(cabecera, leidos, cabecera.length - leidos);
                if (n < 0) {
                    break;
                }
                leidos += n;
            }
            if (leidos < cabecera.length
                    || !CABECERA_SQLITE.equals(new String(cabecera, StandardCharsets.ISO_8859_1))) {
                throw new IOException("El archivo elegido no es un respaldo de MiStock.");
            }
        }

        SQLiteDatabase bd = null;
        try {
            bd = SQLiteDatabase.openDatabase(archivo.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            if (bd.getVersion() > AppDatabase.VERSION) {
                throw new IOException("Ese respaldo viene de una versión más nueva de la app.");
            }
            for (String tabla : TABLAS) {
                Cursor c = bd.rawQuery(
                        "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?", new String[]{tabla});
                try {
                    if (!c.moveToFirst()) {
                        throw new IOException("El archivo elegido no es un respaldo de MiStock.");
                    }
                } finally {
                    c.close();
                }
            }
        } catch (android.database.sqlite.SQLiteException e) {
            throw new IOException("El archivo elegido no es un respaldo válido.");
        } finally {
            if (bd != null) {
                bd.close();
            }
        }
    }

    private static void copiar(InputStream entrada, OutputStream salida) throws IOException {
        byte[] buffer = new byte[8192];
        int n;
        while ((n = entrada.read(buffer)) > 0) {
            salida.write(buffer, 0, n);
        }
        salida.flush();
    }
}
