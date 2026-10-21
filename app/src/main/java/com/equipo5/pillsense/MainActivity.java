package com.equipo5.pillsense;

import android.content.Intent; // necesario para navegar a otras pantallas
import android.os.Bundle;
import android.util.Log;
import android.widget.Button; // para usar los dos botones del layout

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PillSense";
    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    // Referencia a la raíz de la base de datos, disponible en toda la clase
    private DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BarraAcciones.prepararVentana(this);
        setContentView(R.layout.activity_main);

        // Barra de acciones: nombre de la app y menú (⋮) con "Acerca de"
        MaterialToolbar barra = BarraAcciones.configurar(this, R.string.app_name, false, R.menu.menu_principal);
        barra.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.accion_acerca_de) {
                startActivity(new Intent(MainActivity.this, AcercaDeActivity.class));
                return true;
            }
            return false;
        });

        // Conexión con la base de datos
        db = FirebaseDatabase.getInstance(DB_URL).getReference();

        // Prueba de conexión (si funciona):
        // db.child("test").setValue("Hola PillSense");

        //Buscamos los dos botones del layout activity_main.xml
        Button btnIniciarSesion = findViewById(R.id.btnIniciarSesion);
        Button btnRegistrarse = findViewById(R.id.btnRegistrarse);

        //Al pulsar "Iniciar sesión" se abre LoginActivity
        btnIniciarSesion.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LoginActivity.class)));

        //Al pulsar "Registrarse" se abre RegistroActivity
        btnRegistrarse.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RegistroActivity.class)));

    }
}