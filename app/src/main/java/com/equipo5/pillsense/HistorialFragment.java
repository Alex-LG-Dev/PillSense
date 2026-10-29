package com.equipo5.pillsense;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

/** Pestaña "Historial". De momento muestra la semana de ejemplo del diseño. */
public class HistorialFragment extends Fragment {

    // Estados de cada toma: T = tomada, R = tarde (retraso), O = olvidada, P = pendiente
    private final String[] dias = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
    private final String[] semana = {
            "TTTT",   // Lun
            "TTOT",   // Mar
            "TTRT",   // Mié
            "TTTT",   // Jue
            "TTTT",   // Vie
            "RTTO",   // Sáb
            "TTPP"    // Dom (hoy)
    };
    private static final int HOY = 6; // posición del día de hoy en la lista

    public HistorialFragment() {
        super(R.layout.fragment_historial);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout tabla = view.findViewById(R.id.tablaSemana);
        float densidad = getResources().getDisplayMetrics().density;

        // Una fila por día: nombre del día + 4 iconos (uno por hora)
        for (int d = 0; d < dias.length; d++) {
            LinearLayout fila = new LinearLayout(requireContext());
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(Gravity.CENTER_VERTICAL);
            fila.setPadding(0, Math.round(8 * densidad), 0, Math.round(8 * densidad));

            // Nombre del día (hoy en negrita)
            TextView tvDia = new TextView(requireContext());
            tvDia.setText(dias[d]);
            tvDia.setTextSize(15);
            tvDia.setTextColor(ContextCompat.getColor(requireContext(),
                    d == HOY ? R.color.texto_principal : R.color.texto_secundario));
            if (d == HOY) tvDia.setTypeface(tvDia.getTypeface(), Typeface.BOLD);
            fila.addView(tvDia, new LinearLayout.LayoutParams(Math.round(56 * densidad),
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            // 4 celdas con su icono, centradas en su columna
            for (int h = 0; h < 4; h++) {
                FrameLayout celda = new FrameLayout(requireContext());
                ImageView icono = new ImageView(requireContext());
                ponerIcono(icono, semana[d].charAt(h));
                int tam = Math.round(28 * densidad);
                celda.addView(icono, new FrameLayout.LayoutParams(tam, tam, Gravity.CENTER));
                fila.addView(celda, new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            }

            tabla.addView(fila);
        }
    }

    /** Elige icono y color según el estado de la toma. */
    private void ponerIcono(ImageView icono, char estado) {
        int verde = ContextCompat.getColor(requireContext(), R.color.verde_menta);
        switch (estado) {
            case 'T': // Tomada: check verde
                icono.setImageResource(R.drawable.ic_toma_hecha);
                icono.setColorFilter(verde);
                break;
            case 'O': // Olvidada: aspa azul
                icono.setImageResource(R.drawable.ic_toma_olvidada);
                icono.setColorFilter(ContextCompat.getColor(requireContext(), R.color.toma_olvidada));
                break;
            case 'R': // Tarde: círculo verde hueco
                icono.setImageResource(R.drawable.ic_toma_pendiente);
                icono.setColorFilter(verde);
                break;
            default:  // Pendiente: círculo gris hueco
                icono.setImageResource(R.drawable.ic_toma_pendiente);
                icono.setColorFilter(ContextCompat.getColor(requireContext(), R.color.pestana_inactiva));
                break;
        }
    }
}
