package com.equipo5.pillsense;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/** Pestaña "Hoy": lista de tomas del usuario leída de Realtime Database, ordenada por hora. */
public class HoyFragment extends Fragment {

    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    private DatabaseReference refTomas;
    private ValueEventListener escuchador;

    private LinearLayout lista;
    private TextView tvSinTomas;

    public HoyFragment() {
        super(R.layout.fragment_hoy);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lista = view.findViewById(R.id.listaTomas);
        tvSinTomas = view.findViewById(R.id.tvSinTomas);

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario != null) {
            refTomas = FirebaseDatabase.getInstance(DB_URL)
                    .getReference("tomas")
                    .child(usuario.getUid());

            // Se ejecuta al abrir la pestaña y cada vez que cambia una toma en Firebase
            escuchador = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot datos) {
                    mostrarTomas(datos);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            };
            // las tomas llegan ordenadas de la más temprana a la más tardía
            refTomas.orderByChild("hora").addValueEventListener(escuchador);
        }

        // Botón flotante: abre el formulario para crear una toma nueva
        view.findViewById(R.id.fabNuevaToma).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), TomaActivity.class)));
    }

    @Override
    public void onDestroyView() {
        // Dejamos de escuchar cuando la pestaña desaparece
        if (refTomas != null && escuchador != null) {
            refTomas.orderByChild("hora").removeEventListener(escuchador);
        }
        super.onDestroyView();
    }


    private void mostrarTomas(DataSnapshot datos) {
        if (getView() == null) return;

        lista.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        int gris = ContextCompat.getColor(requireContext(), R.color.pestana_inactiva);
        View ultimaFila = null;

        for (DataSnapshot hijo : datos.getChildren()) {
            Toma toma = hijo.getValue(Toma.class);
            if (toma == null) continue;

            View fila = inflater.inflate(R.layout.item_toma, lista, false);
            ((TextView) fila.findViewById(R.id.tvHora)).setText(toma.getHora());
            ((TextView) fila.findViewById(R.id.tvNombre)).setText(toma.getNombreMedicamento());
            ((TextView) fila.findViewById(R.id.tvEstado)).setText(
                    getString(R.string.toma_detalle, toma.getCajon(), toma.getHoraLimite()));


            ImageView ivEstado = fila.findViewById(R.id.ivEstado);
            ivEstado.setImageResource(R.drawable.ic_toma_pendiente);
            ivEstado.setColorFilter(gris);

            // Al tocar la fila se abre el formulario para editar esa toma
            String id = hijo.getKey();
            fila.setOnClickListener(v -> {
                Intent editar = new Intent(requireContext(), TomaActivity.class);
                editar.putExtra(TomaActivity.EXTRA_ID, id);
                startActivity(editar);
            });

            lista.addView(fila);
            ultimaFila = fila;
        }


        if (ultimaFila != null) {
            ultimaFila.findViewById(R.id.separador).setVisibility(View.GONE);
        }
        tvSinTomas.setVisibility(ultimaFila == null ? View.VISIBLE : View.GONE);
    }
}