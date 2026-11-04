package com.equipo5.pillsense;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class AvisoTomaActivity extends AppCompatActivity {


    public static final String EXTRA_ID_TOMA = "idToma";
    public static final String EXTRA_HORA = "hora";
    public static final String EXTRA_MEDICAMENTO = "medicamento";
    public static final String EXTRA_CAJON = "cajon";

    private static final String[] FRANJAS = {"Mañana", "Mediodía", "Tarde", "Noche"};

    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    // Datos de la toma (se rellenan en onCreate y los usan los botones)
    private String idToma, hora, medicamento;
    private int cajon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Como un despertador: se ve encima de la pantalla de bloqueo y enciende la pantalla
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
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
        idToma = getIntent().getStringExtra(EXTRA_ID_TOMA);
        hora = getIntent().getStringExtra(EXTRA_HORA);
        medicamento = getIntent().getStringExtra(EXTRA_MEDICAMENTO);
        cajon = getIntent().getIntExtra(EXTRA_CAJON, 0);

        ((TextView) findViewById(R.id.tvHoraAviso)).setText(hora);
        ((TextView) findViewById(R.id.tvNumeroCajonAviso)).setText(String.valueOf(cajon));
        ((TextView) findViewById(R.id.tvAbreCajon)).setText("Abre el cajón " + cajon);
        ((TextView) findViewById(R.id.tvFranjaAviso))
                .setText((cajon >= 1 && cajon <= 4) ? FRANJAS[cajon - 1] : "");
        ((TextView) findViewById(R.id.tvMedicamentoAviso)).setText(medicamento);

        Button btnTomada = findViewById(R.id.btnTomadaAviso);
        Button btnRecordar = findViewById(R.id.btnRecordarLuego);

        // ¿Ya es la hora? (igual que en Inicio: antes de la hora no se puede marcar como tomada)
        String ahora = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        boolean yaEsLaHora = hora != null && hora.compareTo(ahora) <= 0;

        if (yaEsLaHora) {
            btnTomada.setOnClickListener(v -> marcarComoTomada());
            btnRecordar.setOnClickListener(v -> recordarLuego());
        } else {
            // Todavía no es la hora: solo informamos
            ((TextView) findViewById(R.id.tvEsHora)).setText("Tu próxima pastilla");
            btnTomada.setVisibility(View.GONE);
            btnRecordar.setText("Volver");
            btnRecordar.setOnClickListener(v -> finish());
        }


    }
    private void marcarComoTomada() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null || idToma == null) return;

        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Map<String, Object> registro = new HashMap<>();
        registro.put("estado", "tomada");
        registro.put("horaReal", new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));

        FirebaseDatabase.getInstance(DB_URL)
                .getReference("registros").child(usuario.getUid()).child(hoy).child(idToma)
                .setValue(registro)
                .addOnFailureListener(e -> Toast.makeText(this,
                        "No se ha podido guardar. Inténtalo otra vez", Toast.LENGTH_LONG).show());

        // Ya está tomada: fuera el aviso de "10 minutos" y la notificación que haya en pantalla
        AlarmasToma.cancelarPospuesta(this, idToma);
        NotificationManagerCompat.from(this).cancel(idToma.hashCode());


        Intent intent = new Intent(this, TomaRegistradaActivity.class);
        intent.putExtra(TomaRegistradaActivity.EXTRA_MEDICAMENTO, medicamento);
        intent.putExtra(TomaRegistradaActivity.EXTRA_HORA, hora);
        startActivity(intent);
        finish();
    }

    private void recordarLuego() {
        AlarmasToma.posponer(this, idToma, hora, medicamento, cajon, 10);
        NotificationManagerCompat.from(this).cancel(idToma.hashCode());
        Toast.makeText(this, "Te avisaremos en 10 minutos", Toast.LENGTH_LONG).show();
        finish();
    }
}