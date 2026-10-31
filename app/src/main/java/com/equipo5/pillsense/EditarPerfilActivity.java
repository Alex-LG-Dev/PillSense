package com.equipo5.pillsense;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
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
 * Pantalla "Mi perfil" (T036): editar los datos del perfil y cambiar la contraseña.
 *
 * - Nombre y rol: se guardan en Firebase Auth (nombre) y en la base de datos en
 *   usuarios/{uid} (nombre, rol y correo). Al volver a abrir la pantalla se cargan de ahí.
 * - Contraseña: solo para cuentas de correo y contraseña. Pide la contraseña actual
 *   (Firebase exige volver a autenticar) y después pone la nueva.
 *   Las cuentas de Google o Facebook ven un aviso en lugar del formulario.
 */
public class EditarPerfilActivity extends AppCompatActivity {

    private static final String TAG = "Perfil";
    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";
    private static final String NODO_USUARIOS = "usuarios";
    private static final String ROL_PACIENTE = "paciente";
    private static final String ROL_CUIDADOR = "cuidador";
    private static final int MINIMO_CARACTERES = 8;
    // Si la base de datos no responde en este tiempo (sin internet), se avisa al usuario
    private static final long ESPERA_BD_MS = 8000;

    private FirebaseUser usuario;
    private DatabaseReference refUsuario;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvInicial;
    private TextView tvNombreCabecera;
    private TextView tvCorreoCabecera;
    private EditText etNombre;
    private EditText etCorreo;
    private EditText etTelefono;
    private RadioGroup rgRol;
    private Button btnGuardar;

    private LinearLayout seccionContrasena;
    private TextView tvContrasenaOtraCuenta;
    private EditText etActual;
    private EditText etNueva;
    private EditText etRepetir;
    private Button btnCambiarContrasena;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BarraAcciones.prepararVentana(this);
        setContentView(R.layout.activity_editar_perfil);
        BarraAcciones.configurar(this, R.string.perfil_titulo, true, 0);

        // Solo se puede ver el perfil con la sesión iniciada
        usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Toast.makeText(this, R.string.perfil_error_sesion, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        refUsuario = FirebaseDatabase.getInstance(DB_URL)
                .getReference(NODO_USUARIOS).child(usuario.getUid());

        tvInicial = findViewById(R.id.tvInicialPerfil);
        tvNombreCabecera = findViewById(R.id.tvNombreCabecera);
        tvCorreoCabecera = findViewById(R.id.tvCorreoCabecera);
        etNombre = findViewById(R.id.etNombrePerfil);
        etCorreo = findViewById(R.id.etCorreoPerfil);
        etTelefono = findViewById(R.id.etTelefonoPerfil);
        rgRol = findViewById(R.id.rgRolPerfil);
        btnGuardar = findViewById(R.id.btnGuardarPerfil);
        seccionContrasena = findViewById(R.id.seccionContrasena);
        tvContrasenaOtraCuenta = findViewById(R.id.tvContrasenaOtraCuenta);
        etActual = findViewById(R.id.etContrasenaActual);
        etNueva = findViewById(R.id.etContrasenaNueva);
        etRepetir = findViewById(R.id.etContrasenaRepetir);
        btnCambiarContrasena = findViewById(R.id.btnCambiarContrasena);

        mostrarDatosDeLaCuenta();
        cargarDatosGuardados();
        prepararSeccionContrasena();

        btnGuardar.setOnClickListener(v -> guardarPerfil());
        btnCambiarContrasena.setOnClickListener(v -> cambiarContrasena());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    // ---------------------------------------------------------------- datos del perfil

    /** Rellena la pantalla con lo que ya sabe Firebase Auth (nombre y correo). */
    private void mostrarDatosDeLaCuenta() {
        String nombre = usuario.getDisplayName() != null ? usuario.getDisplayName() : "";
        String correo = usuario.getEmail() != null ? usuario.getEmail() : "";
        etNombre.setText(nombre);
        etCorreo.setText(correo);
        actualizarCabecera(nombre, correo);
    }

    /** Lee de la base de datos el nombre, el rol y el teléfono guardados (si los hay). */
    private void cargarDatosGuardados() {
        refUsuario.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;
                String nombre = snapshot.child("nombre").getValue(String.class);
                String rol = snapshot.child("rol").getValue(String.class);
                if (nombre != null && !nombre.trim().isEmpty()) {
                    etNombre.setText(nombre);
                    actualizarCabecera(nombre, etCorreo.getText().toString());
                }
                String telefono = snapshot.child("telefono").getValue(String.class);
                if (telefono != null) {
                    etTelefono.setText(telefono);
                }
                if (ROL_CUIDADOR.equals(rol)) {
                    rgRol.check(R.id.rbCuidadorPerfil);
                } else if (ROL_PACIENTE.equals(rol)) {
                    rgRol.check(R.id.rbPacientePerfil);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "No se pudo leer el perfil", error.toException());
            }
        });
    }

    private void actualizarCabecera(String nombre, String correo) {
        String limpio = nombre == null ? "" : nombre.trim();
        tvNombreCabecera.setText(limpio);
        tvCorreoCabecera.setText(correo);
        tvInicial.setText(limpio.isEmpty() ? "" : limpio.substring(0, 1).toUpperCase());
    }

    private void guardarPerfil() {
        String nombre = etNombre.getText().toString().trim();
        if (nombre.isEmpty()) {
            etNombre.setError(getString(R.string.perfil_error_nombre));
            return;
        }
        String rol = rgRol.getCheckedRadioButtonId() == R.id.rbCuidadorPerfil
                ? ROL_CUIDADOR : ROL_PACIENTE;

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
            actualizarCabecera(nombre, etCorreo.getText().toString());
            guardarEnBaseDeDatos(nombre, rol);
        });
    }

    /** 2) Nombre, rol, teléfono y correo se guardan también en la base de datos (usuarios/{uid}). */
    private void guardarEnBaseDeDatos(String nombre, String rol) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", nombre);
        datos.put("rol", rol);
        datos.put("telefono", etTelefono.getText().toString().trim());
        datos.put("correo", usuario.getEmail() != null ? usuario.getEmail() : "");

        final boolean[] terminado = {false};
        // Sin internet, Firebase espera y guarda al volver la conexión: se avisa pasado un rato
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
            } else {
                Log.e(TAG, "No se pudo guardar el perfil en la base de datos", tarea.getException());
                Toast.makeText(this, R.string.perfil_guardado_sin_bd, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---------------------------------------------------------------- contraseña

    /** Solo las cuentas de correo y contraseña pueden cambiarla desde la app. */
    private void prepararSeccionContrasena() {
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
            seccionContrasena.setVisibility(LinearLayout.VISIBLE);
            tvContrasenaOtraCuenta.setVisibility(TextView.GONE);
        } else {
            seccionContrasena.setVisibility(LinearLayout.GONE);
            tvContrasenaOtraCuenta.setText(otroProveedor != null
                    ? getString(R.string.contrasena_otra_cuenta, otroProveedor)
                    : getString(R.string.contrasena_otra_cuenta_generica));
            tvContrasenaOtraCuenta.setVisibility(TextView.VISIBLE);
        }
    }

    private void cambiarContrasena() {
        String actual = etActual.getText().toString();
        String nueva = etNueva.getText().toString();
        String repetida = etRepetir.getText().toString();

        if (actual.isEmpty()) {
            etActual.setError(getString(R.string.contrasena_error_actual_vacia));
            return;
        }
        if (nueva.length() < MINIMO_CARACTERES) {
            etNueva.setError(getString(R.string.minimo_caracteres));
            return;
        }
        if (nueva.equals(actual)) {
            etNueva.setError(getString(R.string.contrasena_error_igual));
            return;
        }
        if (!nueva.equals(repetida)) {
            etRepetir.setError(getString(R.string.contrasena_error_distintas));
            return;
        }
        String correo = usuario.getEmail();
        if (correo == null) {
            Toast.makeText(this, R.string.contrasena_error_generico, Toast.LENGTH_LONG).show();
            return;
        }

        btnCambiarContrasena.setEnabled(false);

        // Firebase pide volver a autenticar con la contraseña actual antes de cambiarla
        AuthCredential credencial = EmailAuthProvider.getCredential(correo, actual);
        usuario.reauthenticate(credencial).addOnCompleteListener(this, tareaAuth -> {
            if (!tareaAuth.isSuccessful()) {
                btnCambiarContrasena.setEnabled(true);
                Exception e = tareaAuth.getException();
                if (e instanceof FirebaseAuthInvalidCredentialsException
                        || e instanceof FirebaseAuthInvalidUserException) {
                    etActual.setError(getString(R.string.contrasena_error_actual_incorrecta));
                } else {
                    Log.e(TAG, "No se pudo volver a autenticar", e);
                    avisarErrorContrasena(e);
                }
                return;
            }
            usuario.updatePassword(nueva).addOnCompleteListener(this, tareaCambio -> {
                btnCambiarContrasena.setEnabled(true);
                if (tareaCambio.isSuccessful()) {
                    etActual.setText("");
                    etNueva.setText("");
                    etRepetir.setText("");
                    Toast.makeText(this, R.string.contrasena_cambiada, Toast.LENGTH_SHORT).show();
                } else {
                    Exception e = tareaCambio.getException();
                    Log.e(TAG, "No se pudo cambiar la contraseña", e);
                    if (e instanceof FirebaseAuthWeakPasswordException) {
                        etNueva.setError(getString(R.string.contrasena_error_debil));
                    } else {
                        avisarErrorContrasena(e);
                    }
                }
            });
        });
    }

    private void avisarErrorContrasena(Exception e) {
        int mensaje = (e instanceof FirebaseNetworkException)
                ? R.string.contrasena_error_red : R.string.contrasena_error_generico;
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }
}
