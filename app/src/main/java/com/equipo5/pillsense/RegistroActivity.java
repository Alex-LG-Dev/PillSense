package com.equipo5.pillsense;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegistroActivity extends AppCompatActivity {

    private EditText etNombre;
    private EditText etCorreo;
    private EditText etContrasena;
    private RadioGroup rgRol;
    private CheckBox cbTerminos;

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

        // Flecha de volver: cierra la pantalla
        ImageButton btnVolver = findViewById(R.id.btnVolver);
        btnVolver.setOnClickListener(v -> finish());

        //  "Inicia sesión" abre el login y cierra el registro
        TextView tvIrALogin = findViewById(R.id.tvIrALogin);
        tvIrALogin.setOnClickListener(v -> {
            startActivity(new Intent(RegistroActivity.this, LoginActivity.class));
            finish();
        });

        // Btn "Crear cuenta"
        Button btnCrearCuenta = findViewById(R.id.btnCrearCuenta);
        btnCrearCuenta.setOnClickListener(v -> crearCuenta());
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

        // Rol paciente o cuidador
        String rol = (rgRol.getCheckedRadioButtonId() == R.id.rbPaciente) ? "paciente" : "cuidador";

        // crea usuario en Firebase Auth y guardar el rol en la base de datos
        Toast.makeText(this, "Cuenta de " + rol + " lista para crear", Toast.LENGTH_SHORT).show();
    }
}