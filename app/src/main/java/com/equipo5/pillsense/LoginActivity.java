package com.equipo5.pillsense;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etCorreo;
    private EditText etContrasena;
    private CheckBox cbMantenerSesion;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Enlazamos las vistas del layout con las variables
        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        cbMantenerSesion = findViewById(R.id.cbMantenerSesion);
        tvError = findViewById(R.id.tvError);

        // Botón "Iniciar sesión" valida los campos
        Button btnIniciarSesion = findViewById(R.id.btnIniciarSesion);
        btnIniciarSesion.setOnClickListener(v -> iniciarSesion());

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

        } else {
            // Muestra el recuadro de error
            tvError.setVisibility(View.VISIBLE);
        }
    }
}