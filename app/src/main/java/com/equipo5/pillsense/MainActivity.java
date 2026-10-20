package com.equipo5.pillsense;

import android.content.Intent; // necesario para navegar a otras pantallas
import android.os.Bundle;
import android.util.Log;
import android.widget.Button; // para usar los dos botones del layout

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
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