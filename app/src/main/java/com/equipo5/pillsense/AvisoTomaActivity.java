package com.equipo5.pillsense;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;


public class AvisoTomaActivity extends AppCompatActivity {


    public static final String EXTRA_ID_TOMA = "idToma";
    public static final String EXTRA_HORA = "hora";
    public static final String EXTRA_MEDICAMENTO = "medicamento";
    public static final String EXTRA_CAJON = "cajon";

    private static final String[] FRANJAS = {"Mañana", "Mediodía", "Tarde", "Noche"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aviso_toma);


        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        int margen = Math.round(24 * getResources().getDisplayMetrics().density);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left + margen, barras.top, barras.right + margen, barras.bottom);
            return insets;
        });

        // Datos de la toma
        String hora = getIntent().getStringExtra(EXTRA_HORA);
        String medicamento = getIntent().getStringExtra(EXTRA_MEDICAMENTO);
        int cajon = getIntent().getIntExtra(EXTRA_CAJON, 0);

        ((TextView) findViewById(R.id.tvHoraAviso)).setText(hora);
        ((TextView) findViewById(R.id.tvNumeroCajonAviso)).setText(String.valueOf(cajon));
        ((TextView) findViewById(R.id.tvAbreCajon)).setText("Abre el cajón " + cajon);
        ((TextView) findViewById(R.id.tvFranjaAviso))
                .setText((cajon >= 1 && cajon <= 4) ? FRANJAS[cajon - 1] : "");
        ((TextView) findViewById(R.id.tvMedicamentoAviso)).setText(medicamento);


    }
}