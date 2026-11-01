package com.equipo5.pillsense;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioButton;
import android.view.View;

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

/** Pantalla para crear una toma. Se guarda en tomas/{uid}/{id} de Realtime Database. */
public class TomaActivity extends AppCompatActivity {


    public static final String EXTRA_ID = "idToma";

    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    // Franja del día de cada cajón (posición 0 = cajón 1 ... posición 3 = cajón 4)
    // Cajón 1 mañana · 2 mediodía · 3 tarde · 4 noche
    private static final String[] INICIO_FRANJA = {"06:00", "12:00", "16:00", "20:00"};
    private static final String[] FIN_FRANJA    = {"11:59", "15:59", "19:59", "23:59"};

    private DatabaseReference refTomas;
    private DatabaseReference refMedicamentos;

    // Medicamentos del usuario (para elegir uno) y el que ha elegido
    private final List<Medicamento> medicamentos = new ArrayList<>();
    private Medicamento medicamentoElegido;

    // Horas en texto "HH:mm" (así se ordenan bien al compararlas)
    private String hora = "";
    private String horaLimite = "";

    // Id de la toma que se edita
    private String idEditando;

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

        // Si nos pasan un id, estamos editando; si no, es una toma nueva
        idEditando = getIntent().getStringExtra(EXTRA_ID);
        BarraAcciones.configurar(this,
                idEditando == null ? R.string.toma_titulo_nueva : R.string.toma_titulo_editar,
                true, 0);

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
        ponerTextosCajones();

        // Al cambiar de cajón, las horas se ajustan a su franja
        rgCajon.setOnCheckedChangeListener((grupo, idMarcado) -> ajustarAlCajon());
        ajustarAlCajon(); // horas iniciales del cajón 1

        cargarMedicamentos();

        tvMedicamento.setOnClickListener(v -> elegirMedicamento());
        tvHora.setOnClickListener(v -> elegirHora(true));
        tvHoraLimite.setOnClickListener(v -> elegirHora(false));
        btnGuardar.setOnClickListener(v -> guardar());
        if (idEditando != null) {
            cargarToma();

            // El botón de borrar solo tiene sentido si la toma ya existe
            Button btnBorrar = findViewById(R.id.btnBorrarToma);
            btnBorrar.setVisibility(View.VISIBLE);
            btnBorrar.setOnClickListener(v -> confirmarBorrado());
        }
    }

    /** Lee una vez los medicamentos del usuario para poder elegir uno. */
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

    /** Enseña la lista de medicamentos en un diálogo. */
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

    /** Pone el medicamento elegido (el cajón lo elige el usuario en la toma). */
    private void ponerMedicamento(Medicamento med) {
        medicamentoElegido = med;
        tvMedicamento.setText(med.getNombre());
    }

    private void cargarToma() {
        refTomas.child(idEditando).get()
                .addOnSuccessListener(datos -> {
                    Toma toma = datos.getValue(Toma.class);
                    if (toma == null) {
                        Toast.makeText(this, R.string.toma_error_cargar, Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    medicamentoElegido = new Medicamento(toma.getIdMedicamento(),
                            toma.getNombreMedicamento(), "", "");
                    tvMedicamento.setText(toma.getNombreMedicamento());

                    // Primero el cajón (esto pone las horas de su franja)...
                    marcarCajon(toma.getCajon());
                    // ...y después las horas guardadas, para que no se pisen
                    hora = toma.getHora();
                    horaLimite = toma.getHoraLimite();
                    tvHora.setText(hora);
                    tvHoraLimite.setText(horaLimite);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, R.string.toma_error_cargar, Toast.LENGTH_SHORT).show();
                    finish();
                });
    }


    /**
     * Enseña la franja del cajón marcado y, si las horas no caben en ella,
     * las cambia por el principio de la franja (y la hora límite, 1 hora después).
     */
    /** Si las horas no caben en la franja del cajón marcado, las cambia por el principio de la franja. */
    private void ajustarAlCajon() {
        int cajon = cajonElegido();
        if (!dentroDeFranja(hora, cajon) || !dentroDeFranja(horaLimite, cajon)) {
            hora = INICIO_FRANJA[cajon - 1];
            horaLimite = FIN_FRANJA[cajon - 1];
        }
        tvHora.setText(hora);
        tvHoraLimite.setText(horaLimite);
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
            String elegida = formatear(reloj.getHour(), reloj.getMinute());
            int cajon = cajonElegido();

            // Bloqueo: la hora tiene que estar dentro de la franja del cajón
            if (!dentroDeFranja(elegida, cajon)) {
                avisarFueraDeFranja(cajon);
                return; // no se cambia nada
            }
            if (esHora) {
                hora = elegida;

            } else {
                horaLimite = elegida;
            }
            tvHora.setText(hora);
            tvHoraLimite.setText(horaLimite);
        });
        reloj.show(getSupportFragmentManager(), "reloj");
    }

    private void guardar() {
        if (medicamentoElegido == null) {
            Toast.makeText(this, R.string.toma_error_medicamento, Toast.LENGTH_SHORT).show();
            return;
        }
        int cajon = cajonElegido();
        if (!dentroDeFranja(hora, cajon) || !dentroDeFranja(horaLimite, cajon)) {
            avisarFueraDeFranja(cajon);
            return;
        }
        // Al ser "HH:mm", comparar el texto es comparar las horas
        if (horaLimite.compareTo(hora) <= 0) {
            Toast.makeText(this, R.string.toma_error_limite, Toast.LENGTH_LONG).show();
            return;
        }

        // Si editamos usamos el mismo id (se sobrescribe); si es nueva, push() crea uno
        String id = (idEditando != null) ? idEditando : refTomas.push().getKey();
        Toma toma = new Toma(id, medicamentoElegido.getId(), medicamentoElegido.getNombre(),
                cajon, hora, horaLimite);

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
    /** Pregunta antes de borrar, porque no se puede deshacer. */
    private void confirmarBorrado() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.toma_borrar_titulo)
                .setMessage(getString(R.string.toma_borrar_mensaje,
                        tvMedicamento.getText().toString(), hora))
                .setNegativeButton(R.string.med_cancelar, null)
                .setPositiveButton(R.string.med_borrar_si, (dialogo, boton) -> borrar())
                .show();
    }

    /** Borra la toma de Firebase y vuelve a Hoy. */
    private void borrar() {
        refTomas.child(idEditando).removeValue()
                .addOnSuccessListener(r -> {
                    Toast.makeText(this, R.string.toma_borrada, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.toma_error_borrar, e.getMessage()),
                                Toast.LENGTH_LONG).show());
    }

    // ===== Franjas del día =====

    /** ¿La hora está dentro de la franja del cajón? */
    private boolean dentroDeFranja(String h, int cajon) {
        if (h == null || h.length() != 5) return false;
        return h.compareTo(INICIO_FRANJA[cajon - 1]) >= 0
                && h.compareTo(FIN_FRANJA[cajon - 1]) <= 0;
    }


    private void avisarFueraDeFranja(int cajon) {
        String nombre = getResources().getStringArray(R.array.franjas_cajon)[cajon - 1];
        Toast.makeText(this, getString(R.string.toma_fuera_franja, cajon, nombre,
                INICIO_FRANJA[cajon - 1], FIN_FRANJA[cajon - 1]), Toast.LENGTH_LONG).show();
    }

    private String formatear(int h, int m) {
        return String.format(Locale.ROOT, "%02d:%02d", h, m);
    }

    // ===== Cajón =====

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

    private void ponerTextosCajones() {
        int[] ids = {R.id.rbCajonToma1, R.id.rbCajonToma2, R.id.rbCajonToma3, R.id.rbCajonToma4};
        String[] nombres = getResources().getStringArray(R.array.franjas_cajon);
        for (int i = 0; i < ids.length; i++) {
            RadioButton rb = findViewById(ids[i]);
            rb.setText(getString(R.string.toma_cajon_franja,
                    i + 1, nombres[i], INICIO_FRANJA[i], FIN_FRANJA[i]));
        }
    }
}