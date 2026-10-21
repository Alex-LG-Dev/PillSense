package com.equipo5.pillsense;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

/** Pantalla "Acerca de": se abre desde el menú de la barra de acciones. */
public class AcercaDeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BarraAcciones.prepararVentana(this);
        setContentView(R.layout.activity_acerca_de);
        BarraAcciones.configurar(this, R.string.acerca_de_titulo, true, 0);

        // Versión real de la app (la de build.gradle)
        TextView tvVersion = findViewById(R.id.tvVersion);
        tvVersion.setText(getString(R.string.acerca_de_version, versionDeLaApp()));

        // Lista del equipo (los nombres están en strings.xml > integrantes_equipo)
        LinearLayout listaEquipo = findViewById(R.id.listaEquipo);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (String nombre : getResources().getStringArray(R.array.integrantes_equipo)) {
            View fila = inflater.inflate(R.layout.item_integrante, listaEquipo, false);
            ((TextView) fila.findViewById(R.id.tvNombreIntegrante)).setText(nombre);
            String inicial = nombre.isEmpty() ? "" : nombre.substring(0, 1).toUpperCase();
            ((TextView) fila.findViewById(R.id.tvInicial)).setText(inicial);
            listaEquipo.addView(fila);
        }

        // Enlaces: de momento solo avisan de que la pantalla aún no existe
        enlazar(R.id.enlaceContacto, R.string.acerca_de_contacto);
        enlazar(R.id.enlacePrivacidad, R.string.acerca_de_privacidad);
        enlazar(R.id.enlaceTerminos, R.string.acerca_de_terminos);
    }

    private void enlazar(@IdRes int idFila, @StringRes int idTexto) {
        findViewById(idFila).setOnClickListener(v ->
                Toast.makeText(this,
                        getString(R.string.acerca_de_pendiente, getString(idTexto)),
                        Toast.LENGTH_SHORT).show());
    }

    private String versionDeLaApp() {
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return version != null ? version : "";
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }
}
