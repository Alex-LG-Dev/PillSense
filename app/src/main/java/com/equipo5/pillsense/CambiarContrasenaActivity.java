package com.equipo5.pillsense;

import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

/**
 * Pantalla "Cambiar contraseña" (T036). Solo para las cuentas de correo y contraseña.
 *
 * Firebase obliga a volver a autenticar antes de cambiarla: se pide la contraseña actual,
 * se comprueba y después se pone la nueva (mínimo 8 caracteres). Con "He olvidado mi
 * contraseña" se envía el correo de restablecimiento.
 */
public class CambiarContrasenaActivity extends AppCompatActivity {

    private static final String TAG = "CambiarContrasena";
    private static final int MINIMO_CARACTERES = 8;

    private FirebaseUser usuario;

    private EditText etActual;
    private EditText etNueva;
    private EditText etRepetir;
    private MaterialButton btnGuardar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cambiar_contrasena);

        usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null || !tieneContrasena(usuario)) {
            // Sin sesión, o cuenta de Google o Facebook: aquí no hay contraseña que cambiar
            Toast.makeText(this, R.string.contrasena_otra_cuenta_generica, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        etActual = findViewById(R.id.etContrasenaActual);
        etNueva = findViewById(R.id.etContrasenaNueva);
        etRepetir = findViewById(R.id.etContrasenaRepetir);
        btnGuardar = findViewById(R.id.btnCambiarContrasena);
        CheckBox cbMostrar = findViewById(R.id.cbMostrarContrasenas);

        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
        cbMostrar.setOnCheckedChangeListener((boton, marcado) -> {
            mostrarUOcultar(etActual, marcado);
            mostrarUOcultar(etNueva, marcado);
            mostrarUOcultar(etRepetir, marcado);
        });
        findViewById(R.id.tvContrasenaOlvidada).setOnClickListener(v -> enviarCorreoDeRestablecer());
        btnGuardar.setOnClickListener(v -> cambiarContrasena());
    }

    private static boolean tieneContrasena(FirebaseUser u) {
        for (UserInfo info : u.getProviderData()) {
            if (EmailAuthProvider.PROVIDER_ID.equals(info.getProviderId())) return true;
        }
        return false;
    }

    /** Enseña u oculta lo escrito, dejando el cursor donde estaba. */
    private void mostrarUOcultar(EditText campo, boolean mostrar) {
        int cursor = campo.getSelectionEnd();
        TransformationMethod metodo = mostrar
                ? HideReturnsTransformationMethod.getInstance()
                : PasswordTransformationMethod.getInstance();
        campo.setTransformationMethod(metodo);
        if (cursor >= 0) campo.setSelection(cursor);
    }

    private void enviarCorreoDeRestablecer() {
        String correo = usuario.getEmail();
        if (correo == null) {
            Toast.makeText(this, R.string.contrasena_reset_error, Toast.LENGTH_LONG).show();
            return;
        }
        FirebaseAuth.getInstance().sendPasswordResetEmail(correo)
                .addOnCompleteListener(this, tarea -> {
                    if (tarea.isSuccessful()) {
                        Toast.makeText(this, R.string.contrasena_reset_enviado, Toast.LENGTH_LONG).show();
                    } else {
                        Log.e(TAG, "No se pudo enviar el correo de restablecimiento", tarea.getException());
                        avisarError(tarea.getException(), R.string.contrasena_reset_error);
                    }
                });
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

        btnGuardar.setEnabled(false);

        // Firebase pide volver a autenticar con la contraseña actual antes de cambiarla
        AuthCredential credencial = EmailAuthProvider.getCredential(correo, actual);
        usuario.reauthenticate(credencial).addOnCompleteListener(this, tareaAuth -> {
            if (!tareaAuth.isSuccessful()) {
                btnGuardar.setEnabled(true);
                Exception e = tareaAuth.getException();
                if (e instanceof FirebaseAuthInvalidCredentialsException
                        || e instanceof FirebaseAuthInvalidUserException) {
                    etActual.setError(getString(R.string.contrasena_error_actual_incorrecta));
                } else {
                    Log.e(TAG, "No se pudo volver a autenticar", e);
                    avisarError(e, R.string.contrasena_error_generico);
                }
                return;
            }
            usuario.updatePassword(nueva).addOnCompleteListener(this, tareaCambio -> {
                btnGuardar.setEnabled(true);
                if (tareaCambio.isSuccessful()) {
                    Toast.makeText(this, R.string.contrasena_cambiada, Toast.LENGTH_SHORT).show();
                    finish();   // vuelve a Editar perfil
                } else {
                    Exception e = tareaCambio.getException();
                    Log.e(TAG, "No se pudo cambiar la contraseña", e);
                    if (e instanceof FirebaseAuthWeakPasswordException) {
                        etNueva.setError(getString(R.string.contrasena_error_debil));
                    } else {
                        avisarError(e, R.string.contrasena_error_generico);
                    }
                }
            });
        });
    }

    private void avisarError(Exception e, int mensajeGenerico) {
        int mensaje = (e instanceof FirebaseNetworkException)
                ? R.string.contrasena_error_red : mensajeGenerico;
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }
}
