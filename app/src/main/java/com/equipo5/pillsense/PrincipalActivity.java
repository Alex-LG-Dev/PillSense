package com.equipo5.pillsense;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BarraAcciones.prepararVentana(this);
        setContentView(R.layout.activity_principal);

        mAuth = FirebaseAuth.getInstance();
        dbUsuarios = FirebaseDatabase.getInstance("https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/").getReference("Usuarios");

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Vincular o botón superior de "Menú"
        Button btnAbrirMenu = findViewById(R.id.btnAbrirMenu);
        if (btnAbrirMenu != null && drawerLayout != null) {
            btnAbrirMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        configurarCabeceraMenu();

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_perfil) {
                    startActivity(new Intent(PrincipalActivity.this, PerfilActivity.class));
                } else if (id == R.id.nav_pastillero) {
                    android.widget.Toast.makeText(this, "Pantalla Mi pastillero en construción", android.widget.Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_ayuda) {
                    android.widget.Toast.makeText(this, "Pantalla Ayuda en construción", android.widget.Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_acerca_de) {
                    startActivity(new Intent(PrincipalActivity.this, AcercaDeActivity.class));
                } else if (id == R.id.nav_cerrar_sesion) {
                    cerrarSesion();
                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            });
        }

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabs = findViewById(R.id.tabs);
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
            if (ivFotoPerfil != null && currentUser.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .circleCrop()
                        .into(ivFotoPerfil);
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

    private void cerrarSesion() {
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