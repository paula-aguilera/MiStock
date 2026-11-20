package com.example.mistock.ui;

import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.mistock.R;

/** Pequeñas etiquetas de colores (verde = bien, ámbar = atención, rojo = urgente, gris = neutro). */
public final class Insignias {

    public static final int NEUTRO = 0;
    public static final int OK = 1;
    public static final int AVISO = 2;
    public static final int PELIGRO = 3;

    private Insignias() {
    }

    public static void aplicar(TextView vista, int nivel, CharSequence texto) {
        int fondo;
        int colorTexto;
        switch (nivel) {
            case OK:
                fondo = R.drawable.bg_badge_ok;
                colorTexto = R.color.verde_texto;
                break;
            case AVISO:
                fondo = R.drawable.bg_badge_aviso;
                colorTexto = R.color.ambar_texto;
                break;
            case PELIGRO:
                fondo = R.drawable.bg_badge_peligro;
                colorTexto = R.color.rojo_texto;
                break;
            default:
                fondo = R.drawable.bg_badge_neutro;
                colorTexto = R.color.neutro_texto;
                break;
        }
        vista.setText(texto);
        vista.setBackgroundResource(fondo);
        vista.setTextColor(ContextCompat.getColor(vista.getContext(), colorTexto));
        vista.setVisibility(View.VISIBLE);
    }
}
