package com.equipo5.pillsense;

import android.content.Context;
import android.content.Intent; // necesario para navegar a otras pantallas
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button; // para usar los dos botones del layout
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

    // Variables para el menú lateral y Firebase Auth
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FirebaseAuth mAuth;

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

        // Inicializamos FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Barra de acciones: nombre de la app y menú (⋮) con "Acerca de"
        MaterialToolbar barra = BarraAcciones.configurar(this, R.string.app_name, false, R.menu.menu_principal);
        barra.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.accion_acerca_de) {
                startActivity(new Intent(MainActivity.this, AcercaDeActivity.class));
                return true;
            }
            return false;
        });

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        if (drawerLayout != null && navigationView != null) {

            // Vinculamos el DrawerLayout con la Toolbar (barra) para que aparezca la hamburguesa
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, barra, R.string.abrir_menu, R.string.cerrar_menu);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();

            // Configurar los datos del usuario en la cabecera del menú
            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                TextView tvNombreUsuario = headerView.findViewById(R.id.tvNombreUsuarioMenu);
                TextView btnCerrarMenu = headerView.findViewById(R.id.btnCerrarMenu);

                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null && currentUser.getDisplayName() != null && tvNombreUsuario != null) {
                    tvNombreUsuario.setText(currentUser.getDisplayName());
                }

                if (btnCerrarMenu != null) {
                    btnCerrarMenu.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
                }
            }

            // Configurar las acciones al pulsar las opciones del menú
            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();

                if (id == R.id.nav_perfil) {
                    startActivity(new Intent(MainActivity.this, PerfilActivity.class));
                } else if (id == R.id.nav_acerca_de) {
                    startActivity(new Intent(MainActivity.this, AcercaDeActivity.class));
                } else if (id == R.id.nav_cerrar_sesion) {
                    cerrarSesion();
                }

                // Cierra el menú tras tocar una opción
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            });
        }

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

    // Método extraído para limpiar la sesión
    private void cerrarSesion() {
        // 1. Cierra sesión en Firebase
        mAuth.signOut();

        // 2. Si usaste SharedPreferences para "Mantener sesión", bórralo (opcional)
        SharedPreferences preferencias = getSharedPreferences("PillSensePrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferencias.edit();
        editor.putBoolean("mantener_sesion", false);
        editor.apply();

        // 3. Vuelve a LoginActivity y limpia el historial para no volver atrás
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Cierra el menú con el botón "Atrás" físico del móvil si está desplegado
    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}