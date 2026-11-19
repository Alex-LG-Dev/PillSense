package com.equipo5.pillsense;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

    private TomaAdapter adapter;
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

        // Lista de tomas: RecyclerView con su adapter
        RecyclerView rvTomas = view.findViewById(R.id.rvTomas);
        rvTomas.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TomaAdapter(new TomaAdapter.OnTomaListener() {
            @Override
            public void alTocarIcono(Toma toma, boolean marcarComoTomada) {
                marcar(toma.getId(), toma, marcarComoTomada);
            }

            @Override
            public void alTocarFila(Toma toma) {
                // Abre el formulario para editar esa toma
                Intent editar = new Intent(requireContext(), TomaActivity.class);
                editar.putExtra(TomaActivity.EXTRA_ID, toma.getId());
                startActivity(editar);
            }
        });
        rvTomas.setAdapter(adapter);
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

            // Lo que ya se ha tomado hoy, para pintar el estado de cada fila
            escuchadorRegistros = new ValueEventListener() {
                /*Estos registros los escribe la app al marcar una toma y, en el futuro,
                  el pastillero al abrir la tapa (ver T075)*/
                @Override
                public void onDataChange(@NonNull DataSnapshot datos) {
                    tomadasHoy.clear();
                    for (DataSnapshot hijo : datos.getChildren()) {
                        tomadasHoy.add(hijo.getKey());   // la clave es el id de la toma
                    }
                    if (ultimasTomas != null) mostrarTomas(ultimasTomas);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("HoyFragment", "Registros: " + error.getMessage());
                }
            };

        }

        // Botón flotante: abre el formulario para crear una toma nueva
        view.findViewById(R.id.fabNuevaToma).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), TomaActivity.class)));
    }

    @Override
    public void onStart() {
        super.onStart();
        if (refTomas == null) return;

        // Al entrar en la pantalla: empezamos a escuchar las tomas
        refTomas.orderByChild("hora").addValueEventListener(escuchador);

        // Y los registros de HOY (la fecha se calcula cada vez, por si ha cambiado el día)
        refRegistrosHoy = FirebaseDatabase.getInstance(DB_URL)
                .getReference("registros")
                .child(PacienteActivo.uidDatos(requireContext()))
                .child(hoy());
        refRegistrosHoy.addValueEventListener(escuchadorRegistros);

        Log.d("HoyFragment", "Escucha ACTIVADA");
    }

    @Override
    public void onStop() {
        // Al salir de la pantalla: dejamos de escuchar
        if (refTomas != null) {
            refTomas.orderByChild("hora").removeEventListener(escuchador);
        }
        if (refRegistrosHoy != null) {
            refRegistrosHoy.removeEventListener(escuchadorRegistros);
        }
        Log.d("HoyFragment", "Escucha PARADA");
        super.onStop();
    }


    private void mostrarTomas(DataSnapshot datos) {
        if (getView() == null) return;

        // Pasamos las tomas de Firebase a una lista para el adapter
        List<Toma> tomas = new ArrayList<>();
        for (DataSnapshot hijo : datos.getChildren()) {
            Toma toma = hijo.getValue(Toma.class);
            if (toma == null) continue;
            toma.setId(hijo.getKey());   // la clave del nodo es el id de la toma
            tomas.add(toma);
        }

        // El adapter pinta las filas con su estado y color
        adapter.actualizar(tomas, tomadasHoy);
        tvSinTomas.setVisibility(tomas.isEmpty() ? View.VISIBLE : View.GONE);
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
