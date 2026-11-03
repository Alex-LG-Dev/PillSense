package com.equipo5.pillsense;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;


public class PacienteActivity extends AppCompatActivity {

    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    private TextView tvSaludo;
    // Nombre de cada cajón: 1 Mañana, 2 Mediodía, 3 Tarde, 4 Noche
    private static final String[] FRANJAS = {"Mañana", "Mediodía", "Tarde", "Noche"};

    // Tarjeta "Ahora te toca"
    private TextView tvAhoraHora, tvAhoraMedicamento, tvAhoraDosis;
    private TextView tvAhoraCajonNumero, tvAhoraCajon, tvAhoraFranja, tvTituloDespues;
    private View cajaCajon, btnTomada;
    private android.widget.LinearLayout listaDespues;

    // Tomas de Firebase
    private DatabaseReference refTomas;
    private ValueEventListener escuchador;
    private final List<Toma> tomasDeHoy = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paciente);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        Button btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());

        // Saludo con el nombre y fecha de hoy
        tvSaludo = findViewById(R.id.tvSaludo);
        TextView tvFecha = findViewById(R.id.tvFecha);
        tvFecha.setText(fechaDeHoy());
        cargarNombre();

        //Tarjeta "Ahora te toca" y lista "Después"
        tvAhoraHora = findViewById(R.id.tvAhoraHora);
        tvAhoraMedicamento = findViewById(R.id.tvAhoraMedicamento);
        tvAhoraDosis = findViewById(R.id.tvAhoraDosis);
        tvAhoraCajonNumero = findViewById(R.id.tvAhoraCajonNumero);
        tvAhoraCajon = findViewById(R.id.tvAhoraCajon);
        tvAhoraFranja = findViewById(R.id.tvAhoraFranja);
        tvTituloDespues = findViewById(R.id.tvTituloDespues);
        cajaCajon = findViewById(R.id.cajaCajon);
        btnTomada = findViewById(R.id.btnTomada);
        listaDespues = findViewById(R.id.listaDespues);


        tvAhoraDosis.setVisibility(View.GONE);   // la toma no guarda la dosis
        tvAhoraHora.setText("");
        tvAhoraMedicamento.setText("Cargando…");
        cajaCajon.setVisibility(View.GONE);
        btnTomada.setVisibility(View.GONE);
        tvTituloDespues.setVisibility(View.GONE);
        listaDespues.removeAllViews();

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario != null) {
            refTomas = FirebaseDatabase.getInstance(DB_URL)
                    .getReference("tomas").child(usuario.getUid());

            // Se ejecuta al abrir la pantalla y cada vez que cambia una toma en Firebase
            escuchador = new ValueEventListener() {
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
                    mostrarTomas();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(PacienteActivity.this,
                            "No se han podido cargar tus pastillas", Toast.LENGTH_LONG).show();
                }
            };
        }

    }
    @Override
    protected void onStart() {
        super.onStart();
        // Empieza a escuchar Firebase al ver la pantalla
        if (refTomas != null) refTomas.addValueEventListener(escuchador);
    }

    @Override
    protected void onStop() {
        // Y para al salir, para no gastar batería
        if (refTomas != null) refTomas.removeEventListener(escuchador);
        super.onStop();
    }


    private void mostrarTomas() {
        String ahora = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        // Pendientes: las que todavía no han pasado su hora límite
        List<Toma> pendientes = new ArrayList<>();
        for (Toma toma : tomasDeHoy) {
            String limite = (toma.getHoraLimite() != null && !toma.getHoraLimite().isEmpty())
                    ? toma.getHoraLimite()
                    : toma.getHora();
            if (limite.compareTo(ahora) >= 0) {
                pendientes.add(toma);
            }
        }

        // No queda ninguna
        if (pendientes.isEmpty()) {
            tvAhoraHora.setText("HOY");
            tvAhoraMedicamento.setText("¡Ya has terminado por hoy!");
            cajaCajon.setVisibility(View.GONE);
            btnTomada.setVisibility(View.GONE);
            return;
        }

        // La primera pendiente va en la tarjeta grande
        Toma primera = pendientes.get(0);
        boolean yaEsLaHora = primera.getHora().compareTo(ahora) <= 0;

        tvAhoraHora.setText((yaEsLaHora ? "AHORA TE TOCA · " : "TU PRÓXIMA PASTILLA · ")
                + primera.getHora());
        tvAhoraMedicamento.setText(primera.getNombreMedicamento());
        tvAhoraCajonNumero.setText(String.valueOf(primera.getCajon()));
        tvAhoraCajon.setText("Cajón " + primera.getCajon());
        tvAhoraFranja.setText(franja(primera.getCajon()));
        cajaCajon.setVisibility(View.VISIBLE);

        // Solo se puede marcar como tomada cuando ya es la hora
        btnTomada.setVisibility(yaEsLaHora ? View.VISIBLE : View.GONE);
    }

    /** Nombre del cajón: */
    private String franja(int cajon) {
        return (cajon >= 1 && cajon <= 4) ? FRANJAS[cajon - 1] : "";
    }



    private String fechaDeHoy() {
        String fecha = new SimpleDateFormat("EEEE, d 'de' MMMM", new Locale("es", "ES"))
                .format(new Date());
        // Primera letra en mayúscula: "domingo" → "Domingo"
        return fecha.substring(0, 1).toUpperCase() + fecha.substring(1);
    }


    private void cargarNombre() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) return;


        ponerSaludo(usuario.getDisplayName());

        FirebaseDatabase.getInstance(DB_URL)
                .getReference("usuarios").child(usuario.getUid()).child("nombre")
                .get()
                .addOnSuccessListener(datos -> {
                    String nombre = datos.getValue(String.class);
                    if (nombre != null && !nombre.trim().isEmpty()) {
                        ponerSaludo(nombre);
                    }
                });
    }


    private void ponerSaludo(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            tvSaludo.setText("Hola");
            return;
        }
        String primerNombre = nombre.trim().split(" ")[0];
        tvSaludo.setText("Hola, " + primerNombre);
    }


    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();
        SharedPreferences preferencias = getSharedPreferences("PillSensePrefs", Context.MODE_PRIVATE);
        preferencias.edit().putBoolean("mantener_sesion", false).apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}