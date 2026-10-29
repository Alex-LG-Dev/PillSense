package com.equipo5.pillsense;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

/** Pestaña "Pastillas". De momento muestra datos de ejemplo del diseño. */
public class PastillasFragment extends Fragment {

    // Datos de ejemplo (más adelante vendrán de Firebase)
    private final String[] nombres  = {"Omeprazol", "Enalapril", "Ibuprofeno", "Simvastatina"};
    private final String[] horas    = {"08:00", "09:00", "14:00", "22:00"};
    private final String[] detalles = {
            "20 mg · en ayunas · cajón 1",
            "10 mg · cajón 2",
            "600 mg · después de comer · cajón 3",
            "20 mg · cajón 4"
    };
    private final int[] quedan = {18, 22, 3, 26};

    // Con estas pastillas o menos avisamos de que hay que comprar
    private static final int POCAS_PASTILLAS = 5;

    public PastillasFragment() {
        super(R.layout.fragment_pastillas);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvNum = view.findViewById(R.id.tvNumMedicamentos);
        tvNum.setText(nombres.length + " medicamentos");

        LinearLayout lista = view.findViewById(R.id.listaMedicamentos);
        LayoutInflater inflater = getLayoutInflater();
        int colorAviso = ContextCompat.getColor(requireContext(), R.color.aviso_stock);

        // Una tarjeta (item_medicamento.xml) por cada medicamento
        for (int i = 0; i < nombres.length; i++) {
            View tarjeta = inflater.inflate(R.layout.item_medicamento, lista, false);

            ((TextView) tarjeta.findViewById(R.id.tvNombreMed)).setText(nombres[i]);
            ((TextView) tarjeta.findViewById(R.id.tvHoraMed)).setText(horas[i]);
            ((TextView) tarjeta.findViewById(R.id.tvDetalleMed)).setText(detalles[i]);

            TextView tvQuedan = tarjeta.findViewById(R.id.tvQuedan);
            if (quedan[i] <= POCAS_PASTILLAS) {
                // Quedan pocas: aviso en verde y negrita
                tvQuedan.setText("Quedan " + quedan[i] + " · hay que comprar");
                tvQuedan.setTextColor(colorAviso);
                tvQuedan.setTypeface(tvQuedan.getTypeface(), Typeface.BOLD);
            } else {
                tvQuedan.setText("Quedan " + quedan[i]);
            }

            lista.addView(tarjeta);
        }

        // De momento el botón solo avisa; la pantalla de añadir medicamento es otra tarea
        Button btnAnadir = view.findViewById(R.id.btnAnadirMedicamento);
        btnAnadir.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Añadir medicamento (próximamente)", Toast.LENGTH_SHORT).show());
    }
}
