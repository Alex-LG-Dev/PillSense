package com.equipo5.pillsense;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/** Pestaña "Pastillas": lista de medicamentos del usuario, leída de Realtime Database. */
public class PastillasFragment extends Fragment {

    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    private DatabaseReference refMedicamentos;
    private ValueEventListener escuchador;

    private LinearLayout lista;
    private TextView tvNum;

    public PastillasFragment() {
        super(R.layout.fragment_pastillas);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvNum = view.findViewById(R.id.tvNumMedicamentos);
        lista = view.findViewById(R.id.listaMedicamentos);

        // Abre el formulario para añadir un medicamento nuevo
        Button btnAnadir = view.findViewById(R.id.btnAnadirMedicamento);
        btnAnadir.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MedicamentoActivity.class)));

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            tvNum.setText(R.string.med_sin_sesion);
            return;
        }
        refMedicamentos = FirebaseDatabase.getInstance(DB_URL)
                .getReference("medicamentos")
                .child(usuario.getUid());

        // Se ejecuta al abrir la pestaña y cada vez que cambia algo en Firebase
        // (al añadir, editar o borrar, la lista se actualiza sola)
        escuchador = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot datos) {
                mostrarLista(datos);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        };
        refMedicamentos.addValueEventListener(escuchador);
    }

    @Override
    public void onDestroyView() {
        // Dejamos de escuchar cuando la pestaña desaparece
        if (refMedicamentos != null && escuchador != null) {
            refMedicamentos.removeEventListener(escuchador);
        }
        super.onDestroyView();
    }

    /** Una tarjeta (item_medicamento.xml) por cada medicamento de Firebase. */
    private void mostrarLista(DataSnapshot datos) {
        if (getView() == null) return;

        lista.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        int total = 0;

        for (DataSnapshot hijo : datos.getChildren()) {
            Medicamento med = hijo.getValue(Medicamento.class);
            if (med == null) continue;

            View tarjeta = inflater.inflate(R.layout.item_medicamento, lista, false);
            ((TextView) tarjeta.findViewById(R.id.tvNombreMed)).setText(med.getNombre());
            ((TextView) tarjeta.findViewById(R.id.tvDetalleMed)).setText(med.getDosis());

            Bitmap foto = FotoUtils.deBase64(med.getImagenBase64());
            if (foto != null) {
                ImageView iv = tarjeta.findViewById(R.id.ivFotoItem);
                iv.setImageTintList(null);
                iv.setPadding(0, 0, 0, 0);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setImageBitmap(foto);
            }
            // Al tocar la tarjeta se abre el formulario para editar ese medicamento
            String id = hijo.getKey();
            tarjeta.setOnClickListener(v -> {
                Intent editar = new Intent(requireContext(), MedicamentoActivity.class);
                editar.putExtra(MedicamentoActivity.EXTRA_ID, id);
                startActivity(editar);
            });

            lista.addView(tarjeta);
            total++;
        }

        tvNum.setText(total == 0
                ? getString(R.string.med_lista_vacia)
                : getResources().getQuantityString(R.plurals.med_num, total, total));
    }
}
