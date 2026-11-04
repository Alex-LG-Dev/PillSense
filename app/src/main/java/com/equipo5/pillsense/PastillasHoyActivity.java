package com.equipo5.pillsense;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.ImageViewCompat;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Pantalla del paciente con todas las pastillas del día, ordenadas por cajón. */
public class PastillasHoyActivity extends AppCompatActivity {

    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    // Nombre de cada cajón: 1 Mañana, 2 Mediodía, 3 Tarde, 4 Noche
    private static final String[] FRANJAS = {"Mañana", "Mediodía", "Tarde", "Noche"};

    private TextView tvProgreso;
    private LinearProgressIndicator barraProgreso;
    private LinearLayout listaCajones;
    private ScrollView scrollCajones;

    // Tomas:
    private DatabaseReference refTomas;
    private ValueEventListener escuchadorTomas;
    private final List<Toma> tomasDeHoy = new ArrayList<>();

    // Tomadas hoy:
    private DatabaseReference refRegistrosHoy;
    private ValueEventListener escuchadorRegistros;
    private final Set<String> tomadasHoy = new HashSet<>();

    // Para bajar solo UNA vez hasta el cajón de AHORA, cuando ya han llegado los datos
    private boolean tomasCargadas = false;
    private boolean registrosCargados = false;
    private boolean yaBajado = false;

    // Cada minuto se repinta, para que AHORA y "No tomada" cambien solos con la hora
    private final Handler reloj = new Handler(Looper.getMainLooper());
    private final Runnable cadaMinuto = new Runnable() {
        @Override
        public void run() {
            mostrarCajones();
            reloj.postDelayed(this, 60_000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pastillas_hoy);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        // Flecha de volver
        ImageButton btnVolver = findViewById(R.id.btnVolver);
        btnVolver.setOnClickListener(v -> finish());

        // "Pedir ayuda" abre la pantalla para llamar a la cuidadora
        findViewById(R.id.btnPedirAyuda).setOnClickListener(v ->
                startActivity(new Intent(this, PedirAyudaActivity.class)));

        tvProgreso = findViewById(R.id.tvProgreso);
        barraProgreso = findViewById(R.id.barraProgreso);
        listaCajones = findViewById(R.id.listaCajones);
        scrollCajones = findViewById(R.id.scrollCajones);


        tvProgreso.setText("Cargando…");
        barraProgreso.setVisibility(View.INVISIBLE);
        listaCajones.removeAllViews();

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) return;

        refTomas = FirebaseDatabase.getInstance(DB_URL)
                .getReference("tomas").child(usuario.getUid());

        // Se ejecuta al abrir la pantalla y cada vez que cambia una toma
        escuchadorTomas = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot datos) {
                tomasDeHoy.clear();
                for (DataSnapshot hijo : datos.getChildren()) {
                    Toma toma = hijo.getValue(Toma.class);
                    if (toma != null && toma.getHora() != null) {
                        toma.setId(hijo.getKey());
                        tomasDeHoy.add(toma);
                    }
                }
                // De la más temprana a la más tardía
                Collections.sort(tomasDeHoy, (a, b) -> a.getHora().compareTo(b.getHora()));
                tomasCargadas = true;
                mostrarCajones();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PastillasHoyActivity.this,
                        "No se han podido cargar tus pastillas", Toast.LENGTH_LONG).show();
            }
        };

        // Se ejecuta cada vez que se marca (o se borra) una toma como tomada hoy
        escuchadorRegistros = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot datos) {
                tomadasHoy.clear();
                for (DataSnapshot hijo : datos.getChildren()) {
                    tomadasHoy.add(hijo.getKey());   // la clave es el id de la toma
                }
                registrosCargados = true;
                mostrarCajones();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (refTomas != null) refTomas.addValueEventListener(escuchadorTomas);
        reloj.postDelayed(cadaMinuto, 60_000);

        // Registros de HOY (la fecha se calcula cada vez que se abre la pantalla)
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario != null) {
            String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            refRegistrosHoy = FirebaseDatabase.getInstance(DB_URL)
                    .getReference("registros").child(usuario.getUid()).child(hoy);
            refRegistrosHoy.addValueEventListener(escuchadorRegistros);
        }
    }

    @Override
    protected void onStop() {
        if (refTomas != null) refTomas.removeEventListener(escuchadorTomas);
        if (refRegistrosHoy != null) refRegistrosHoy.removeEventListener(escuchadorRegistros);
        reloj.removeCallbacks(cadaMinuto);
        super.onStop();
    }


    private void mostrarCajones() {
        String ahora = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());


        int total = tomasDeHoy.size();
        int tomadas = 0;
        for (Toma toma : tomasDeHoy) {
            if (tomadasHoy.contains(toma.getId())) tomadas++;
        }
        if (total == 0) {
            tvProgreso.setText("Hoy no tienes pastillas");
            barraProgreso.setVisibility(View.INVISIBLE);
        } else {
            tvProgreso.setText("Llevas " + tomadas + " de " + total);
            barraProgreso.setVisibility(View.VISIBLE);
            barraProgreso.setMax(total);
            barraProgreso.setProgress(tomadas);
        }


        int cajonAhora = 0;
        for (Toma toma : tomasDeHoy) {
            boolean yaTomada = tomadasHoy.contains(toma.getId());
            boolean yaEsLaHora = toma.getHora().compareTo(ahora) <= 0;
            if (!yaTomada && yaEsLaHora && !estaOlvidada(toma, ahora)) {
                cajonAhora = toma.getCajon();
                break;
            }
        }

        // ===== Las 4 tarjetas =====
        listaCajones.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();

        for (int cajon = 1; cajon <= 4; cajon++) {
            View tarjeta = inflater.inflate(R.layout.item_cajon_paciente, listaCajones, false);
            ((TextView) tarjeta.findViewById(R.id.tvNumeroCajon)).setText(String.valueOf(cajon));
            ((TextView) tarjeta.findViewById(R.id.tvNombreCajon))
                    .setText("Cajón " + cajon + " · " + FRANJAS[cajon - 1]);


            if (cajon == cajonAhora) {
                tarjeta.setBackgroundResource(R.drawable.bg_cajon_ahora);
                tarjeta.findViewById(R.id.tvChipAhora).setVisibility(View.VISIBLE);
            }


            LinearLayout listaTomas = tarjeta.findViewById(R.id.listaTomasCajon);
            listaTomas.removeAllViews();
            boolean tieneTomas = false;

            for (Toma toma : tomasDeHoy) {
                if (toma.getCajon() != cajon) continue;
                tieneTomas = true;

                View fila = inflater.inflate(R.layout.item_toma_cajon_paciente, listaTomas, false);
                ((TextView) fila.findViewById(R.id.tvHora)).setText(toma.getHora());
                ((TextView) fila.findViewById(R.id.tvMedicamento)).setText(toma.getNombreMedicamento());

                ImageView ivEstado = fila.findViewById(R.id.ivEstado);
                if (tomadasHoy.contains(toma.getId())) {
                    ponerEstado(ivEstado, R.drawable.ic_toma_hecha, R.color.verde_bosque, "Tomada");
                } else if (estaOlvidada(toma, ahora)) {
                    ponerEstado(ivEstado, R.drawable.ic_toma_olvidada, R.color.texto_secundario, "No tomada");
                    fila.setAlpha(0.6f);   // más clarita: ya no se puede tomar
                } else {
                    ponerEstado(ivEstado, R.drawable.ic_toma_pendiente, R.color.texto_secundario, "Pendiente");
                }
                listaTomas.addView(fila);
            }

            // Cajón vacío
            if (!tieneTomas) {
                TextView tvVacio = new TextView(this);
                tvVacio.setText("Sin pastillas");
                tvVacio.setTextSize(17);
                tvVacio.setTextColor(ContextCompat.getColor(this, R.color.texto_secundario));
                tvVacio.setPadding(0, 0, 0, dp(10));
                listaTomas.addView(tvVacio);
            }

            listaCajones.addView(tarjeta);
        }
    }

    /** Una toma está olvidada si ya ha pasado su hora límite (y no se ha tomado). */
    private boolean estaOlvidada(Toma toma, String ahora) {
        String limite = (toma.getHoraLimite() != null && !toma.getHoraLimite().isEmpty())
                ? toma.getHoraLimite()
                : toma.getHora();
        return limite.compareTo(ahora) < 0;
    }


    private void ponerEstado(ImageView iv, int icono, int color, String descripcion) {
        iv.setImageResource(icono);
        ImageViewCompat.setImageTintList(iv,
                ColorStateList.valueOf(ContextCompat.getColor(this, color)));
        iv.setContentDescription(descripcion);
    }


    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }
}