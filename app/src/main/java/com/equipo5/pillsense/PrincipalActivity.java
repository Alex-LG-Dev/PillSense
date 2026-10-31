package com.equipo5.pillsense;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PrincipalActivity extends AppCompatActivity {

    // Texto e icono de cada pestaña, en el mismo orden que el adaptador
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BarraAcciones.prepararVentana(this);
        setContentView(R.layout.activity_principal);

        mAuth = FirebaseAuth.getInstance();

        // 1. Configuración del menú lateral (Drawer) y vistas
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // 2. Barra de acciones y toggle del menú lateral
        MaterialToolbar barra = BarraAcciones.configurar(this, R.string.app_name, false, R.menu.menu_con_sesion);
        if (drawerLayout != null) {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, barra, R.string.abrir_menu, R.string.cerrar_menu);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();

            barra.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.accion_perfil) {
                    startActivity(new Intent(PrincipalActivity.this, PerfilActivity.class));
                    return true;
                }
                if (item.getItemId() == R.id.accion_acerca_de) {
                    startActivity(new Intent(PrincipalActivity.this, AcercaDeActivity.class));
                    return true;
                }
                return false;
            });
        }

        // Cargar los datos dinámicos del usuario (nombre, correo y foto) en la cabecera
        configurarCabeceraMenu();

        // Manejar las opciones al pulsar elementos del menú lateral
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_perfil || id == R.id.accion_perfil) {
                    startActivity(new Intent(PrincipalActivity.this, PerfilActivity.class));
                } else if (id == R.id.nav_acerca_de) {
                    startActivity(new Intent(PrincipalActivity.this, AcercaDeActivity.class));
                } else if (id == R.id.nav_cerrar_sesion) {
                    cerrarSesion();
                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            });
        }

        // 3. Pestañas: el ViewPager2 muestra las pantallas y la TabLayout los botones de abajo
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabs = findViewById(R.id.tabs);
        if (viewPager != null) {
            viewPager.setAdapter(new PestanasAdapter(this));
        }

        if (tabs != null && viewPager != null) {
            // Une las pestañas con las pantallas: tocar o deslizar cambia las dos a la vez
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
                // Carga la foto de perfil en círculo usando Glide
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .circleCrop()
                        .into(ivFotoPerfil);
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