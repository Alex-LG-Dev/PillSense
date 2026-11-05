package com.equipo5.pillsense;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;



import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;



public class PrincipalActivity extends AppCompatActivity {

    private final String[] titulos = {"Hoy", "Pastillero", "Pastillas", "Historial"};
    private final int[] iconos = {
            R.drawable.ic_pestana_hoy,
            R.drawable.ic_pestana_pastillero,
            R.drawable.ic_pestana_pastillas,
            R.drawable.ic_pestana_historial
    };

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FirebaseAuth mAuth;
    private DatabaseReference dbUsuarios;
    private String uidMostrado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BarraAcciones.prepararVentana(this);
        setContentView(R.layout.activity_principal);
        // Baja los botones de arriba (Menú, paciente y Avisos) para que no queden debajo de la hora y la cámara
        View barraSuperior = findViewById(R.id.layoutBotonesSuperiores);
        int paddingArribaOriginal = barraSuperior.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(barraSuperior, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), paddingArribaOriginal + barras.top,
                    v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        dbUsuarios = FirebaseDatabase.getInstance("https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/").getReference("usuarios");

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabs = findViewById(R.id.tabs);

        // Vincular el botón superior de "Menú"
        Button btnAbrirMenu = findViewById(R.id.btnAbrirMenu);
        if (btnAbrirMenu != null && drawerLayout != null) {
            btnAbrirMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        // Vincular el botón superior de "Avisos" restaurado
        Button btnAvisos = findViewById(R.id.btnAvisos);
        if (btnAvisos != null) {
            btnAvisos.setOnClickListener(v ->
                    android.widget.Toast.makeText(this, "Pantalla Avisos en construcción", android.widget.Toast.LENGTH_SHORT).show()
            );
        }
        // Paciente activo: se elige des del menú
        uidMostrado = PacienteActivo.uidDatos(this);
        elegirPrimeroSiNoHayNinguno();
        configurarCabeceraMenu();

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_perfil) {
                    startActivity(new Intent(PrincipalActivity.this, PerfilActivity.class));
                } else if (id == R.id.nav_mis_pacientes) {
                    startActivity(new Intent(PrincipalActivity.this, MisPacientesActivity.class));
                } else if (id == R.id.nav_pastillero) {
                    if (viewPager != null) {
                        viewPager.setCurrentItem(1, true);
                    }
                } else if (id == R.id.nav_ayuda) {
                    android.widget.Toast.makeText(this, "Pantalla Ayuda en construcción", android.widget.Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_acerca_de) {
                    startActivity(new Intent(PrincipalActivity.this, AcercaDeActivity.class));
                } else if (id == R.id.nav_cerrar_sesion) {
                    cerrarSesion();
                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            });
        }

        if (viewPager != null) {
            viewPager.setAdapter(new PestanasAdapter(this));
        }

        if (tabs != null && viewPager != null) {
            new TabLayoutMediator(tabs, viewPager, (tab, posicion) -> {
                tab.setText(titulos[posicion]);
                tab.setIcon(iconos[posicion]);
            }).attach();
        }
    }

    private void configurarCabeceraMenu() {
        if (navigationView == null) return;
        View headerView = navigationView.getHeaderView(0);
        if (headerView == null) return;

        TextView tvNombreUsuario = headerView.findViewById(R.id.tvNombreUsuarioMenu);
        TextView tvCorreoMenu = headerView.findViewById(R.id.tvCorreoMenu);
        TextView tvRolMenu = headerView.findViewById(R.id.tvRolMenu);
        ImageView ivFotoPerfil = headerView.findViewById(R.id.ivFotoPerfilMenu);
        TextView btnCerrarMenu = headerView.findViewById(R.id.btnCerrarMenu);

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            if (tvNombreUsuario != null && currentUser.getDisplayName() != null) {
                tvNombreUsuario.setText(currentUser.getDisplayName());
            }
            if (tvCorreoMenu != null && currentUser.getEmail() != null) {
                tvCorreoMenu.setText(currentUser.getEmail());
            }
            // Si el usuario ha puesto su foto (T037), se ve en lugar de la de Google/Facebook
            if (ivFotoPerfil != null) {
                dbUsuarios.child(currentUser.getUid()).child("foto").get().addOnSuccessListener(datos -> {
                    if (isFinishing() || isDestroyed()) return;
                    Bitmap foto = FotoUtils.deBase64(datos.getValue(String.class));
                    if (foto != null) {
                        Glide.with(this).load(foto).circleCrop().into(ivFotoPerfil);
                    }
                });
            }

            if (tvRolMenu != null) {
                dbUsuarios.child(currentUser.getUid()).child("rol").get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        String rol = task.getResult().getValue(String.class);
                        if (rol != null) {
                            String rolCapitalizado = rol.substring(0, 1).toUpperCase() + rol.substring(1);
                            tvRolMenu.setText(rolCapitalizado);
                        }
                    }
                });
            }
        }

        if (btnCerrarMenu != null) {
            btnCerrarMenu.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        }
    }

    /** Al volver a esta pantalla (por ejemplo, desde "Mis pacientes"): si ha cambiado el paciente, recargamos. */
    @Override
    protected void onResume() {
        super.onResume();
        configurarCabeceraMenu();   // por si ha cambiado la foto o el nombre
        String uidAhora = PacienteActivo.uidDatos(this);
        if (uidAhora != null && !uidAhora.equals(uidMostrado)) {
            recreate();   // vuelve a cargar todas las pestañas con el paciente nuevo
        }
    }





    /** Si todavía no ha elegido ninguno pero tiene pacientes, elegimos el primero automáticamente. */
    private void elegirPrimeroSiNoHayNinguno() {
        if (PacienteActivo.hayElegido(this)) return;
        FirebaseUser yo = mAuth.getCurrentUser();
        if (yo == null) return;

        dbUsuarios.child(yo.getUid()).child("pacientes").get().addOnSuccessListener(datos -> {
            for (DataSnapshot paciente : datos.getChildren()) {
                PacienteActivo.elegir(this, paciente.getKey(), paciente.getValue(String.class));
                recreate();
                return;   // solo el primero
            }
        });
    }


    private void cerrarSesion() {
        PacienteActivo.olvidar(this);
        mAuth.signOut();
        SharedPreferences preferencias = getSharedPreferences("PillSensePrefs", Context.MODE_PRIVATE);
        preferencias.edit().putBoolean("mantener_sesion", false).apply();

        Intent intent = new Intent(PrincipalActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}