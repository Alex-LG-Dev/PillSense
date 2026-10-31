package com.equipo5.pillsense;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class PrincipalActivity extends AppCompatActivity {

    // Texto e icono de cada pestaña, en el mismo orden que el adaptador
    private final String[] titulos = {"Hoy", "Pastillero", "Pastillas", "Historial"};
    private final int[] iconos = {
            R.drawable.ic_pestana_hoy,
            R.drawable.ic_pestana_pastillero,
            R.drawable.ic_pestana_pastillas,
            R.drawable.ic_pestana_historial
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BarraAcciones.prepararVentana(this);
        setContentView(R.layout.activity_principal);

        // Barra de acciones: nombre de la app y menú (⋮) con "Acerca de"
        MaterialToolbar barra = BarraAcciones.configurar(this, R.string.app_name, false, R.menu.menu_principal);
        barra.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.accion_acerca_de) {
                startActivity(new Intent(PrincipalActivity.this, AcercaDeActivity.class));
                return true;
            }
            return false;
        });

        // Pestañas: el ViewPager2 muestra las pantallas y la TabLayout los botones de abajo
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabs = findViewById(R.id.tabs);
        viewPager.setAdapter(new PestanasAdapter(this));

        // Une las pestañas con las pantallas: tocar o deslizar cambia las dos a la vez
        new TabLayoutMediator(tabs, viewPager, (tab, posicion) -> {
            tab.setText(titulos[posicion]);
            tab.setIcon(iconos[posicion]);
        }).attach();
    }
}