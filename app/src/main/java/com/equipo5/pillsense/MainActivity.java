package com.equipo5.pillsense;

import android.content.Intent; // necesario para navegar a otras pantallas
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button; // para usar los dos botones del layout

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.appbar.MaterialToolbar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PillSense";

    // Splash screen: tiempo mínimo que se ve (ms) y duración de la animación de salida
    private static final long SPLASH_MINIMO_MS = 800;
    private static final long SPLASH_SALIDA_MS = 300;
    // Solo se aplica en el arranque en frío (la primera vez que se abre la app)
    private static boolean splashYaMostrada = false;
    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    // Referencia a la raíz de la base de datos, disponible en toda la clase
    private DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Splash screen: debe instalarse ANTES de super.onCreate()
        SplashScreen splash = SplashScreen.installSplashScreen(this);
        if (!splashYaMostrada) {
            splashYaMostrada = true;
            final long inicio = SystemClock.uptimeMillis();
            // La splash se mantiene hasta que pasa el tiempo mínimo
            splash.setKeepOnScreenCondition(
                    () -> SystemClock.uptimeMillis() - inicio < SPLASH_MINIMO_MS);
        }
        // Salida: el logo crece un poco y toda la splash se funde con la pantalla siguiente
        splash.setOnExitAnimationListener(proveedor -> {
            View icono = proveedor.getIconView();
            if (icono != null) {
                icono.animate().scaleX(1.15f).scaleY(1.15f)
                        .setDuration(SPLASH_SALIDA_MS).start();
            }
            proveedor.getView().animate().alpha(0f)
                    .setDuration(SPLASH_SALIDA_MS)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(proveedor::remove)
                    .start();
        });

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