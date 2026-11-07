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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Pestaña "Hoy": lista de tomas del usuario leída de Realtime Database, ordenada por hora. */
public class HoyFragment extends Fragment {

    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    private DatabaseReference refTomas;
    private ValueEventListener escuchador;

    // Tomas ya marcadas hoy: registros/{uid}/{fecha}/{idToma}
    private DatabaseReference refRegistrosHoy;
    private ValueEventListener escuchadorRegistros;
    private final Set<String> tomadasHoy = new HashSet<>();
    private DataSnapshot ultimasTomas;

    private LinearLayout lista;
    private TextView tvSinTomas;

    public HoyFragment() {
        super(R.layout.fragment_hoy);
    }

    private static String hoy() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lista = view.findViewById(R.id.listaTomas);
        tvSinTomas = view.findViewById(R.id.tvSinTomas);

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario != null) {
            String uidDatos = PacienteActivo.uidDatos(requireContext());

            refTomas = FirebaseDatabase.getInstance(DB_URL)
                    .getReference("tomas")
                    .child(uidDatos);

            // Se ejecuta al abrir la pestaña y cada vez que cambia una toma en Firebase
            escuchador = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot datos) {
                    ultimasTomas = datos;
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

            // Lo que ya se ha tomado hoy, para pintar el estado de cada fila
            refRegistrosHoy = FirebaseDatabase.getInstance(DB_URL)
                    .getReference("registros").child(uidDatos).child(hoy());
            escuchadorRegistros = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot datos) {
                    tomadasHoy.clear();
                    for (DataSnapshot hijo : datos.getChildren()) {
                        tomadasHoy.add(hijo.getKey());   // la clave es el id de la toma
                    }
                    if (ultimasTomas != null) mostrarTomas(ultimasTomas);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            };
            refRegistrosHoy.addValueEventListener(escuchadorRegistros);
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
        if (refRegistrosHoy != null && escuchadorRegistros != null) {
            refRegistrosHoy.removeEventListener(escuchadorRegistros);
        }
        super.onDestroyView();
    }


    private void mostrarTomas(DataSnapshot datos) {
        if (getView() == null) return;

        lista.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        int gris = ContextCompat.getColor(requireContext(), R.color.pestana_inactiva);
        int verde = ContextCompat.getColor(requireContext(), R.color.verde_menta);
        int azul = ContextCompat.getColor(requireContext(), R.color.toma_olvidada);
        String ahora = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        View ultimaFila = null;

        for (DataSnapshot hijo : datos.getChildren()) {
            Toma toma = hijo.getValue(Toma.class);
            if (toma == null) continue;

            View fila = inflater.inflate(R.layout.item_toma, lista, false);
            ((TextView) fila.findViewById(R.id.tvHora)).setText(toma.getHora());
            ((TextView) fila.findViewById(R.id.tvNombre)).setText(toma.getNombreMedicamento());
            ((TextView) fila.findViewById(R.id.tvEstado)).setText(
                    getString(R.string.toma_detalle, toma.getCajon(), toma.getHoraLimite()));

            String id = hijo.getKey();
            boolean tomada = id != null && tomadasHoy.contains(id);
            boolean olvidada = !tomada && toma.getHoraLimite() != null
                    && ahora.compareTo(toma.getHoraLimite()) > 0;

            ImageView ivEstado = fila.findViewById(R.id.ivEstado);
            if (tomada) {
                ivEstado.setImageResource(R.drawable.ic_toma_hecha);
                ivEstado.setColorFilter(verde);
            } else if (olvidada) {
                ivEstado.setImageResource(R.drawable.ic_toma_olvidada);
                ivEstado.setColorFilter(azul);
            } else {
                ivEstado.setImageResource(R.drawable.ic_toma_pendiente);
                ivEstado.setColorFilter(gris);
            }

            // Tocando el icono se marca la toma como tomada, o se desmarca si fue sin querer
            ivEstado.setOnClickListener(v -> marcar(id, toma, !tomada));

            // Al tocar la fila se abre el formulario para editar esa toma
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

    /**
     * Guarda (o borra) el registro de que esta toma se ha tomado hoy.
     * Escribe en el mismo sitio y con los mismos campos que la pantalla de aviso.
     */
    private void marcar(String idToma, Toma toma, boolean tomada) {
        if (idToma == null) return;
        String uidDatos = PacienteActivo.uidDatos(requireContext());
        if (uidDatos == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL)
                .getReference("registros").child(uidDatos).child(hoy()).child(idToma);

        if (!tomada) {
            ref.removeValue();
            return;
        }

        Map<String, Object> registro = new HashMap<>();
        registro.put("estado", "tomada");
        registro.put("horaReal", new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));

        ref.setValue(registro)
                .addOnSuccessListener(r -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                toma.getNombreMedicamento() + " marcada como tomada",
                                Toast.LENGTH_SHORT).show();
                    }
                    // Ya está tomada: sobra el aviso que estuviera pendiente
                    AlarmasToma.cancelarPospuesta(requireContext(), idToma);
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                "No se ha podido guardar. Inténtalo otra vez",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
