package com.equipo5.pillsense;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * Pestaña "Historial" (T042).
 *
 * Mantiene la tabla del prototipo (un día por fila y una hora por columna) pero con
 * datos de verdad, y añade los filtros que pide la tarjeta: rango de fechas y estado.
 *
 * El filtro de fechas lo hace la base de datos. Los registros se guardan en
 * registros/{uid}/{fecha}/{idToma}, o sea que la fecha es la clave del nodo, así que se
 * piden con orderByKey().startAt(desde).endAt(hasta) y solo baja ese rango.
 *
 * El estado se resuelve al leer, porque en la base de datos solo quedan guardadas las
 * tomas marcadas como tomadas: si un día ya pasado no tiene registro, es una olvidada.
 */
public class HistorialFragment extends Fragment {

    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    private static final String ESTADO_TOMADA = "tomada";

    // Filtro de estado
    private static final int TODAS = 0, TOMADAS = 1, OLVIDADAS = 2;

    // Estado de cada celda. De menor a mayor importancia: si a una hora hay varias
    // tomas, en la celda manda la peor (una olvidada tapa a una tomada).
    private static final int VACIA = 0, TOMADA = 1, TARDE = 2, PENDIENTE = 3, OLVIDADA = 4;

    // Si el rango es muy largo la tabla no cabe a lo ancho, así que se limitan las columnas
    private static final int MAXIMO_COLUMNAS = 6;

    private final SimpleDateFormat formatoClave = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat formatoDiaSemana = new SimpleDateFormat("EEE", new Locale("es", "ES"));
    private final SimpleDateFormat formatoDiaMes = new SimpleDateFormat("d MMM", new Locale("es", "ES"));
    private final SimpleDateFormat formatoTituloDetalle =
            new SimpleDateFormat("EEEE d 'de' MMMM", new Locale("es", "ES"));

    private final Calendar desde = Calendar.getInstance();
    private final Calendar hasta = Calendar.getInstance();
    private int filtroEstado = TODAS;

    private final List<Toma> tomasProgramadas = new ArrayList<>();
    private DataSnapshot registros;

    private LinearLayout cabeceraHoras, tabla;
    private TextView tvSinResultados, tvPeriodo, tvPorcentaje, tvTomasSemana, tvOlvidadas;
    private MaterialButton btnDesde, btnHasta, btnTodas, btnTomadas, btnOlvidadas;

    public HistorialFragment() {
        super(R.layout.fragment_historial);
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(vista, savedInstanceState);

        cabeceraHoras = vista.findViewById(R.id.cabeceraHoras);
        tabla = vista.findViewById(R.id.tablaSemana);
        tvSinResultados = vista.findViewById(R.id.tvSinResultados);
        tvPeriodo = vista.findViewById(R.id.tvPeriodo);
        tvPorcentaje = vista.findViewById(R.id.tvPorcentaje);
        tvTomasSemana = vista.findViewById(R.id.tvTomasSemana);
        tvOlvidadas = vista.findViewById(R.id.tvOlvidadas);
        btnDesde = vista.findViewById(R.id.btnDesde);
        btnHasta = vista.findViewById(R.id.btnHasta);
        btnTodas = vista.findViewById(R.id.btnTodas);
        btnTomadas = vista.findViewById(R.id.btnTomadas);
        btnOlvidadas = vista.findViewById(R.id.btnOlvidadas);

        // Por defecto, la última semana contando hoy (como en el prototipo)
        long ahora = System.currentTimeMillis();
        hasta.setTimeInMillis(ahora);
        desde.setTimeInMillis(ahora);
        desde.add(Calendar.DAY_OF_MONTH, -6);
        aMedianoche(desde);
        aMedianoche(hasta);

        btnDesde.setOnClickListener(v -> elegirFecha(desde, true));
        btnHasta.setOnClickListener(v -> elegirFecha(hasta, false));
        btnTodas.setOnClickListener(v -> cambiarFiltro(TODAS));
        btnTomadas.setOnClickListener(v -> cambiarFiltro(TOMADAS));
        btnOlvidadas.setOnClickListener(v -> cambiarFiltro(OLVIDADAS));

        pintarBotones();
        cargar();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Al volver a la pestaña puede haber tomas o registros nuevos
        cargar();
    }

    // ------------------------------------------------------------- filtros

    private void elegirFecha(Calendar cual, boolean esDesde) {
        new DatePickerDialog(requireContext(), (picker, anio, mes, dia) -> {
            cual.set(anio, mes, dia);
            aMedianoche(cual);
            // Que el rango no quede del revés
            if (esDesde && desde.after(hasta)) hasta.setTime(desde.getTime());
            if (!esDesde && hasta.before(desde)) desde.setTime(hasta.getTime());
            pintarBotones();
            cargar();
        }, cual.get(Calendar.YEAR), cual.get(Calendar.MONTH), cual.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void cambiarFiltro(int nuevo) {
        filtroEstado = nuevo;
        pintarBotones();
        mostrar();   // cambiar de estado no necesita volver a consultar
    }

    private void pintarBotones() {
        btnDesde.setText("Desde: " + formatoDiaMes.format(desde.getTime()));
        btnHasta.setText("Hasta: " + formatoDiaMes.format(hasta.getTime()));
        resaltar(btnTodas, filtroEstado == TODAS);
        resaltar(btnTomadas, filtroEstado == TOMADAS);
        resaltar(btnOlvidadas, filtroEstado == OLVIDADAS);
    }

    /** El botón elegido va en verde; los otros, apagados. */
    private void resaltar(MaterialButton boton, boolean elegido) {
        boton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),
                elegido ? R.color.verde_menta : R.color.fondo_tarjeta)));
        boton.setTextColor(ContextCompat.getColor(requireContext(),
                elegido ? R.color.azul_noche : R.color.texto_secundario));
    }

    // ------------------------------------------------------------- datos

    /** Pide los horarios y, con ellos, los registros del rango elegido. */
    private void cargar() {
        // El mismo usuario que mira el resto de pestañas: el paciente elegido si eres
        // cuidador, y si no el tuyo propio
        String uidDatos = PacienteActivo.uidDatos(requireContext());
        if (uidDatos == null) return;

        FirebaseDatabase bd = FirebaseDatabase.getInstance(DB_URL);
        DatabaseReference refTomas = bd.getReference("tomas").child(uidDatos);
        DatabaseReference refRegistros = bd.getReference("registros").child(uidDatos);

        refTomas.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot datos) {
                if (!isAdded()) return;
                tomasProgramadas.clear();
                for (DataSnapshot hijo : datos.getChildren()) {
                    Toma toma = hijo.getValue(Toma.class);
                    if (toma != null) {
                        if (toma.getId() == null) toma.setId(hijo.getKey());
                        tomasProgramadas.add(toma);
                    }
                }

                // Aquí está el filtro por fechas: la clave de cada hijo es la fecha
                refRegistros.orderByKey()
                        .startAt(formatoClave.format(desde.getTime()))
                        .endAt(formatoClave.format(hasta.getTime()))
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot datosRegistros) {
                                if (!isAdded()) return;
                                registros = datosRegistros;
                                mostrar();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) { }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // ------------------------------------------------------------- pintar

    private void mostrar() {
        if (!isAdded() || registros == null) return;

        // Las columnas son las horas distintas de los medicamentos del usuario
        TreeSet<String> horasOrdenadas = new TreeSet<>();
        for (Toma toma : tomasProgramadas) {
            if (toma.getHora() != null) horasOrdenadas.add(toma.getHora());
        }
        List<String> horas = new ArrayList<>(horasOrdenadas);
        if (horas.size() > MAXIMO_COLUMNAS) horas = horas.subList(0, MAXIMO_COLUMNAS);

        float densidad = getResources().getDisplayMetrics().density;
        int anchoDia = Math.round(56 * densidad);

        // Cabecera con las horas
        cabeceraHoras.removeAllViews();
        cabeceraHoras.addView(new View(requireContext()),
                new LinearLayout.LayoutParams(anchoDia, 1));
        for (String hora : horas) {
            TextView tvHora = new TextView(requireContext());
            tvHora.setText(hora);
            tvHora.setTextSize(13);
            tvHora.setGravity(Gravity.CENTER);
            tvHora.setTextColor(ContextCompat.getColor(requireContext(), R.color.texto_secundario));
            cabeceraHoras.addView(tvHora, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        }

        // Una fila por día del rango
        tabla.removeAllViews();
        Calendar dia = (Calendar) desde.clone();
        String claveHoy = formatoClave.format(new Date());
        String claveHasta = formatoClave.format(hasta.getTime());
        boolean rangoCorto = diasDelRango() <= 7;

        int tomadas = 0, olvidadas = 0, total = 0, celdasPintadas = 0;

        // Comparamos por la fecha en texto (aaaa-mm-dd), no por milisegundos:
        // así el último día del rango nunca se queda fuera
        while (formatoClave.format(dia.getTime()).compareTo(claveHasta) <= 0) {
            String clave = formatoClave.format(dia.getTime());
            boolean esHoy = clave.equals(claveHoy);

            // El resumen se cuenta toma a toma, no celda a celda: si dos pastillas
            // coinciden a la misma hora, las dos cuentan aunque compartan casilla
            for (Toma toma : tomasProgramadas) {
                int estado = estadoDeToma(toma, clave, esHoy);
                if (estado == TOMADA || estado == TARDE) { tomadas++; total++; }
                else if (estado == OLVIDADA) { olvidadas++; total++; }
            }

            LinearLayout fila = new LinearLayout(requireContext());
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(Gravity.CENTER_VERTICAL);
            fila.setPadding(0, Math.round(8 * densidad), 0, Math.round(8 * densidad));

            TextView tvDia = new TextView(requireContext());
            tvDia.setText(rangoCorto
                    ? formatoDiaSemana.format(dia.getTime())
                    : formatoDiaMes.format(dia.getTime()));
            tvDia.setTextSize(15);
            tvDia.setTextColor(ContextCompat.getColor(requireContext(),
                    esHoy ? R.color.texto_principal : R.color.texto_secundario));
            if (esHoy) tvDia.setTypeface(tvDia.getTypeface(), Typeface.BOLD);
            fila.addView(tvDia, new LinearLayout.LayoutParams(anchoDia,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            for (String hora : horas) {
                int estado = estadoDe(clave, hora, esHoy);

                FrameLayout celda = new FrameLayout(requireContext());
                if (seVeConElFiltro(estado)) {
                    ImageView icono = new ImageView(requireContext());
                    ponerIcono(icono, estado);
                    int tam = Math.round(28 * densidad);
                    celda.addView(icono, new FrameLayout.LayoutParams(tam, tam, Gravity.CENTER));
                    if (estado != VACIA) celdasPintadas++;
                }

                // Tocando la casilla se ve qué pastillas tocaban a esa hora y cómo fue cada
                // una. Hace falta porque si dos pastillas coinciden a la misma hora comparten
                // casilla y el icono solo puede enseñar una de las dos.
                if (estado != VACIA) {
                    final Date fechaCelda = dia.getTime();
                    final String claveCelda = clave;
                    final String horaCelda = hora;
                    final boolean hoyCelda = esHoy;
                    celda.setOnClickListener(v ->
                            mostrarDetalle(fechaCelda, claveCelda, horaCelda, hoyCelda));
                }
                fila.addView(celda, new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            }

            tabla.addView(fila);
            dia.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Resumen: se calcula con todo el rango, no con lo que deja ver el filtro
        int porcentaje = total == 0 ? 0 : Math.round(tomadas * 100f / total);
        tvPeriodo.setText(textoPeriodo());
        tvPorcentaje.setText(porcentaje + "%");
        tvTomasSemana.setText(tomadas + " de " + total + " tomas");
        tvOlvidadas.setText(olvidadas + (olvidadas == 1 ? " olvidada" : " olvidadas"));

        boolean vacio = horas.isEmpty() || celdasPintadas == 0;
        tvSinResultados.setVisibility(vacio ? View.VISIBLE : View.GONE);
    }

    /**
     * Diálogo con el detalle de una casilla: las pastillas que tocaban ese día a esa hora,
     * cómo acabó cada una y a qué hora se tomó.
     */
    private void mostrarDetalle(Date fecha, String clave, String hora, boolean esHoy) {
        StringBuilder texto = new StringBuilder();
        for (Toma toma : tomasProgramadas) {
            if (!hora.equals(toma.getHora())) continue;

            int estado = estadoDeToma(toma, clave, esHoy);
            if (estado == VACIA) continue;

            if (texto.length() > 0) texto.append("\n\n");
            texto.append(toma.getNombreMedicamento() == null
                    ? "Sin nombre" : toma.getNombreMedicamento());
            texto.append("\n").append(nombreDelEstado(estado));

            String horaReal = registros.child(clave)
                    .child(toma.getId() == null ? "" : toma.getId())
                    .child("horaReal").getValue(String.class);
            if (horaReal != null) texto.append(" a las ").append(horaReal);
            if (toma.getCajon() > 0) texto.append(" · cajón ").append(toma.getCajon());
        }
        if (texto.length() == 0) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(formatoTituloDetalle.format(fecha) + " · " + hora)
                .setMessage(texto.toString())
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private String nombreDelEstado(int estado) {
        switch (estado) {
            case TOMADA:    return "Tomada";
            case TARDE:     return "Tomada tarde";
            case PENDIENTE: return "Pendiente";
            case OLVIDADA:  return "Olvidada";
            default:        return "";
        }
    }

    /** Estado de la celda de un día y una hora. Si hay varias tomas a esa hora, manda la peor. */
    private int estadoDe(String clave, String hora, boolean esHoy) {
        int peor = VACIA;
        for (Toma toma : tomasProgramadas) {
            if (!hora.equals(toma.getHora())) continue;
            int estado = estadoDeToma(toma, clave, esHoy);
            if (estado > peor) peor = estado;
        }
        return peor;
    }

    /**
     * Estado de una toma concreta en un día concreto.
     * Devuelve VACIA si ese día la toma todavía no existía.
     */
    private int estadoDeToma(Toma toma, String clave, boolean esHoy) {
        DataSnapshot registro = registros.child(clave)
                .child(toma.getId() == null ? "" : toma.getId());
        boolean tomada = registro.exists()
                && ESTADO_TOMADA.equals(registro.child("estado").getValue(String.class));

        // Si hay registro de ese día, se enseña siempre. Si no lo hay, una toma
        // no puede salir olvidada en días en los que todavía no existía.
        if (!registro.exists()) {
            String creada = fechaDeCreacion(toma.getId());
            if (creada != null && clave.compareTo(creada) < 0) return VACIA;
        }

        if (tomada) {
            String horaReal = registro.child("horaReal").getValue(String.class);
            return seTomoTarde(toma, horaReal) ? TARDE : TOMADA;
        }
        if (esHoy && !pasoLaHoraLimite(toma)) return PENDIENTE;
        return OLVIDADA;
    }

    /**
     * El día en que se creó la toma, sacado de su identificador.
     *
     * Las claves que genera Firebase con push() empiezan por 8 caracteres que llevan
     * codificada la hora exacta en que se crearon, así que se puede saber desde cuándo
     * existe una toma sin guardar ningún campo más. Devuelve null si el id no es de ese
     * tipo (por ejemplo si alguien lo creó a mano desde la consola).
     */
    private String fechaDeCreacion(String id) {
        if (id == null || id.length() < 8 || !id.startsWith("-")) return null;
        final String ALFABETO =
                "-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz";
        long milis = 0;
        for (int i = 0; i < 8; i++) {
            int valor = ALFABETO.indexOf(id.charAt(i));
            if (valor < 0) return null;
            milis = milis * 64 + valor;
        }
        // Una fecha razonable: si sale algo absurdo, mejor no filtrar nada
        if (milis < 1_000_000_000_000L || milis > System.currentTimeMillis() + 86_400_000L) {
            return null;
        }
        return formatoClave.format(new Date(milis));
    }

    private boolean seTomoTarde(Toma toma, String horaReal) {
        String limite = toma.getHoraLimite();
        return horaReal != null && limite != null && horaReal.compareTo(limite) > 0;
    }

    private boolean pasoLaHoraLimite(Toma toma) {
        String limite = toma.getHoraLimite() != null ? toma.getHoraLimite() : toma.getHora();
        if (limite == null) return false;
        String ahora = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        return ahora.compareTo(limite) >= 0;
    }

    private boolean seVeConElFiltro(int estado) {
        if (estado == VACIA) return false;
        if (filtroEstado == TOMADAS) return estado == TOMADA || estado == TARDE;
        if (filtroEstado == OLVIDADAS) return estado == OLVIDADA;
        return true;
    }

    /** Mismos iconos y colores que la leyenda del prototipo. */
    private void ponerIcono(ImageView icono, int estado) {
        int verde = ContextCompat.getColor(requireContext(), R.color.verde_menta);
        switch (estado) {
            case TOMADA:
                icono.setImageResource(R.drawable.ic_toma_hecha);
                icono.setColorFilter(verde);
                break;
            case TARDE:
                icono.setImageResource(R.drawable.ic_toma_pendiente);
                icono.setColorFilter(verde);
                break;
            case OLVIDADA:
                icono.setImageResource(R.drawable.ic_toma_olvidada);
                icono.setColorFilter(ContextCompat.getColor(requireContext(), R.color.toma_olvidada));
                break;
            default:
                icono.setImageResource(R.drawable.ic_toma_pendiente);
                icono.setColorFilter(ContextCompat.getColor(requireContext(), R.color.pestana_inactiva));
                break;
        }
    }

    /**
     * Deja la fecha a las 00:00. Así "desde" y "hasta" se pueden restar y comparar
     * sin que la hora del día cuele o deje fuera el último día del rango.
     */
    private static void aMedianoche(Calendar cuando) {
        cuando.set(Calendar.HOUR_OF_DAY, 0);
        cuando.set(Calendar.MINUTE, 0);
        cuando.set(Calendar.SECOND, 0);
        cuando.set(Calendar.MILLISECOND, 0);
    }

    private int diasDelRango() {
        long milis = hasta.getTimeInMillis() - desde.getTimeInMillis();
        return (int) Math.round(milis / (24d * 60 * 60 * 1000)) + 1;
    }

    /** "Esta semana · 1 al 7 de noviembre" cuando el rango son los últimos 7 días. */
    private String textoPeriodo() {
        SimpleDateFormat soloDia = new SimpleDateFormat("d", new Locale("es", "ES"));
        SimpleDateFormat diaYMes = new SimpleDateFormat("d 'de' MMMM", new Locale("es", "ES"));
        // Si el rango cruza de mes, hay que decir el mes también en la primera fecha
        boolean mismoMes = desde.get(Calendar.MONTH) == hasta.get(Calendar.MONTH)
                && desde.get(Calendar.YEAR) == hasta.get(Calendar.YEAR);
        String rango = (mismoMes ? soloDia.format(desde.getTime()) : diaYMes.format(desde.getTime()))
                + " al " + diaYMes.format(hasta.getTime());

        Calendar haceUnaSemana = Calendar.getInstance();
        haceUnaSemana.add(Calendar.DAY_OF_MONTH, -6);
        boolean esLaSemana = formatoClave.format(desde.getTime())
                .equals(formatoClave.format(haceUnaSemana.getTime()))
                && formatoClave.format(hasta.getTime()).equals(formatoClave.format(new Date()));

        return (esLaSemana ? "Esta semana · " : "Del ") + rango;
    }
}
