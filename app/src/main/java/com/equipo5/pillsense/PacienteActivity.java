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


public class PacienteActivity extends AppCompatActivity {

    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    private TextView tvSaludo;

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