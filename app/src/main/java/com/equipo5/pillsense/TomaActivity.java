package com.equipo5.pillsense;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;


public class TomaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BarraAcciones.prepararVentana(this);
        setContentView(R.layout.activity_toma);
        BarraAcciones.configurar(this, R.string.toma_titulo_nueva, true, 0);
    }
}