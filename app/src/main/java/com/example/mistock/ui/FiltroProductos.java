package com.example.mistock.ui;

import com.example.mistock.data.Producto;
import com.example.mistock.util.Texto;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtra la lista de productos por texto y por categoría.
 *
 * Cómo busca (sin distinguir mayúsculas ni tildes):
 * - Se pueden escribir varias palabras: el producto debe coincidir con todas ("kaiak mujer").
 * - Con 3 letras o más, la palabra se busca en cualquier parte del nombre, marca, línea, categoría o código
 *   (por ejemplo "kai" o "iak" encuentran Kaiak).
 * - Con 1 o 2 letras solo cuentan las palabras que empiezan así, para no llenar la lista de resultados de más.
 */
public final class FiltroProductos {

    /** Desde esta cantidad de letras la búsqueda encuentra la palabra en cualquier parte. */
    public static final int LETRAS_MINIMAS_BUSQUEDA_LIBRE = 3;

    private FiltroProductos() {
    }

    /** Devuelve siempre una lista nueva (el adaptador necesita una instancia distinta para detectar cambios). */
    public static List<Producto> filtrar(List<Producto> origen, String consulta, String categoria) {
        List<Producto> resultado = new ArrayList<>();
        if (origen == null) {
            return resultado;
        }
        String q = Texto.normalizar(consulta);
        String[] palabras = q.isEmpty() ? new String[0] : q.split("\\s+");

        for (Producto p : origen) {
            if (categoria != null && !categoria.equals(p.categoria)) {
                continue;
            }
            if (palabras.length > 0) {
                String texto = Texto.normalizar(p.nombre + " " + nz(p.marca) + " " + nz(p.linea) + " "
                        + nz(p.categoria) + " " + nz(p.codigoBarras));
                boolean coincide = true;
                for (String palabra : palabras) {
                    if (!coincidePalabra(texto, palabra)) {
                        coincide = false;
                        break;
                    }
                }
                if (!coincide) {
                    continue;
                }
            }
            resultado.add(p);
        }
        return resultado;
    }

    private static boolean coincidePalabra(String texto, String palabra) {
        if (palabra.length() >= LETRAS_MINIMAS_BUSQUEDA_LIBRE) {
            return texto.contains(palabra);
        }
        return texto.startsWith(palabra) || texto.contains(" " + palabra);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
