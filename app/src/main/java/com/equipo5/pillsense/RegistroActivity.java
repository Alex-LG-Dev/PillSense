package com.equipo5.pillsense;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class RegistroActivity extends AppCompatActivity {

    private static final String TAG = "RegistroGoogle";

    private EditText etNombre;
    private EditText etCorreo;
    private EditText etContrasena;
    private RadioGroup rgRol;
    private CheckBox cbTerminos;

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        // Enlazamos las vistas del layout con las variables
        etNombre = findViewById(R.id.etNombre);
        etCorreo = findViewById(R.id.etCorreoRegistro);
        etContrasena = findViewById(R.id.etContrasenaRegistro);
        rgRol = findViewById(R.id.rgRol);
        cbTerminos = findViewById(R.id.cbTerminos);

        // Firebase y Credential Manager
        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        // Flecha de volver: cierra la pantalla
        ImageButton btnVolver = findViewById(R.id.btnVolver);
        btnVolver.setOnClickListener(v -> finish());

        //  Inicia sesión abre el login y cierra el registro
        TextView tvIrALogin = findViewById(R.id.tvIrALogin);
        tvIrALogin.setOnClickListener(v -> {
            startActivity(new Intent(RegistroActivity.this, LoginActivity.class));
            finish();
        });

        // Btn Crear Cuenta
        Button btnCrearCuenta = findViewById(R.id.btnCrearCuenta);
        btnCrearCuenta.setOnClickListener(v -> crearCuenta());

        // Btn Google
        Button btnGoogleRegistro = findViewById(R.id.btnGoogleRegistro);
        btnGoogleRegistro.setOnClickListener(v -> registrarseConGoogle());
    }

    private void crearCuenta() {
        String nombre = etNombre.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();
        String contrasena = etContrasena.getText().toString();

        if (nombre.isEmpty()) {
            etNombre.setError("Introduce tu nombre");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.setError("Correo no válido");
            return;
        }
        if (contrasena.length() < 8) {
            etContrasena.setError("Mínimo 8 caracteres");
            return;
        }
        if (!cbTerminos.isChecked()) {
            Toast.makeText(this, "Debes aceptar los términos", Toast.LENGTH_SHORT).show();
            return;
        }

        // rol paciente o cuidador
        String rol = (rgRol.getCheckedRadioButtonId() == R.id.rbPaciente) ? "paciente" : "cuidador";

        // crea usuario en Firebase Auth y guardar el rol en la bbdd
        Toast.makeText(this, "Cuenta de " + rol + " lista para crear", Toast.LENGTH_SHORT).show();
    }

    //Comprueba los términos y abre la ventana de Google para elegir la cuenta
    private void registrarseConGoogle() {
        // Al ser un registro, también debe aceptar los términos
        if (!cbTerminos.isChecked()) {
            Toast.makeText(this, "Debes aceptar los términos", Toast.LENGTH_SHORT).show();
            return;
        }

        GetSignInWithGoogleOption opcionGoogle = new GetSignInWithGoogleOption.Builder(
                getString(R.string.default_web_client_id)
        ).build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(opcionGoogle)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        manejarCredencial(result.getCredential());
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Log.e(TAG, "Error al obtener la credencial de Google", e);
                        Toast.makeText(RegistroActivity.this,
                                "No se pudo registrar con Google", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    // Recibe Google y se lo pasa a Firebase (crea el usuario si es nuevo)
    private void manejarCredencial(Credential credential) {
        if (credential instanceof CustomCredential
                && credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {

            GoogleIdTokenCredential googleCredential =
                    GoogleIdTokenCredential.createFrom(credential.getData());

            AuthCredential firebaseCredential =
                    GoogleAuthProvider.getCredential(googleCredential.getIdToken(), null);

            mAuth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();


                            String rol = (rgRol.getCheckedRadioButtonId() == R.id.rbPaciente)
                                    ? "paciente" : "cuidador";

                            Toast.makeText(this, "Bienvenido, " + user.getDisplayName(),
                                    Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegistroActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Log.e(TAG, "Error al registrarse en Firebase", task.getException());
                            Toast.makeText(this, "Error al registrarse con Google",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Log.e(TAG, "Tipo de credencial inesperado");
        }
    }
}