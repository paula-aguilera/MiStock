package com.example.mistock.data;

/** Se lanza cuando una operación incumple una regla del negocio; el mensaje se muestra tal cual a la usuaria. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
