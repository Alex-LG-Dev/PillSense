package com.equipo5.pillsense;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginGoogle";

    private EditText etCorreo;
    private EditText etContrasena;
    private CheckBox cbMantenerSesion;
    private TextView tvError;

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;

    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        cbMantenerSesion = findViewById(R.id.cbMantenerSesion);
        tvError = findViewById(R.id.tvError);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        //CallbackManager para Facebook
        callbackManager = CallbackManager.Factory.create();

        // Botón "Iniciar sesión" valida los campos
        Button btnIniciarSesion = findViewById(R.id.btnIniciarSesion);
        btnIniciarSesion.setOnClickListener(v -> iniciarSesion());

        // Botón "Continuar con Google"
        Button btnGoogle = findViewById(R.id.btnGoogle);
        btnGoogle.setOnClickListener(v -> iniciarSesionConGoogle());

        // Configurar botón y el Callback para Facebook
        Button btnFacebook = findViewById(R.id.btnFacebook);
        btnFacebook.setOnClickListener(v -> iniciarSesionConFacebook());

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        manejarCredencialFacebook(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "Login con Facebook cancelado");
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Log.e(TAG, "Error en login con Facebook", error);
                        Toast.makeText(LoginActivity.this,
                                "No se pudo iniciar sesión con Facebook", Toast.LENGTH_SHORT).show();
                    }
                });

        // Enlace "Regístrate" abre el registro y cierra el login
        TextView tvIrARegistro = findViewById(R.id.tvIrARegistro);
        tvIrARegistro.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegistroActivity.class));
            finish(); // evita acumular pantallas al pulsar atrás
        });
    }

    private void iniciarSesion() {
        String correo = etCorreo.getText().toString().trim();
        String contrasena = etContrasena.getText().toString();

        boolean correoValido = Patterns.EMAIL_ADDRESS.matcher(correo).matches();
        boolean contrasenaValida = contrasena.length() >= 8;

        if (correoValido && contrasenaValida) {
            tvError.setVisibility(View.GONE);

            // Entra con el correo y la contraseña de la cuenta (la nueva, si se ha cambiado)
            mAuth.signInWithEmailAndPassword(correo, contrasena)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            startActivity(new Intent(LoginActivity.this, PrincipalActivity.class));
                            finish();
                        } else {
                            Log.e(TAG, "Error al iniciar sesión con correo", task.getException());
                            tvError.setVisibility(View.VISIBLE);
                        }
                    });
        } else {
            // Muestra el recuadro de error
            tvError.setVisibility(View.VISIBLE);
        }
    }

    //Abre la ventana de Google para elegir la cuenta
    private void iniciarSesionConGoogle() {
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
                        Toast.makeText(LoginActivity.this,
                                "No se pudo iniciar sesión con Google", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    //Recibe el token de Google y se lo pasa a Firebase
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
                            Toast.makeText(this, "Hola, " + user.getDisplayName(),
                                    Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, PrincipalActivity.class));
                            finish();
                        } else {
                            Log.e(TAG, "Error al iniciar sesión en Firebase", task.getException());
                            tvError.setVisibility(View.VISIBLE);
                        }
                    });
        } else {
            Log.e(TAG, "Tipo de credencial inesperado");
        }
    }

    //Abre la ventana de Facebook para confirmar la cuenta (si ya tiene una en el dispositivo)
    private void iniciarSesionConFacebook() {
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
    }

    //Recibe el token de Facebook y se lo pasa a Firebase
    private void manejarCredencialFacebook(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Hola, " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, PrincipalActivity.class));
                        finish();
                    } else {
                        Log.e(TAG, "Error al iniciar sesión en Firebase con FB", task.getException());
                        tvError.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}