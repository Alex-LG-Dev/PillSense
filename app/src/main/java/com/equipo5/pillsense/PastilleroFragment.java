package com.equipo5.pillsense;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class PastilleroFragment extends Fragment {

    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    // Cada cajón es una franja del día
    private static final String[] MOMENTOS = {"Mañana", "Mediodía", "Tarde", "Noche"};

    private final CajonAdapter adaptador = new CajonAdapter();


    private DatabaseReference refTomas, refMedicamentos;
    private ValueEventListener escuchadorTomas, escuchadorMedicamentos;
    private DataSnapshot ultimasTomas, ultimosMedicamentos;

    public PastilleroFragment() {
        super(R.layout.fragment_pastillero);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // RecyclerView en 2 columnas (2 filas de 2 cajones)
        RecyclerView rvCajones = view.findViewById(R.id.rvCajones);
        rvCajones.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvCajones.setAdapter(adaptador);

        // Mientras llegan los datos de Firebase se ven los 4 cajones vacíos
        adaptador.setCajones(crearCajonesVacios());

        // De momento el botón solo avisa; cuando conectemos con el pastillero enviará la orden
        Button btnHacerSonar = view.findViewById(R.id.btnHacerSonar);
        btnHacerSonar.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Haciendo sonar el pastillero…", Toast.LENGTH_SHORT).show());

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) return;

        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        refTomas = db.getReference("tomas").child(usuario.getUid());
        refMedicamentos = db.getReference("medicamentos").child(usuario.getUid());

        // Cada vez que cambian las tomas o los medicamentos, se repintan los cajones
        escuchadorTomas = new EscuchadorFirebase() {
            @Override
            public void onDataChange(@NonNull DataSnapshot datos) {
                ultimasTomas = datos;
                actualizarCajones();
            }
        };
        escuchadorMedicamentos = new EscuchadorFirebase() {
            @Override
            public void onDataChange(@NonNull DataSnapshot datos) {
                ultimosMedicamentos = datos;
                actualizarCajones();
            }
        };
        refTomas.addValueEventListener(escuchadorTomas);
        refMedicamentos.addValueEventListener(escuchadorMedicamentos);
    }

    @Override
    public void onDestroyView() {
        // Dejamos de escuchar cuando la pestaña desaparece
        if (refTomas != null && escuchadorTomas != null) {
            refTomas.removeEventListener(escuchadorTomas);
        }
        if (refMedicamentos != null && escuchadorMedicamentos != null) {
            refMedicamentos.removeEventListener(escuchadorMedicamentos);
        }
        super.onDestroyView();
    }

    /** Solo repinta cuando ya han llegado las tomas Y los medicamentos. */
    private void actualizarCajones() {
        if (ultimasTomas == null || ultimosMedicamentos == null) return;
        adaptador.setCajones(repartirEnCajones(ultimasTomas, ultimosMedicamentos));
    }

    /** Los 4 cajones sin medicamentos. */
    private List<Cajon> crearCajonesVacios() {
        List<Cajon> cajones = new ArrayList<>();
        for (int i = 0; i < MOMENTOS.length; i++) {
            cajones.add(new Cajon(i + 1, MOMENTOS[i]));
        }
        return cajones;
    }

    /** Mete en cada cajón los medicamentos de las tomas de ese cajón (sin repetir). */
    private List<Cajon> repartirEnCajones(DataSnapshot tomas, DataSnapshot medicamentos) {
        // 1. Medicamentos por id, para encontrar rápido la foto de cada toma
        Map<String, Medicamento> medPorId = new HashMap<>();
        for (DataSnapshot hijo : medicamentos.getChildren()) {
            Medicamento med = hijo.getValue(Medicamento.class);
            if (med != null) medPorId.put(hijo.getKey(), med);
        }

        // Recorremos las tomas y ponemos su medicamento en el cajón de la toma
        List<Cajon> cajones = crearCajonesVacios();
        List<Set<String>> yaPuestos = new ArrayList<>();
        for (int i = 0; i < 4; i++) yaPuestos.add(new HashSet<>());

        for (DataSnapshot hijo : tomas.getChildren()) {
            Toma toma = hijo.getValue(Toma.class);
            if (toma == null) continue;

            int numero = toma.getCajon();
            if (numero < 1 || numero > 4) continue;

            String idMed = toma.getIdMedicamento();
            // Si el medicamento ya está en este cajón (otra toma), no lo repetimos
            if (idMed == null || !yaPuestos.get(numero - 1).add(idMed)) continue;

            Medicamento med = medPorId.get(idMed);
            if (med == null) {
                // El medicamento se borró pero la toma sigue: lo enseñamos sin foto
                med = new Medicamento(idMed, toma.getNombreMedicamento(), "", numero, null);
            }
            cajones.get(numero - 1).anadirMedicamento(med);
        }
        return cajones;
    }


    private abstract class EscuchadorFirebase implements ValueEventListener {
        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            if (isAdded()) {
                Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}