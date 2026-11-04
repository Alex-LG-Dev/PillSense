package com.equipo5.pillsense;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Pantalla principal del paciente: versión sencilla para personas mayores. */
public class PacienteActivity extends AppCompatActivity {

    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    // Nombre de cada cajón: 1 Mañana, 2 Mediodía, 3 Tarde, 4 Noche
    private static final String[] FRANJAS = {"Mañana", "Mediodía", "Tarde", "Noche"};

    private TextView tvSaludo;

    // Tarjeta "Ahora te toca"
    private TextView tvAhoraHora, tvAhoraMedicamento, tvAhoraDosis;
    private TextView tvAhoraCajonNumero, tvAhoraCajon, tvAhoraFranja, tvTituloDespues;
    private View cajaCajon, btnTomada;
    private LinearLayout listaDespues;

    // Tomas de Firebase: tomas/{uid}
    private DatabaseReference refTomas;
    private ValueEventListener escuchador;
    private final List<Toma> tomasDeHoy = new ArrayList<>();

    // Tomas ya tomadas hoy: registros/{uid}/{fecha}/{idToma}
    private DatabaseReference refRegistrosHoy;
    private ValueEventListener escuchadorRegistros;
    private final Set<String> tomadasHoy = new HashSet<>();   // ids de las tomas ya tomadas
    private Toma tomaActual;
    private Toma siguienteToma;                               // la de después (para "¡Muy bien!")
    private String primerNombre = ""; // la que sale en la tarjeta grande

    // Cada minuto se vuelve a pintar la pantalla, para que "Ahora te toca" cambie solo con la hora
    private final Handler reloj = new Handler(Looper.getMainLooper());
    private final Runnable cadaMinuto = new Runnable() {
        @Override
        public void run() {
            mostrarTomas();
            reloj.postDelayed(this, 60_000);   // 60.000 ms = 1 minuto
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paciente);

        // Deja hueco arriba (hora, batería) y abajo (botones del móvil) para que nada quede tapado
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        Button btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());

        // Saludo con el nombre y fecha de hoy
        tvSaludo = findViewById(R.id.tvSaludo);
        TextView tvFecha = findViewById(R.id.tvFecha);
        tvFecha.setText(fechaDeHoy());
        cargarNombre();

        // Tarjeta "Ahora te toca" y lista "Después"
        tvAhoraHora = findViewById(R.id.tvAhoraHora);
        tvAhoraMedicamento = findViewById(R.id.tvAhoraMedicamento);
        tvAhoraDosis = findViewById(R.id.tvAhoraDosis);
        tvAhoraCajonNumero = findViewById(R.id.tvAhoraCajonNumero);
        tvAhoraCajon = findViewById(R.id.tvAhoraCajon);
        tvAhoraFranja = findViewById(R.id.tvAhoraFranja);
        tvTituloDespues = findViewById(R.id.tvTituloDespues);
        cajaCajon = findViewById(R.id.cajaCajon);
        btnTomada = findViewById(R.id.btnTomada);
        listaDespues = findViewById(R.id.listaDespues);

        // Mientras llegan los datos, quitamos los ejemplos del XML
        tvAhoraDosis.setVisibility(View.GONE);   // la toma no guarda la dosis (de momento)
        tvAhoraHora.setText("");
        tvAhoraMedicamento.setText("Cargando…");
        cajaCajon.setVisibility(View.GONE);
        btnTomada.setVisibility(View.GONE);
        tvTituloDespues.setVisibility(View.GONE);
        listaDespues.removeAllViews();

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario != null) {
            refTomas = FirebaseDatabase.getInstance(DB_URL)
                    .getReference("tomas").child(usuario.getUid());

            // Se ejecuta al abrir la pantalla y cada vez que cambia una toma en Firebase
            escuchador = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot datos) {
                    tomasDeHoy.clear();
                    for (DataSnapshot hijo : datos.getChildren()) {
                        Toma toma = hijo.getValue(Toma.class);
                        if (toma != null && toma.getHora() != null) {
                            toma.setId(hijo.getKey());
                            tomasDeHoy.add(toma);
                        }
                    }
                    // De la más temprana a la más tardía ("08:00" va antes que "14:00")
                    Collections.sort(tomasDeHoy, (a, b) -> a.getHora().compareTo(b.getHora()));
                    mostrarTomas();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(PacienteActivity.this,
                            "No se han podido cargar tus pastillas", Toast.LENGTH_LONG).show();
                }
            };

            // Se ejecuta cada vez que se marca (o se borra) una toma como tomada hoy
            escuchadorRegistros = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot datos) {
                    tomadasHoy.clear();
                    for (DataSnapshot hijo : datos.getChildren()) {
                        tomadasHoy.add(hijo.getKey());   // la clave es el id de la toma
                    }
                    mostrarTomas();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            };
        }

        // Botón verde: guarda que se ha tomado la pastilla de la tarjeta
        btnTomada.setOnClickListener(v -> marcarComoTomada());

        // "Ver todas mis pastillas" abre la lista completa del día
        Button btnVerTodas = findViewById(R.id.btnVerTodas);
         btnVerTodas.setOnClickListener(v ->
                startActivity(new Intent(this, PastillasHoyActivity.class)));

        // "Pedir ayuda" abre la pantalla para llamar a la cuidadora
        Button btnPedirAyuda = findViewById(R.id.btnPedirAyuda);
        btnPedirAyuda.setOnClickListener(v ->
                startActivity(new Intent(this, PedirAyudaActivity.class)));

        // Tarjeta azul: al pulsarla se abre "Aviso de toma"

        findViewById(R.id.tarjetaAhora).setOnClickListener(v -> {
            if (tomaActual == null) return;   // "¡Ya has terminado por hoy!": no hay nada que enseñar
            Intent intent = new Intent(this, AvisoTomaActivity.class);
            intent.putExtra(AvisoTomaActivity.EXTRA_ID_TOMA, tomaActual.getId());
            intent.putExtra(AvisoTomaActivity.EXTRA_HORA, tomaActual.getHora());
            intent.putExtra(AvisoTomaActivity.EXTRA_MEDICAMENTO, tomaActual.getNombreMedicamento());
            intent.putExtra(AvisoTomaActivity.EXTRA_CAJON, tomaActual.getCajon());
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Empieza a escuchar Firebase y el reloj al ver la pantalla
        if (refTomas != null) refTomas.addValueEventListener(escuchador);
        reloj.postDelayed(cadaMinuto, 60_000);

        // Registros de HOY (la fecha se calcula cada vez que se abre la pantalla)
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario != null) {
            String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            refRegistrosHoy = FirebaseDatabase.getInstance(DB_URL)
                    .getReference("registros").child(usuario.getUid()).child(hoy);
            refRegistrosHoy.addValueEventListener(escuchadorRegistros);
        }
    }

    @Override
    protected void onStop() {
        // Y para al salir, para no gastar batería
        if (refTomas != null) refTomas.removeEventListener(escuchador);
        if (refRegistrosHoy != null) refRegistrosHoy.removeEventListener(escuchadorRegistros);
        reloj.removeCallbacks(cadaMinuto);
        super.onStop();
    }


    private void mostrarTomas() {
        String ahora = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        // Pendientes: no han pasado su hora límite y todavía no se han tomado
        List<Toma> pendientes = new ArrayList<>();
        for (Toma toma : tomasDeHoy) {
            String limite = (toma.getHoraLimite() != null && !toma.getHoraLimite().isEmpty())
                    ? toma.getHoraLimite()
                    : toma.getHora();
            boolean yaTomada = tomadasHoy.contains(toma.getId());
            if (limite.compareTo(ahora) >= 0 && !yaTomada) {
                pendientes.add(toma);
            }
        }

        listaDespues.removeAllViews();   // borramos las filas de antes para no repetirlas

        // No queda ninguna
        if (pendientes.isEmpty()) {
            tomaActual = null;
            siguienteToma = null;
            tvAhoraHora.setText("HOY");
            tvAhoraMedicamento.setText("¡Ya has terminado por hoy!");
            cajaCajon.setVisibility(View.GONE);
            btnTomada.setVisibility(View.GONE);
            tvTituloDespues.setVisibility(View.GONE);
            return;
        }

        // La primera pendiente va en la tarjeta grande
        Toma primera = pendientes.get(0);
        tomaActual = primera;
        siguienteToma = pendientes.size() > 1 ? pendientes.get(1) : null;

        boolean yaEsLaHora = primera.getHora().compareTo(ahora) <= 0;
        tvAhoraHora.setText((yaEsLaHora ? "AHORA TE TOCA · " : "TU PRÓXIMA PASTILLA · ")
                + primera.getHora());
        tvAhoraMedicamento.setText(primera.getNombreMedicamento());
        tvAhoraCajonNumero.setText(String.valueOf(primera.getCajon()));
        tvAhoraCajon.setText("Cajón " + primera.getCajon());
        tvAhoraFranja.setText(franja(primera.getCajon()));
        cajaCajon.setVisibility(View.VISIBLE);

        // Solo se puede marcar como tomada cuando ya es la hora
        btnTomada.setVisibility(yaEsLaHora ? View.VISIBLE : View.GONE);

        // El resto de pendientes va en "Después" (la primera ya está en la tarjeta)
        tvTituloDespues.setVisibility(pendientes.size() > 1 ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 1; i < pendientes.size(); i++) {
            Toma toma = pendientes.get(i);
            View fila = inflater.inflate(R.layout.item_toma_paciente, listaDespues, false);
            ((TextView) fila.findViewById(R.id.tvHora)).setText(toma.getHora());
            ((TextView) fila.findViewById(R.id.tvMedicamento)).setText(toma.getNombreMedicamento());
            ((TextView) fila.findViewById(R.id.tvCajon)).setText("Cajón " + toma.getCajon());
            listaDespues.addView(fila);
        }
    }


    private void marcarComoTomada() {
        if (tomaActual == null || refRegistrosHoy == null) return;

        Map<String, Object> registro = new HashMap<>();
        registro.put("estado", "tomada");
        registro.put("horaReal", new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));

        refRegistrosHoy.child(tomaActual.getId()).setValue(registro)
                .addOnFailureListener(e -> Toast.makeText(this,
                        "No se ha podido guardar. Inténtalo otra vez", Toast.LENGTH_LONG).show());
        // No hace falta repintar aquí: escuchadorRegistros se entera y llama a mostrarTomas()

        // Abrimos la pantalla "¡Muy bien!" con los datos de esta toma y de la siguiente
        Intent intent = new Intent(this, TomaRegistradaActivity.class);
        intent.putExtra(TomaRegistradaActivity.EXTRA_NOMBRE, primerNombre);
        intent.putExtra(TomaRegistradaActivity.EXTRA_MEDICAMENTO, tomaActual.getNombreMedicamento());
        intent.putExtra(TomaRegistradaActivity.EXTRA_HORA, tomaActual.getHora());
        if (siguienteToma != null) {
            intent.putExtra(TomaRegistradaActivity.EXTRA_PROXIMA_HORA, siguienteToma.getHora());
            intent.putExtra(TomaRegistradaActivity.EXTRA_PROXIMA_MEDICAMENTO, siguienteToma.getNombreMedicamento());
            intent.putExtra(TomaRegistradaActivity.EXTRA_PROXIMO_CAJON, siguienteToma.getCajon());
        }
        startActivity(intent);
    }



    private String franja(int cajon) {
        return (cajon >= 1 && cajon <= 4) ? FRANJAS[cajon - 1] : "";
    }


    private String fechaDeHoy() {
        String fecha = new SimpleDateFormat("EEEE, d 'de' MMMM", new Locale("es", "ES"))
                .format(new Date());
        // Primera letra en mayúscula: "martes" → "Martes"
        return fecha.substring(0, 1).toUpperCase() + fecha.substring(1);
    }


    private void cargarNombre() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) return;

        // Mientras carga, el nombre de la cuenta (si lo tiene)
        ponerSaludo(usuario.getDisplayName());

        FirebaseDatabase.getInstance(DB_URL)
                .getReference("usuarios").child(usuario.getUid()).child("nombre")
                .get()
                .addOnSuccessListener(datos -> {
                    String nombre = datos.getValue(String.class);
                    if (nombre != null && !nombre.trim().isEmpty()) {
                        ponerSaludo(nombre);
                    }
                });
    }


    private void ponerSaludo(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            tvSaludo.setText("Hola");
            return;
        }
        primerNombre = nombre.trim().split(" ")[0];
        tvSaludo.setText("Hola, " + primerNombre);
    }


    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();
        SharedPreferences preferencias = getSharedPreferences("PillSensePrefs", Context.MODE_PRIVATE);
        preferencias.edit().putBoolean("mantener_sesion", false).apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}