package com.equipo5.pillsense;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DrawerBaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    private FirebaseAuth mAuth;

    @Override
    public void setContentView(int layoutResID) {
        drawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_base_drawer, null);
        FrameLayout contenedor = drawerLayout.findViewById(R.id.contenedor_actividad);
        getLayoutInflater().inflate(layoutResID, contenedor, true);
        super.setContentView(drawerLayout);

        mAuth = FirebaseAuth.getInstance();
        configurarMenu();
    }

    private void configurarMenu() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        MaterialToolbar barra = findViewById(R.id.barraAcciones);

        if (barra != null) {
            setSupportActionBar(barra);
            // Cambiamos el título a "Menú"
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Menú");
            }

            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, barra, R.string.abrir_menu, R.string.cerrar_menu);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        }

        if (navigationView != null) {
            View headerView = navigationView.getHeaderView(0);
            TextView tvNombreUsuario = headerView.findViewById(R.id.tvNombreUsuarioMenu);
            TextView btnCerrarMenu = headerView.findViewById(R.id.btnCerrarMenu);

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null && currentUser.getDisplayName() != null && tvNombreUsuario != null) {
                tvNombreUsuario.setText(currentUser.getDisplayName());
            }

            if (btnCerrarMenu != null) {
                btnCerrarMenu.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
            }

            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_perfil) {
                    startActivity(new Intent(this, PerfilActivity.class));
                } else if (id == R.id.nav_acerca_de) {
                    startActivity(new Intent(this, AcercaDeActivity.class));
                } else if (id == R.id.nav_cerrar_sesion) {
                    cerrarSesion();
                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            });
        }
    }

    private void cerrarSesion() {
        mAuth.signOut();
        SharedPreferences preferencias = getSharedPreferences("PillSensePrefs", Context.MODE_PRIVATE);
        preferencias.edit().putBoolean("mantener_sesion", false).apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}