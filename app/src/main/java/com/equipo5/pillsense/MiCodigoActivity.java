package com.equipo5.pillsense;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MiCodigoActivity extends AppCompatActivity {

    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    private TextView tvCodigo;

    private TextView tvListaCuidadores;
    private DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mi_codigo);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
        findViewById(R.id.btnListo).setOnClickListener(v -> finish());

        tvCodigo = findViewById(R.id.tvCodigo);
        tvCodigo.setText("…");   // mientras carga
        db = FirebaseDatabase.getInstance(DB_URL).getReference();

        cargarOCrearCodigo();
        tvListaCuidadores = findViewById(R.id.tvListaCuidadores);
        cargarCuidadores();
    }

    /** Si el paciente ya tiene código, lo enseña. Si no, crea uno nuevo. */
    private void cargarOCrearCodigo() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) return;
        String uid = usuario.getUid();

        db.child("usuarios").child(uid).child("codigoVinculacion").get()
                .addOnSuccessListener(datos -> {
                    String codigo = datos.getValue(String.class);
                    if (codigo != null) {
                        mostrarCodigo(codigo);
                    } else {
                        crearCodigoNuevo(uid, 0);
                    }
                })
                .addOnFailureListener(e -> tvCodigo.setText("Sin conexión"));
    }

    /** Inventa un código de 6 números que no tenga otro paciente y lo guarda en Firebase. */
    private void crearCodigoNuevo(String uid, int intentos) {
        if (intentos >= 5) {   // muy raro: 5 códigos seguidos ya usados
            tvCodigo.setText("Inténtalo más tarde");
            return;
        }

        // Número al azar del 000000 al 999999
        String codigo = String.format(Locale.ROOT, "%06d", new SecureRandom().nextInt(1_000_000));

        db.child("codigos").child(codigo).get().addOnSuccessListener(datos -> {
            if (datos.exists()) {   // ya lo tiene otro paciente: probamos con otro
                crearCodigoNuevo(uid, intentos + 1);
                return;
            }

            Map<String, Object> cambios = new HashMap<>();
            cambios.put("codigos/" + codigo, uid);
            cambios.put("usuarios/" + uid + "/codigoVinculacion", codigo);
            db.updateChildren(cambios)
                    .addOnSuccessListener(x -> mostrarCodigo(codigo))
                    .addOnFailureListener(e -> tvCodigo.setText("No se ha podido crear"));
        }).addOnFailureListener(e -> tvCodigo.setText("Sin conexión"));
    }
    /** Pone los nombres de sus cuidadores */
    private void cargarCuidadores() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) return;

        db.child("usuarios").child(usuario.getUid()).child("cuidadores").get()
                .addOnSuccessListener(datos -> {
                    List<String> nombres = new ArrayList<>();
                    for (DataSnapshot cuidador : datos.getChildren()) {
                        String nombre = cuidador.child("nombre").getValue(String.class);
                        if (nombre != null && !nombre.trim().isEmpty()) nombres.add(nombre.trim());
                    }
                    tvListaCuidadores.setText(nombres.isEmpty()
                            ? "Todavía nadie"
                            : TextUtils.join("\n", nombres));
                });
    }
    /** "482913" → "482 913", más fácil de leer y de dictar. */
    private void mostrarCodigo(String codigo) {
        tvCodigo.setText(codigo.substring(0, 3) + " " + codigo.substring(3));
    }
}