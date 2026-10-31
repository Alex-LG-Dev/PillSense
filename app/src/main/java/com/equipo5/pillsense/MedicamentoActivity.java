package com.equipo5.pillsense;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/** Pantalla para añadir un medicamento. Se guarda en medicamentos/{uid}/{id} de Realtime Database. */
public class MedicamentoActivity extends AppCompatActivity {

    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    // Medicamentos del usuario que ha iniciado sesión
    private DatabaseReference refMedicamentos;

    private EditText etNombre;
    private EditText etDosis;
    private RadioGroup rgCajon;
    private Button btnGuardar;

    // La foto en Base64 (la rellenaremos en el paso 4)
    private String imagenBase64 = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BarraAcciones.prepararVentana(this);
        setContentView(R.layout.activity_medicamento);
        BarraAcciones.configurar(this, R.string.med_titulo_nuevo, true, 0);

        // Sin sesión no sabemos de quién es el medicamento
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Toast.makeText(this, R.string.med_sin_sesion, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        refMedicamentos = FirebaseDatabase.getInstance(DB_URL)
                .getReference("medicamentos")
                .child(usuario.getUid());

        etNombre = findViewById(R.id.etNombreMed);
        etDosis = findViewById(R.id.etDosisMed);
        rgCajon = findViewById(R.id.rgCajon);
        btnGuardar = findViewById(R.id.btnGuardarMed);

        btnGuardar.setOnClickListener(v -> guardar());
    }

    private void guardar() {
        String nombre = etNombre.getText().toString().trim();
        String dosis = etDosis.getText().toString().trim();

        // Comprobamos que no falte nada
        if (nombre.isEmpty()) {
            etNombre.setError(getString(R.string.med_error_nombre));
            etNombre.requestFocus();
            return;
        }
        if (dosis.isEmpty()) {
            etDosis.setError(getString(R.string.med_error_dosis));
            etDosis.requestFocus();
            return;
        }

        // push() crea un id único para el medicamento nuevo
        String id = refMedicamentos.push().getKey();
        Medicamento med = new Medicamento(id, nombre, dosis, cajonElegido(), imagenBase64);

        btnGuardar.setEnabled(false); // evita guardarlo dos veces si se pulsa rápido
        refMedicamentos.child(id).setValue(med)
                .addOnSuccessListener(r -> {
                    Toast.makeText(this, R.string.med_guardado, Toast.LENGTH_SHORT).show();
                    finish(); // volvemos a la lista
                })
                .addOnFailureListener(e -> {
                    btnGuardar.setEnabled(true);
                    Toast.makeText(this, getString(R.string.med_error_guardar, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }

    /** Devuelve el número de cajón marcado (1 a 4). */
    private int cajonElegido() {
        int marcado = rgCajon.getCheckedRadioButtonId();
        if (marcado == R.id.rbCajon2) return 2;
        if (marcado == R.id.rbCajon3) return 3;
        if (marcado == R.id.rbCajon4) return 4;
        return 1;
    }
}