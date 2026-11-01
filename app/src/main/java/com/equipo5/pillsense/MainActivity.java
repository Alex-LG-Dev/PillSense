package com.equipo5.pillsense;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PillSense";

    // Splash screen: tiempo mínimo que se ve (ms) y duración de la animación de salida
    private static final long SPLASH_MINIMO_MS = 800;
    private static final long SPLASH_SALIDA_MS = 300;
    private static boolean splashYaMostrada = false;
    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    private DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Splash screen
        SplashScreen splash = SplashScreen.installSplashScreen(this);
        if (!splashYaMostrada) {
            splashYaMostrada = true;
            final long inicio = SystemClock.uptimeMillis();
            splash.setKeepOnScreenCondition(() -> SystemClock.uptimeMillis() - inicio < SPLASH_MINIMO_MS);
        }
        splash.setOnExitAnimationListener(proveedor -> {
            View icono = proveedor.getIconView();
            if (icono != null) {
                icono.animate().scaleX(1.15f).scaleY(1.15f).setDuration(SPLASH_SALIDA_MS).start();
            }
            proveedor.getView().animate().alpha(0f)
                    .setDuration(SPLASH_SALIDA_MS)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(proveedor::remove)
                    .start();
        });

        super.onCreate(savedInstanceState);

        // Si ya hay una sesión iniciada, vamos a la pantalla que toca según el rol
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Navegacion.abrirPantallaInicio(this);
            return;
        }

        BarraAcciones.prepararVentana(this);
        setContentView(R.layout.activity_main);
        BarraAcciones.prepararVentana(this);
        setContentView(R.layout.activity_main);

        // Barra de acciones: nombre de la app y menú (⋮) con "Acerca de"
        MaterialToolbar barra = BarraAcciones.configurar(this, R.string.app_name, false, R.menu.menu_principal);
        if (barra != null) {
            barra.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.accion_acerca_de) {
                    startActivity(new Intent(MainActivity.this, AcercaDeActivity.class));
                    return true;
                }
                return false;
            });
        }

        // Conexión con la base de datos
        db = FirebaseDatabase.getInstance(DB_URL).getReference();

        Button btnIniciarSesion = findViewById(R.id.btnIniciarSesion);
        Button btnRegistrarse = findViewById(R.id.btnRegistrarse);

        if (btnIniciarSesion != null) {
            btnIniciarSesion.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, LoginActivity.class)));
        }

        if (btnRegistrarse != null) {
            btnRegistrarse.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, RegistroActivity.class)));
        }
    }
}