package com.equipo5.pillsense;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Pantalla "Editar perfil" (T036): cambiar el nombre y el teléfono.
 *
 * - El nombre se guarda en la cuenta de Firebase Auth y, con el correo y el teléfono,
 *   en la base de datos en usuarios/{uid}. Al volver a Perfil se ven los cambios.
 * - El correo se muestra pero no se puede cambiar.
 * - La fila "Cambiar contraseña" abre su propia pantalla. Solo aparece en las cuentas
 *   de correo y contraseña: las de Google o Facebook ven un aviso.
 */
public class EditarPerfilActivity extends AppCompatActivity {

    private static final String TAG = "EditarPerfil";
    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";
    // Si la base de datos no responde en este tiempo (sin internet), se avisa al usuario
    private static final long ESPERA_BD_MS = 8000;

    private FirebaseUser usuario;
    private DatabaseReference refUsuario;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private EditText etNombre;
    private EditText etCorreo;
    private EditText etTelefono;
    private MaterialButton btnGuardar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {          // sin sesión no hay perfil que editar
            Toast.makeText(this, R.string.perfil_error_sesion, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        refUsuario = FirebaseDatabase.getInstance(DB_URL)
                .getReference("usuarios").child(usuario.getUid());

        etNombre = findViewById(R.id.etNombrePerfil);
        etCorreo = findViewById(R.id.etCorreoPerfil);
        etTelefono = findViewById(R.id.etTelefonoPerfil);
        btnGuardar = findViewById(R.id.btnGuardarPerfil);

        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
        findViewById(R.id.btnCambiarFoto).setOnClickListener(v ->
                Toast.makeText(this, "Cambiar foto: próximamente", Toast.LENGTH_SHORT).show());
        btnGuardar.setOnClickListener(v -> guardarPerfil());

        mostrarDatosDeLaCuenta();
        cargarDatosGuardados();
        prepararFilaContrasena();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    /** Lo que ya sabe Firebase Auth: nombre, correo y foto. */
    private void mostrarDatosDeLaCuenta() {
        String nombre = usuario.getDisplayName() != null ? usuario.getDisplayName() : "";
        etNombre.setText(nombre);
        etCorreo.setText(usuario.getEmail() != null ? usuario.getEmail() : "");

        // Foto de Google o Facebook; si no hay, se queda el icono de persona
        Uri foto = usuario.getPhotoUrl();
        if (foto != null) {
            ImageView ivFoto = findViewById(R.id.ivFotoEditar);
            ivFoto.setPadding(0, 0, 0, 0);
            Glide.with(this).load(foto).circleCrop()
                    .error(R.drawable.ic_persona)
                    .into(ivFoto);
        }
    }

    /** Lo guardado en usuarios/{uid}: nombre y teléfono. */
    private void cargarDatosGuardados() {
        refUsuario.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot datos) {
                if (isFinishing() || isDestroyed()) return;

                String nombre = datos.child("nombre").getValue(String.class);
                String deLaCuenta = usuario.getDisplayName() != null ? usuario.getDisplayName() : "";
                // No se pisa lo que el usuario ya haya empezado a escribir
                if (nombre != null && !nombre.trim().isEmpty()
                        && etNombre.getText().toString().equals(deLaCuenta)) {
                    etNombre.setText(nombre);
                }

                String telefono = datos.child("telefono").getValue(String.class);
                if (telefono != null && etTelefono.getText().toString().isEmpty()) {
                    etTelefono.setText(telefono);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "No se pudo leer el perfil", error.toException());
            }
        });
    }

    /** La contraseña solo se puede cambiar en las cuentas de correo y contraseña. */
    private void prepararFilaContrasena() {
        View fila = findViewById(R.id.filaCambiarContrasena);
        TextView aviso = findViewById(R.id.tvContrasenaOtraCuenta);

        boolean conContrasena = false;
        String otroProveedor = null;
        for (UserInfo info : usuario.getProviderData()) {
            switch (info.getProviderId()) {
                case EmailAuthProvider.PROVIDER_ID:
                    conContrasena = true;
                    break;
                case "google.com":
                    otroProveedor = getString(R.string.google);
                    break;
                case "facebook.com":
                    otroProveedor = getString(R.string.facebook);
                    break;
                default:
                    break;
            }
        }

        if (conContrasena) {
            fila.setVisibility(View.VISIBLE);
            aviso.setVisibility(View.GONE);
            fila.setOnClickListener(v ->
                    startActivity(new Intent(this, CambiarContrasenaActivity.class)));
        } else {
            fila.setVisibility(View.GONE);
            aviso.setText(otroProveedor != null
                    ? getString(R.string.contrasena_otra_cuenta, otroProveedor)
                    : getString(R.string.contrasena_otra_cuenta_generica));
            aviso.setVisibility(View.VISIBLE);
        }
    }

    // ---------------------------------------------------------------- guardar

    private void guardarPerfil() {
        String nombre = etNombre.getText().toString().trim();
        if (nombre.isEmpty()) {
            etNombre.setError(getString(R.string.perfil_error_nombre));
            return;
        }
        // El teléfono es opcional; si se escribe, se quitan espacios y guiones
        String telefono = etTelefono.getText().toString().replaceAll("[\\s-]", "");
        if (!telefono.isEmpty() && !telefono.matches("\\+?[0-9]{6,15}")) {
            etTelefono.setError(getString(R.string.perfil_error_telefono));
            return;
        }

        btnGuardar.setEnabled(false);

        // 1) El nombre se guarda en la cuenta de Firebase Auth
        UserProfileChangeRequest cambios = new UserProfileChangeRequest.Builder()
                .setDisplayName(nombre)
                .build();
        usuario.updateProfile(cambios).addOnCompleteListener(this, tarea -> {
            if (!tarea.isSuccessful()) {
                Log.e(TAG, "No se pudo actualizar el nombre en Firebase Auth", tarea.getException());
                btnGuardar.setEnabled(true);
                Toast.makeText(this, R.string.perfil_error_guardar, Toast.LENGTH_LONG).show();
                return;
            }
            guardarEnBaseDeDatos(nombre, telefono);
        });
    }

    /** 2) Nombre, correo y teléfono se guardan también en usuarios/{uid}. El rol no se toca. */
    private void guardarEnBaseDeDatos(String nombre, String telefono) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", nombre);
        datos.put("correo", usuario.getEmail() != null ? usuario.getEmail() : "");
        datos.put("telefono", telefono);

        final boolean[] terminado = {false};
        // Sin internet, Firebase guarda al volver la conexión: se avisa pasado un rato
        Runnable sinRespuesta = () -> {
            if (terminado[0]) return;
            terminado[0] = true;
            btnGuardar.setEnabled(true);
            Toast.makeText(this, R.string.perfil_guardado_sin_conexion, Toast.LENGTH_LONG).show();
        };
        handler.postDelayed(sinRespuesta, ESPERA_BD_MS);

        refUsuario.updateChildren(datos).addOnCompleteListener(tarea -> {
            if (terminado[0]) return;
            terminado[0] = true;
            handler.removeCallbacks(sinRespuesta);
            btnGuardar.setEnabled(true);
            if (tarea.isSuccessful()) {
                Toast.makeText(this, R.string.perfil_guardado, Toast.LENGTH_SHORT).show();
                finish();   // vuelve a Perfil, que se actualiza solo
            } else {
                Log.e(TAG, "No se pudo guardar el perfil en la base de datos", tarea.getException());
                Toast.makeText(this, R.string.perfil_guardado_sin_bd, Toast.LENGTH_LONG).show();
            }
        });
    }
}
