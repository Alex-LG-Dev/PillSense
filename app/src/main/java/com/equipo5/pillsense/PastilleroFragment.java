package com.equipo5.pillsense;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

/** Pestaña "Pastillero". De momento muestra datos de ejemplo del diseño. */
public class PastilleroFragment extends Fragment {

    // Datos de ejemplo de los 4 cajones (más adelante vendrán de Firebase)
    private final String[] momentos = {"Mañana", "Desayuno", "Comida", "Noche"};
    private final String[] estados  = {"Ya tomada", "Ya tomada", "Luz encendida · 14:00", "A las 22:00"};
    private final boolean[] luzEncendida = {false, false, true, false};

    public PastilleroFragment() {
        super(R.layout.fragment_pastillero);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int[] idsCajones = {R.id.cajon1, R.id.cajon2, R.id.cajon3, R.id.cajon4};
        for (int i = 0; i < idsCajones.length; i++) {
            rellenarCajon(view.findViewById(idsCajones[i]), i);
        }

        // De momento el botón solo avisa; cuando conectemos con el pastillero enviará la orden
        Button btnHacerSonar = view.findViewById(R.id.btnHacerSonar);
        btnHacerSonar.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Haciendo sonar el pastillero…", Toast.LENGTH_SHORT).show());
    }

    /** Pone los textos de un cajón y lo resalta si tiene la luz encendida. */
    private void rellenarCajon(MaterialCardView tarjeta, int i) {
        TextView tvNumero = tarjeta.findViewById(R.id.tvNumero);
        TextView tvMomento = tarjeta.findViewById(R.id.tvMomento);
        TextView tvEstado = tarjeta.findViewById(R.id.tvEstadoCajon);
        View vPunto = tarjeta.findViewById(R.id.vPunto);

        tvNumero.setText(String.valueOf(i + 1));
        tvMomento.setText(momentos[i]);
        tvEstado.setText(estados[i]);

        if (luzEncendida[i]) {
            // Cajón activo: fondo azul, borde verde, textos blancos y punto verde relleno
            int blanco = ContextCompat.getColor(requireContext(), R.color.white);
            tarjeta.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.azul_pillsense));
            tarjeta.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.verde_menta));
            tarjeta.setStrokeWidth(Math.round(2 * getResources().getDisplayMetrics().density));
            tvNumero.setTextColor(blanco);
            tvMomento.setTextColor(blanco);
            tvEstado.setTextColor(blanco);
            vPunto.setBackgroundResource(R.drawable.punto_lleno);
        }
    }
}
