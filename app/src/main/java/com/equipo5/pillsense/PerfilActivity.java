package com.equipo5.pillsense;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.bumptech.glide.Glide;

/**
 * Pantalla "Perfil" (T035): muestra la foto y los datos del usuario.
 * Los datos salen de Firebase Auth (nombre, correo, foto) y de usuarios/{uid}
 * en la base de datos (nombre, teléfono y rol). Para cambiarlos: "Editar mis datos".
 */
public class PerfilActivity extends AppCompatActivity {

    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    private FirebaseUser usuario;
    private ImageView ivFoto;
    private TextView tvNombre, tvCorreo, tvTelefono, tvRol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {          // sin sesión no hay perfil
            finish();
            return;
        }

        ivFoto = findViewById(R.id.ivFotoPerfil);
        tvNombre = findViewById(R.id.tvNombrePerfil);
        tvCorreo = findViewById(R.id.tvCorreoPerfil);
        tvTelefono = findViewById(R.id.tvTelefonoPerfil);
        tvRol = findViewById(R.id.tvRolPerfil);

        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
        findViewById(R.id.filaEditar).setOnClickListener(v ->
                startActivity(new Intent(this, EditarPerfilActivity.class)));
        findViewById(R.id.filaAvisos).setOnClickListener(v ->
                Toast.makeText(this, "Avisos: próximamente", Toast.LENGTH_SHORT).show());
    }

    /** En onResume para que, al volver de "Editar mis datos", se vean los cambios. */
    @Override
    protected void onResume() {
        super.onResume();
        if (usuario == null) return;
        mostrarDatosDeLaCuenta();
        cargarDatosDeLaBaseDeDatos();
    }

    /** Lo que ya sabe Firebase Auth: nombre y correo. */
    private void mostrarDatosDeLaCuenta() {
        String nombre = usuario.getDisplayName();
        tvNombre.setText(nombre != null && !nombre.isEmpty() ? nombre : "Sin nombre");
        tvCorreo.setText(usuario.getEmail());
        tvRol.setText("Cuidador");      // valor por defecto hasta leer la base de datos
        // Foto de Google o Facebook; si no hay, se queda el icono de persona
        Uri foto = usuario.getPhotoUrl();
        if (foto != null) {
            ivFoto.setPadding(0, 0, 0, 0);
            Glide.with(this).load(foto).circleCrop()
                    .error(R.drawable.ic_persona)
                    .into(ivFoto);
        }
    }

    /** Lo guardado en usuarios/{uid}: nombre, teléfono y rol. */
    private void cargarDatosDeLaBaseDeDatos() {
        FirebaseDatabase.getInstance(DB_URL)
                .getReference("usuarios").child(usuario.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot datos) {
                        if (isFinishing() || isDestroyed()) return;

                        String nombre = datos.child("nombre").getValue(String.class);
                        if (nombre != null && !nombre.trim().isEmpty()) {
                            tvNombre.setText(nombre);
                        }

                        String telefono = datos.child("telefono").getValue(String.class);
                        if (telefono != null && !telefono.trim().isEmpty()) {
                            tvTelefono.setText(telefono);
                            tvTelefono.setVisibility(View.VISIBLE);
                        } else {
                            tvTelefono.setVisibility(View.GONE);
                        }

                        String rol = datos.child("rol").getValue(String.class);
                        tvRol.setText("paciente".equals(rol) ? "Paciente" : "Cuidador");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Sin conexión: se quedan los datos de Firebase Auth
                    }
                });
    }
}
