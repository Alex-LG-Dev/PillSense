package com.equipo5.pillsense;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

/** Pestaña "Hoy". De momento muestra datos de ejemplo del diseño. */
public class HoyFragment extends Fragment {

    // Datos de ejemplo (más adelante vendrán de Firebase)
    private final String[] horas   = {"08:00", "09:00", "14:00", "22:00"};
    private final String[] nombres = {"Omeprazol", "Enalapril", "Ibuprofeno", "Simvastatina"};
    private final String[] estados = {"Tomada", "Tomada", "Siguiente", "Pendiente"};

    public HoyFragment() {
        super(R.layout.fragment_hoy);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout lista = view.findViewById(R.id.listaTomas);
        LayoutInflater inflater = getLayoutInflater();

        int verde = ContextCompat.getColor(requireContext(), R.color.verde_menta);
        int gris = ContextCompat.getColor(requireContext(), R.color.pestana_inactiva);

        // Una fila (item_toma.xml) por cada toma
        for (int i = 0; i < horas.length; i++) {
            View fila = inflater.inflate(R.layout.item_toma, lista, false);

            TextView tvHora = fila.findViewById(R.id.tvHora);
            TextView tvNombre = fila.findViewById(R.id.tvNombre);
            TextView tvEstado = fila.findViewById(R.id.tvEstado);
            ImageView ivEstado = fila.findViewById(R.id.ivEstado);

            tvHora.setText(horas[i]);
            tvNombre.setText(nombres[i]);
            tvEstado.setText(estados[i]);

            // Icono y color según el estado de la toma
            switch (estados[i]) {
                case "Tomada":
                    ivEstado.setImageResource(R.drawable.ic_toma_hecha);
                    ivEstado.setColorFilter(verde);
                    break;
                case "Siguiente":
                    ivEstado.setImageResource(R.drawable.ic_toma_pendiente);
                    ivEstado.setColorFilter(verde);
                    tvHora.setTextColor(verde);
                    tvEstado.setTextColor(verde);
                    break;
                default: // Pendiente
                    ivEstado.setImageResource(R.drawable.ic_toma_pendiente);
                    ivEstado.setColorFilter(gris);
                    break;
            }

            // La última fila no lleva línea debajo
            if (i == horas.length - 1) {
                fila.findViewById(R.id.separador).setVisibility(View.GONE);
            }

            lista.addView(fila);
        }
    }
}
