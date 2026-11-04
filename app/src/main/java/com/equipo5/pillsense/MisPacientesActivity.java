package com.equipo5.pillsense;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

import java.util.HashMap;
import java.util.Map;


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


        // "Añadir paciente": pide el código del paciente
        findViewById(R.id.btnAnadirPaciente).setOnClickListener(v -> pedirCodigo());

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
        String uidActivo = PacienteActivo.hayElegido(this) ? PacienteActivo.uidDatos(this) : null;

        for (DataSnapshot paciente : datos.getChildren()) {
            String uid = paciente.getKey();
            String nombre = paciente.getValue(String.class);
            String nombreMostrado = (nombre != null && !nombre.isEmpty()) ? nombre : "Paciente";

            View fila = getLayoutInflater().inflate(R.layout.item_paciente_vinculado, listaPacientes, false);
            TextView tvNombre = fila.findViewById(R.id.tvNombrePaciente);


            if (uid != null && uid.equals(uidActivo)) {
                fila.setBackgroundResource(R.drawable.bg_cajon_ahora);
                tvNombre.setText(nombreMostrado + "\n✓ Viendo ahora");
            } else {
                tvNombre.setText(nombreMostrado);
            }

            // Al tocarlo pasa a ser el paciente activo y volvemos a la pantalla principal
            fila.setOnClickListener(v -> {
                PacienteActivo.elegir(this, uid, nombreMostrado);
                Toast.makeText(this, "Ahora ves a " + nombreMostrado, Toast.LENGTH_SHORT).show();
                finish();
            });

            listaPacientes.addView(fila);
        }
        tvSinPacientes.setVisibility(datos.hasChildren() ? View.GONE : View.VISIBLE);
    }
    /** Ventanita para escribir el código de 6 números del paciente. */
    private void pedirCodigo() {
        EditText etCodigo = new EditText(this);
        etCodigo.setInputType(InputType.TYPE_CLASS_NUMBER);
        etCodigo.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        etCodigo.setHint("123456");
        etCodigo.setTextSize(28);
        etCodigo.setGravity(Gravity.CENTER);

        // Un poco de margen a los lados para que no quede pegado al borde
        FrameLayout contenedor = new FrameLayout(this);
        int margen = Math.round(24 * getResources().getDisplayMetrics().density);
        contenedor.setPadding(margen, 0, margen, 0);
        contenedor.addView(etCodigo);

        new AlertDialog.Builder(this)
                .setTitle("Añadir paciente")
                .setMessage("Escribe el código de 6 números que sale en la app de tu paciente.")
                .setView(contenedor)
                .setPositiveButton("Añadir", (dialogo, cual) -> vincularConCodigo(etCodigo.getText().toString()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /** Busca de quién es el código y guarda el vínculo en el cuidador y en el paciente. */
    private void vincularConCodigo(String textoEscrito) {
        String codigo = textoEscrito.replace(" ", "").trim();
        if (codigo.length() != 6) {
            aviso("El código tiene que tener 6 números");
            return;
        }

        FirebaseUser yo = FirebaseAuth.getInstance().getCurrentUser();
        if (yo == null) return;
        String miUid = yo.getUid();
        DatabaseReference db = FirebaseDatabase.getInstance(DB_URL).getReference();

        // ¿De qué paciente es este código?
        db.child("codigos").child(codigo).get().addOnSuccessListener(datosCodigo -> {
            String uidPaciente = datosCodigo.getValue(String.class);
            if (uidPaciente == null) {
                aviso("Ese código no existe. Revisa los números.");
                return;
            }
            if (uidPaciente.equals(miUid)) {
                aviso("Ese es tu propio código");
                return;
            }

            // Nombre del paciente
            db.child("usuarios").child(uidPaciente).child("nombre").get().addOnSuccessListener(datosPaciente -> {
                String nombrePaciente = datosPaciente.getValue(String.class);

                // Mis datos (nombre, teléfono y si ya lo tengo en la lista)
                db.child("usuarios").child(miUid).get().addOnSuccessListener(misDatos -> {
                    if (misDatos.child("pacientes").hasChild(uidPaciente)) {
                        aviso("Este paciente ya está en tu lista");
                        return;
                    }

                    String miNombre = misDatos.child("nombre").getValue(String.class);
                    if (miNombre == null || miNombre.trim().isEmpty()) {
                        miNombre = (yo.getDisplayName() != null) ? yo.getDisplayName() : "Cuidador";
                    }
                    Object miTelefono = misDatos.child("telefono").getValue();

                    Map<String, Object> datosCuidador = new HashMap<>();
                    datosCuidador.put("nombre", miNombre);
                    datosCuidador.put("telefono", miTelefono != null ? String.valueOf(miTelefono) : "");

                    // Guardamos los dos lados a la vez
                    Map<String, Object> cambios = new HashMap<>();
                    cambios.put("usuarios/" + miUid + "/pacientes/" + uidPaciente,
                            nombrePaciente != null ? nombrePaciente : "Paciente");
                    cambios.put("usuarios/" + uidPaciente + "/cuidadores/" + miUid, datosCuidador);

                    db.updateChildren(cambios)
                            .addOnSuccessListener(x -> aviso("¡Paciente añadido!"))
                            .addOnFailureListener(e -> aviso("No se ha podido vincular. Inténtalo otra vez."));
                });
            });
        }).addOnFailureListener(e -> aviso("Sin conexión. Inténtalo otra vez."));
    }

    private void aviso(String texto) {
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show();
    }
}