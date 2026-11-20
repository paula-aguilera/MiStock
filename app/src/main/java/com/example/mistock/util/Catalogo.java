package com.example.mistock.util;

/**
 * Listas de apoyo para el formulario de productos, basadas en el catálogo oficial de Natura Chile.
 * Las categorías son una lista cerrada; las líneas y marcas son sugerencias (también aprende las que tú escribas).
 */
public final class Catalogo {

    public static final String[] CATEGORIAS = {
            "Perfumería",
            "Cuidado diario",
            "Rostro",
            "Cabello",
            "Maquillaje",
            "Regalos",
            "Infantil",
            "Hombres",
            "Protectores solares",
            "Repuestos",
            "Otros"
    };

    public static final String[] LINEAS = {
            "Kaiak", "Humor", "Essencial", "Luna", "Ekos",
            "Tododia", "Chronos", "Faces", "Lumina", "Una",
            "Naturé", "Mamá y Bebé", "Creer para Ver"
    };

    public static final String[] MARCAS = {"Natura"};

    private Catalogo() {
    }
}
