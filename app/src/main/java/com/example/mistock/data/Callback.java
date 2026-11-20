package com.example.mistock.data;

/** Se llama siempre en el hilo principal, cuando la operación terminó. */
public interface Callback<T> {
    void alTerminar(Resultado<T> resultado);
}
