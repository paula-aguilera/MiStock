package com.example.mistock.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/** Base de datos local de MiStock (archivo mistock.db dentro del teléfono). */
@Database(
        entities = {Producto.class, Cliente.class, Venta.class, VentaItem.class, Abono.class},
        version = 2,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public static final String NOMBRE = "mistock.db";

    /** Mantener igual al valor de "version" de arriba (se usa para validar respaldos). */
    public static final int VERSION = 2;

    private static volatile AppDatabase instancia;

    /** Versión 2: se agrega el código de barras a los productos (los datos existentes se conservan). */
    static final Migration MIGRACION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE productos ADD COLUMN codigoBarras TEXT");
        }
    };

    public abstract ProductoDao productoDao();

    public abstract ClienteDao clienteDao();

    public abstract VentaDao ventaDao();

    public static AppDatabase obtener(Context contexto) {
        if (instancia == null) {
            synchronized (AppDatabase.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(
                            contexto.getApplicationContext(), AppDatabase.class, NOMBRE)
                            .addMigrations(MIGRACION_1_2)
                            .build();
                }
            }
        }
        return instancia;
    }

    /** Cierra la base (se usa solo al restaurar un respaldo). */
    public static synchronized void cerrar() {
        if (instancia != null) {
            instancia.close();
            instancia = null;
        }
    }
}
