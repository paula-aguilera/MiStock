package com.example.mistock.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import androidx.core.util.Consumer;

/** Atajo para reaccionar cuando cambia el texto de un campo. */
public final class Watchers {

    private Watchers() {
    }

    public static void alCambiar(TextView campo, Consumer<String> accion) {
        campo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                accion.accept(s == null ? "" : s.toString());
            }
        });
    }
}
