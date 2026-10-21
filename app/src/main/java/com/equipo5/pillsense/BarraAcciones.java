package com.equipo5.pillsense;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.MenuRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

/**
 * Ayuda para poner la barra de acciones (layout app_bar) en cualquier pantalla.
 *
 * Uso en una Activity:
 *   BarraAcciones.prepararVentana(this);           // antes de setContentView
 *   setContentView(R.layout.mi_pantalla);           // el layout incluye app_bar y tiene un contenedor raíz con id "main"
 *   MaterialToolbar barra = BarraAcciones.configurar(this, R.string.mi_titulo, false, R.menu.menu_principal);
 */
public final class BarraAcciones {

    private BarraAcciones() { }

    /** Pantalla de borde a borde. La parte de arriba es azul oscuro, así que los iconos de la barra de estado (hora, batería, notificaciones) van en blanco. */
    public static void prepararVentana(AppCompatActivity activity) {
        EdgeToEdge.enable(
                activity,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT));
    }

    /**
     * @param titulo     título de la barra
     * @param conVolver  true para mostrar la flecha de volver (pantallas secundarias)
     * @param menu       menú de la barra (los tres puntos), o 0 si no tiene
     */
    public static MaterialToolbar configurar(AppCompatActivity activity, @StringRes int titulo,
                                             boolean conVolver, @MenuRes int menu) {
        View raiz = activity.findViewById(R.id.main);
        View contenedorBarra = activity.findViewById(R.id.contenedorBarra);

        // El contenedor azul oscuro llega hasta arriba del todo (por detrás de la barra de estado)
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, 0, barras.right, barras.bottom);
            contenedorBarra.setPadding(0, barras.top, 0, 0);
            return insets;
        });

        MaterialToolbar barra = activity.findViewById(R.id.barraAcciones);
        barra.setTitle(titulo);

        if (conVolver) {
            barra.setNavigationIcon(R.drawable.ic_volver_barra);
            barra.setNavigationContentDescription(R.string.volver);
            barra.setNavigationOnClickListener(v ->
                    activity.getOnBackPressedDispatcher().onBackPressed());
        }

        if (menu != 0) {
            barra.inflateMenu(menu);
            // Icono de los tres puntos en blanco, como el título
            Drawable puntos = barra.getOverflowIcon();
            if (puntos != null) {
                Drawable tenido = DrawableCompat.wrap(puntos.mutate());
                DrawableCompat.setTint(tenido, ContextCompat.getColor(activity, R.color.white));
                barra.setOverflowIcon(tenido);
            }
        }
        return barra;
    }
}
