package com.equipo5.pillsense;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;


public class PedirAyudaActivity extends AppCompatActivity {

    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    private TextView tvNombreCuidador;
    private Button btnLlamarCuidador;
    private String telefonoCuidador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pedir_ayuda);

        // Deja hueco arriba (hora, batería) y abajo (botones del móvil)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        // Flecha de volver y Cancelar: cierran esta pantalla
        ImageButton btnVolver = findViewById(R.id.btnVolver);
        btnVolver.setOnClickListener(v -> finish());

        Button btnCancelar = findViewById(R.id.btnCancelar);
        btnCancelar.setOnClickListener(v -> finish());

        // "Llamar al 112": solo visual, de momento no hace nada

        // Cuidadora: nombre y botón de llamar
        tvNombreCuidador = findViewById(R.id.tvNombreCuidador);
        btnLlamarCuidador = findViewById(R.id.btnLlamarCuidador);


        tvNombreCuidador.setText("");
        btnLlamarCuidador.setEnabled(false);

        btnLlamarCuidador.setOnClickListener(v -> abrirMarcador());
        cargarCuidador();
    }


    private void cargarCuidador() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) return;

        FirebaseDatabase.getInstance(DB_URL)
                .getReference("usuarios").child(usuario.getUid()).child("cuidador")
                .get()
                .addOnSuccessListener(datos -> {
                    String nombre = datos.child("nombre").getValue(String.class);
                    Object telefono = datos.child("telefono").getValue();
                    telefonoCuidador = (telefono != null) ? String.valueOf(telefono).trim() : null;

                    // Sin teléfono: no se puede llamar
                    if (telefonoCuidador == null || telefonoCuidador.isEmpty()) {
                        tvNombreCuidador.setText("Sin cuidadora");
                        btnLlamarCuidador.setVisibility(View.GONE);
                        Toast.makeText(this, "Todavía no tienes una cuidadora guardada",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Con teléfono: ponemos su nombre y "Llamar a María"
                    boolean tieneNombre = nombre != null && !nombre.trim().isEmpty();
                    tvNombreCuidador.setText(tieneNombre ? nombre.trim() : "");
                    btnLlamarCuidador.setText(tieneNombre
                            ? "Llamar a " + nombre.trim().split(" ")[0]
                            : "Llamar");
                    btnLlamarCuidador.setEnabled(true);
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "No se han podido cargar los datos de tu cuidadora", Toast.LENGTH_LONG).show());
    }

    /** Abre la app de Teléfono con el número de la cuidadora ya escrito (no llama sola). */
    private void abrirMarcador() {
        if (telefonoCuidador == null) return;
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + telefonoCuidador));
        startActivity(intent);
    }
}