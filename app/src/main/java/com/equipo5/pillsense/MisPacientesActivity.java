package com.equipo5.pillsense;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class MisPacientesActivity extends AppCompatActivity {

    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    private LinearLayout listaPacientes;
    private TextView tvSinPacientes;


    private DatabaseReference refPacientes;
    private ValueEventListener escuchador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mis_pacientes);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());

        listaPacientes = findViewById(R.id.listaPacientes);
        tvSinPacientes = findViewById(R.id.tvSinPacientes);


        findViewById(R.id.btnAnadirPaciente).setOnClickListener(v ->
                Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show());

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) return;

        refPacientes = FirebaseDatabase.getInstance(DB_URL)
                .getReference("usuarios").child(usuario.getUid()).child("pacientes");

        // Se ejecuta al abrir y cada vez que se añade o quita un paciente
        escuchador = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot datos) {
                mostrarPacientes(datos);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MisPacientesActivity.this,
                        "No se han podido cargar tus pacientes", Toast.LENGTH_LONG).show();
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (refPacientes != null) refPacientes.addValueEventListener(escuchador);
    }

    @Override
    protected void onStop() {
        if (refPacientes != null) refPacientes.removeEventListener(escuchador);
        super.onStop();
    }


    private void mostrarPacientes(DataSnapshot datos) {
        listaPacientes.removeAllViews();
        for (DataSnapshot paciente : datos.getChildren()) {
            String nombre = paciente.getValue(String.class);
            View fila = getLayoutInflater().inflate(R.layout.item_paciente_vinculado, listaPacientes, false);
            ((TextView) fila.findViewById(R.id.tvNombrePaciente))
                    .setText(nombre != null && !nombre.isEmpty() ? nombre : "Paciente");
            listaPacientes.addView(fila);
        }
        tvSinPacientes.setVisibility(datos.hasChildren() ? View.GONE : View.VISIBLE);
    }
}