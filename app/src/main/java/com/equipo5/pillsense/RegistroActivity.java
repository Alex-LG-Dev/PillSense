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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.firebase.auth.FacebookAuthProvider;
import java.util.Arrays;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegistroActivity extends AppCompatActivity {

    private static final String TAG = "RegistroGoogle";
    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    private EditText etNombre;
    private EditText etCorreo;
    private EditText etContrasena;
    private RadioGroup rgRol;
    private CheckBox cbTerminos;

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;

    private CallbackManager callbackManager;

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

        // AÑADIDO PARA FACEBOOK: Inicializar CallbackManager
        callbackManager = CallbackManager.Factory.create();

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

        // Btn Facebook
        Button btnFacebookRegistro = findViewById(R.id.btnFacebookRegistro);
        btnFacebookRegistro.setOnClickListener(v -> registrarseConFacebook());

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<>() {
                    @Override
                    public void onSuccess(@NonNull LoginResult loginResult) {
                        manejarCredencialFacebook(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "Registro con Facebook cancelado");
                    }

                    @Override
                    public void onError(@NonNull FacebookException error) {
                        Log.e(TAG, "Error en registro con Facebook", error);
                        Toast.makeText(RegistroActivity.this,
                                "No se pudo registrar con Facebook", Toast.LENGTH_SHORT).show();
                    }
                });
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

        // Crea el usuario en Firebase Auth y guarda nombre, rol y correo en la base de datos
        mAuth.createUserWithEmailAndPassword(correo, contrasena)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Error al crear la cuenta con correo", task.getException());
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            etCorreo.setError("Ya existe una cuenta con ese correo");
                        } else {
                            Toast.makeText(this, "No se pudo crear la cuenta", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    FirebaseUser user = task.getResult().getUser();
                    if (user == null) return;


                    user.updateProfile(new UserProfileChangeRequest.Builder()
                            .setDisplayName(nombre).build());
                    guardarDatosYEntrar(user, nombre);
                });
    }


    private void guardarDatosYEntrar(FirebaseUser user, String nombre) {
        String rol = (rgRol.getCheckedRadioButtonId() == R.id.rbPaciente) ? "paciente" : "cuidador";

        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", nombre);
        datos.put("rol", rol);
        datos.put("correo", user.getEmail());

        FirebaseDatabase.getInstance(DB_URL)
                .getReference("usuarios").child(user.getUid())
                .updateChildren(datos)
                .addOnCompleteListener(t -> {
                    Toast.makeText(this, "Bienvenido, " + nombre, Toast.LENGTH_SHORT).show();
                    Navegacion.abrirPantallaInicio(this);
                });
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
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(@NonNull GetCredentialResponse result) {
                        manejarCredencial(result.getCredential());
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
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
                            String nombre = (user != null && user.getDisplayName() != null)
                                    ? user.getDisplayName() : "";

                            if (user != null) guardarDatosYEntrar(user, nombre);
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

    //Comprueba los términos y abre la ventana de Facebook para elegir la cuenta
    private void registrarseConFacebook() {
        // Al ser un registro, también debe aceptar los términos
        if (!cbTerminos.isChecked()) {
            Toast.makeText(this, "Debes aceptar los términos", Toast.LENGTH_SHORT).show();
            return;
        }
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
    }

    // Recibe Facebook y se lo pasa a Firebase (crea el usuario si es nuevo)
    private void manejarCredencialFacebook(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String nombre = (user != null && user.getDisplayName() != null)
                                ? user.getDisplayName() : "";

                        if (user != null) guardarDatosYEntrar(user, nombre);
                    } else {
                        Log.e(TAG, "Error al registrarse en FB", task.getException());
                        Toast.makeText(this, "Error al registrarse con Facebook", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}