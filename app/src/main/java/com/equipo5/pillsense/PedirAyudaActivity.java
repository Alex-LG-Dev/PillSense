package com.equipo5.pillsense;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

public class PedirAyudaActivity extends AppCompatActivity {

    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    private TextView tvTituloCuidadores;
    private LinearLayout listaCuidadores;
    private View btnVerMiCodigo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pedir_ayuda);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        // Flecha de volver y Cancelar: cierran esta pantalla
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
        findViewById(R.id.btnCancelar).setOnClickListener(v -> finish());

        // "Llamar al 112": solo visual, no hace nada

        tvTituloCuidadores = findViewById(R.id.tvTituloCuidadores);
        listaCuidadores = findViewById(R.id.listaCuidadores);
        btnVerMiCodigo = findViewById(R.id.btnVerMiCodigo);


        btnVerMiCodigo.setOnClickListener(v -> startActivity(new Intent(this, MiCodigoActivity.class)));
        findViewById(R.id.btnAnadirCuidador).setOnClickListener(v ->
                startActivity(new Intent(this, MiCodigoActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();

        cargarCuidadores();
    }


    private void cargarCuidadores() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) return;

        FirebaseDatabase.getInstance(DB_URL)
                .getReference("usuarios").child(usuario.getUid()).child("cuidadores")
                .get()
                .addOnSuccessListener(datos -> {
                    listaCuidadores.removeAllViews();

                    // Sin cuidadores: mensaje y botón "Ver mi código"
                    if (!datos.hasChildren()) {
                        tvTituloCuidadores.setText("Aún no tienes cuidador");
                        btnVerMiCodigo.setVisibility(View.VISIBLE);
                        return;
                    }

                    tvTituloCuidadores.setText("¿A quién quieres llamar?");
                    btnVerMiCodigo.setVisibility(View.GONE);

                    for (DataSnapshot cuidador : datos.getChildren()) {
                        String nombre = cuidador.child("nombre").getValue(String.class);

                        Object tel = cuidador.child("telefono").getValue();
                        String telefono = (tel != null) ? String.valueOf(tel).trim() : "";
                        anadirBotonCuidador(nombre, telefono);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "No se han podido cargar tus cuidadores", Toast.LENGTH_LONG).show());
    }


    private void anadirBotonCuidador(String nombre, String telefono) {
        View fila = getLayoutInflater().inflate(R.layout.item_boton_cuidador, listaCuidadores, false);
        Button btnLlamar = fila.findViewById(R.id.btnLlamar);

        String primerNombre = (nombre != null && !nombre.trim().isEmpty())
                ? nombre.trim().split(" ")[0]
                : "tu cuidador";
        btnLlamar.setText("Llamar a " + primerNombre);

        if (telefono.isEmpty()) {
            btnLlamar.setEnabled(false);
            fila.findViewById(R.id.tvSinTelefono).setVisibility(View.VISIBLE);
        } else {
            btnLlamar.setOnClickListener(v -> abrirMarcador(telefono));
        }
        listaCuidadores.addView(fila);
    }

    /** Abre la app de Teléfono con el número ya escrito */
    private void abrirMarcador(String telefono) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + telefono));
        startActivity(intent);
    }
}