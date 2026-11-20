package com.example.mistock.data;

/** Resultado de una operación asíncrona: o salió bien (con un valor) o falló (con un mensaje para mostrar). */
public final class Resultado<T> {

    public final boolean exito;
    public final T valor;
    public final String mensaje;

    private Resultado(boolean exito, T valor, String mensaje) {
        this.exito = exito;
        this.valor = valor;
        this.mensaje = mensaje;
    }

    public static <T> Resultado<T> ok(T valor) {
        return new Resultado<>(true, valor, null);
    }

    public static <T> Resultado<T> error(String mensaje) {
        return new Resultado<>(false, null, mensaje);
    }
}
