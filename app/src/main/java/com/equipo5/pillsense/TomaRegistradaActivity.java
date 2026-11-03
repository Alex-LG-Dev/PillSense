package com.equipo5.pillsense;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class TomaRegistradaActivity extends AppCompatActivity {

    // Datos que le pasa PacienteActivity al abrirla
    public static final String EXTRA_NOMBRE = "nombre";
    public static final String EXTRA_MEDICAMENTO = "medicamento";
    public static final String EXTRA_HORA = "hora";
    public static final String EXTRA_PROXIMA_HORA = "proximaHora";
    public static final String EXTRA_PROXIMA_MEDICAMENTO = "proximaMedicamento";
    public static final String EXTRA_PROXIMO_CAJON = "proximoCajon";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toma_registrada);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left + 24, barras.top + 24, barras.right + 24, barras.bottom + 24);
            return insets;
        });

        String nombre = getIntent().getStringExtra(EXTRA_NOMBRE);
        String medicamento = getIntent().getStringExtra(EXTRA_MEDICAMENTO);
        String hora = getIntent().getStringExtra(EXTRA_HORA);
        String proximaHora = getIntent().getStringExtra(EXTRA_PROXIMA_HORA);
        String proximaMedicamento = getIntent().getStringExtra(EXTRA_PROXIMA_MEDICAMENTO);
        int proximoCajon = getIntent().getIntExtra(EXTRA_PROXIMO_CAJON, 0);

        // "¡Muy bien, Carmen!" (o solo "¡Muy bien!" si no sabemos el nombre)
        TextView tvMuyBien = findViewById(R.id.tvMuyBien);
        tvMuyBien.setText(nombre != null && !nombre.isEmpty()
                ? "¡Muy bien, " + nombre + "!"
                : "¡Muy bien!");

        // "Has tomado Perejil / de las 16:00"
        TextView tvDetalle = findViewById(R.id.tvDetalle);
        tvDetalle.setText("Has tomado " + medicamento + "\nde las " + hora);

        // Próxima pastilla: si no hay, escondemos la tarjeta
        View tarjetaProxima = findViewById(R.id.tarjetaProxima);
        if (proximaHora == null) {
            tarjetaProxima.setVisibility(View.GONE);
        } else {
            ((TextView) findViewById(R.id.tvProximaHora)).setText(proximaHora);
            ((TextView) findViewById(R.id.tvProximaMedicamento)).setText(proximaMedicamento);
            ((TextView) findViewById(R.id.tvProximoCajon)).setText("Cajón " + proximoCajon);
        }

        // Volver al inicio
        Button btnVolverInicio = findViewById(R.id.btnVolverInicio);
        btnVolverInicio.setOnClickListener(v -> finish());
    }
}