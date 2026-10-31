package com.equipo5.pillsense;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class TomaActivity extends AppCompatActivity {

    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    private DatabaseReference refTomas;
    private DatabaseReference refMedicamentos;

    // Medicamentos del usuario (para elegir uno) y el que ha elegido
    private final List<Medicamento> medicamentos = new ArrayList<>();
    private Medicamento medicamentoElegido;

    private String hora = "08:00";
    private String horaLimite = "09:00";

    private TextView tvMedicamento;
    private TextView tvHora;
    private TextView tvHoraLimite;
    private RadioGroup rgCajon;
    private Button btnGuardar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BarraAcciones.prepararVentana(this);
        setContentView(R.layout.activity_toma);
        BarraAcciones.configurar(this, R.string.toma_titulo_nueva, true, 0);

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Toast.makeText(this, R.string.med_sin_sesion, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        refTomas = db.getReference("tomas").child(usuario.getUid());
        refMedicamentos = db.getReference("medicamentos").child(usuario.getUid());

        tvMedicamento = findViewById(R.id.tvMedicamentoToma);
        tvHora = findViewById(R.id.tvHoraToma);
        tvHoraLimite = findViewById(R.id.tvHoraLimiteToma);
        rgCajon = findViewById(R.id.rgCajonToma);
        btnGuardar = findViewById(R.id.btnGuardarToma);

        tvHora.setText(hora);
        tvHoraLimite.setText(horaLimite);

        cargarMedicamentos();

        tvMedicamento.setOnClickListener(v -> elegirMedicamento());
        tvHora.setOnClickListener(v -> elegirHora(true));
        tvHoraLimite.setOnClickListener(v -> elegirHora(false));
        btnGuardar.setOnClickListener(v -> guardar());
    }

    /** Lee una vez los medicamentos del usuario para poder elegir uno */
    private void cargarMedicamentos() {
        refMedicamentos.get().addOnSuccessListener(datos -> {
            medicamentos.clear();
            for (DataSnapshot hijo : datos.getChildren()) {
                Medicamento med = hijo.getValue(Medicamento.class);
                if (med != null) {
                    med.setId(hijo.getKey());
                    medicamentos.add(med);
                }
            }
        });
    }

    /** Enseña la lista de medicamentos */
    private void elegirMedicamento() {
        if (medicamentos.isEmpty()) {
            Toast.makeText(this, R.string.toma_sin_medicamentos, Toast.LENGTH_LONG).show();
            return;
        }
        String[] nombres = new String[medicamentos.size()];
        for (int i = 0; i < medicamentos.size(); i++) {
            Medicamento med = medicamentos.get(i);
            nombres[i] = med.getNombre() + " · " + med.getDosis();
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.toma_elige_medicamento)
                .setItems(nombres, (dialogo, posicion) -> ponerMedicamento(medicamentos.get(posicion)))
                .show();
    }

    /** Pone el medicamento elegido y marca su cajón. */
    private void ponerMedicamento(Medicamento med) {
        medicamentoElegido = med;
        tvMedicamento.setText(med.getNombre());
        marcarCajon(med.getCajon());
    }


    private void elegirHora(boolean esHora) {
        String actual = esHora ? hora : horaLimite;
        MaterialTimePicker reloj = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(Integer.parseInt(actual.substring(0, 2)))
                .setMinute(Integer.parseInt(actual.substring(3, 5)))
                .setTitleText(esHora ? R.string.toma_hora : R.string.toma_hora_limite)
                .build();

        reloj.addOnPositiveButtonClickListener(v -> {
            int h = reloj.getHour();
            int m = reloj.getMinute();
            if (esHora) {
                hora = formatear(h, m);
                // La hora límite se pone sola 1 hora después (sin pasar de las 23:59)
                horaLimite = (h >= 23) ? "23:59" : formatear(h + 1, m);
                tvHora.setText(hora);
                tvHoraLimite.setText(horaLimite);
            } else {
                horaLimite = formatear(h, m);
                tvHoraLimite.setText(horaLimite);
            }
        });
        reloj.show(getSupportFragmentManager(), "reloj");
    }

    private void guardar() {
        if (medicamentoElegido == null) {
            Toast.makeText(this, R.string.toma_error_medicamento, Toast.LENGTH_SHORT).show();
            return;
        }
        // Al ser "HH:mm", comparar el texto es comparar las horas
        if (horaLimite.compareTo(hora) <= 0) {
            Toast.makeText(this, R.string.toma_error_limite, Toast.LENGTH_LONG).show();
            return;
        }

        String id = refTomas.push().getKey();
        Toma toma = new Toma(id, medicamentoElegido.getId(), medicamentoElegido.getNombre(),
                cajonElegido(), hora, horaLimite);

        btnGuardar.setEnabled(false); // evita guardarla dos veces
        refTomas.child(id).setValue(toma)
                .addOnSuccessListener(r -> {
                    Toast.makeText(this, R.string.toma_guardada, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnGuardar.setEnabled(true);
                    Toast.makeText(this, getString(R.string.toma_error_guardar, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }


    private String formatear(int h, int m) {
        return String.format(Locale.ROOT, "%02d:%02d", h, m);
    }

    private int cajonElegido() {
        int marcado = rgCajon.getCheckedRadioButtonId();
        if (marcado == R.id.rbCajonToma2) return 2;
        if (marcado == R.id.rbCajonToma3) return 3;
        if (marcado == R.id.rbCajonToma4) return 4;
        return 1;
    }

    private void marcarCajon(int cajon) {
        if (cajon == 2) rgCajon.check(R.id.rbCajonToma2);
        else if (cajon == 3) rgCajon.check(R.id.rbCajonToma3);
        else if (cajon == 4) rgCajon.check(R.id.rbCajonToma4);
        else rgCajon.check(R.id.rbCajonToma1);
    }
}