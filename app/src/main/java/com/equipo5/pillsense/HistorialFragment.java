package com.equipo5.pillsense;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Pestaña "Historial". Arriba, el gráfico semanal con los registros reales (T043);
 * abajo, la tabla con la semana de ejemplo del diseño.
 */
public class HistorialFragment extends Fragment {

    // De dónde salen los registros del gráfico. Cuando se acuerde Firestore, se cambia solo esta línea.
    private final FuenteRegistros fuente = new FuenteRegistrosVacia();

    // Estados de cada toma: T = tomada, R = tarde (retraso), O = olvidada, P = pendiente
    private final String[] dias = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
    private final String[] semana = {
            "TTTT",   // Lun
            "TTOT",   // Mar
            "TTRT",   // Mié
            "TTTT",   // Jue
            "TTTT",   // Vie
            "RTTO",   // Sáb
            "TTPP"    // Dom (hoy)
    };
    private static final int HOY = 6; // posición del día de hoy en la lista

    public HistorialFragment() {
        super(R.layout.fragment_historial);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout tabla = view.findViewById(R.id.tablaSemana);
        float densidad = getResources().getDisplayMetrics().density;

        // Una fila por día: nombre del día + 4 iconos (uno por hora)
        for (int d = 0; d < dias.length; d++) {
            LinearLayout fila = new LinearLayout(requireContext());
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(Gravity.CENTER_VERTICAL);
            fila.setPadding(0, Math.round(8 * densidad), 0, Math.round(8 * densidad));

            // Nombre del día (hoy en negrita)
            TextView tvDia = new TextView(requireContext());
            tvDia.setText(dias[d]);
            tvDia.setTextSize(15);
            tvDia.setTextColor(ContextCompat.getColor(requireContext(),
                    d == HOY ? R.color.texto_principal : R.color.texto_secundario));
            if (d == HOY) tvDia.setTypeface(tvDia.getTypeface(), Typeface.BOLD);
            fila.addView(tvDia, new LinearLayout.LayoutParams(Math.round(56 * densidad),
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            // 4 celdas con su icono, centradas en su columna
            for (int h = 0; h < 4; h++) {
                FrameLayout celda = new FrameLayout(requireContext());
                ImageView icono = new ImageView(requireContext());
                ponerIcono(icono, semana[d].charAt(h));
                int tam = Math.round(28 * densidad);
                celda.addView(icono, new FrameLayout.LayoutParams(tam, tam, Gravity.CENTER));
                fila.addView(celda, new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            }

            tabla.addView(fila);
        }

        // Gráfico semanal: se pide la semana de hoy (de lunes a domingo) a la fuente de registros
        Calendar hoy = Calendar.getInstance();
        ResumenSemanal semana = ResumenSemanal.calcular(null, hoy);
        ponerRangoSemana(view, hoy, semana.getIndiceHoy());
        mostrarResumen(semana);

        fuente.escuchar(semana.getLunes(), semana.getDomingo(), new FuenteRegistros.Escuchador() {
            @Override
            public void alRecibir(List<RegistroToma> registros) {
                mostrarResumen(ResumenSemanal.calcular(registros, hoy));
            }

            @Override
            public void alFallar(String mensaje) {
                if (isAdded()) {
                    Toast.makeText(requireContext(),
                            getString(R.string.historial_error_cargar, mensaje), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        // Dejamos de escuchar cuando la pestaña desaparece
        fuente.dejarDeEscuchar();
        super.onDestroyView();
    }

    /** "Esta semana · 26 de octubre – 1 de noviembre" */
    private void ponerRangoSemana(View vista, Calendar hoy, int diasDesdeLunes) {
        SimpleDateFormat formato = new SimpleDateFormat("d 'de' MMMM", Locale.forLanguageTag("es-ES"));
        Calendar lunes = (Calendar) hoy.clone();
        lunes.add(Calendar.DAY_OF_MONTH, -diasDesdeLunes);
        Calendar domingo = (Calendar) lunes.clone();
        domingo.add(Calendar.DAY_OF_MONTH, 6);

        TextView tvRango = vista.findViewById(R.id.tvRangoSemana);
        tvRango.setText(getString(R.string.historial_rango_semana,
                formato.format(lunes.getTime()), formato.format(domingo.getTime())));
    }

    /** Dibuja las barras de cada día o, si no hay registros, el mensaje de semana vacía. */
    private void mostrarResumen(ResumenSemanal resumen) {
        View vista = getView();
        if (vista == null) return;

        LinearLayout grafico = vista.findViewById(R.id.graficoSemana);
        TextView tvVacio = vista.findViewById(R.id.tvGraficoVacio);
        grafico.removeAllViews();

        boolean vacio = resumen.estaVacio();
        grafico.setVisibility(vacio ? View.GONE : View.VISIBLE);
        vista.findViewById(R.id.leyendaGrafico).setVisibility(vacio ? View.GONE : View.VISIBLE);
        tvVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        ponerTarjetaCumplimiento(vista, resumen);
        if (vacio) return;

        float densidad = getResources().getDisplayMetrics().density;
        int altoBarras = Math.round(110 * densidad);
        // Hueco para el número de encima de la barra; en sp para que crezca con el tamaño de letra
        int altoNumero = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18,
                getResources().getDisplayMetrics()));
        int gris = ContextCompat.getColor(requireContext(), R.color.pestana_inactiva);
        int maximo = resumen.getMaximoDia();
        int verde = ContextCompat.getColor(requireContext(), R.color.verde_menta);
        int azul = ContextCompat.getColor(requireContext(), R.color.toma_olvidada);

        // Una columna por día: barra de hechas + barra de olvidadas, y debajo el nombre del día
        for (int d = 0; d < ResumenSemanal.DIAS; d++) {
            int hechas = resumen.getHechas(d);
            int olvidadas = resumen.getOlvidadas(d);

            LinearLayout columna = new LinearLayout(requireContext());
            columna.setOrientation(LinearLayout.VERTICAL);
            columna.setGravity(Gravity.CENTER_HORIZONTAL);
            columna.setContentDescription(
                    getString(R.string.historial_dia_descripcion, dias[d], hechas, olvidadas));

            LinearLayout barras = new LinearLayout(requireContext());
            barras.setOrientation(LinearLayout.HORIZONTAL);
            barras.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            barras.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            barras.addView(crearBarra(hechas, maximo, altoBarras, verde));
            barras.addView(crearBarra(olvidadas, maximo, altoBarras, azul));
            columna.addView(barras, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, altoBarras + altoNumero));

            // Línea base: así un día sin tomas se ve como 0 y no como un hueco
            View base = new View(requireContext());
            base.setBackgroundColor(gris);
            base.setAlpha(0.3f);
            columna.addView(base, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, Math.round(densidad))));

            // Nombre del día (hoy en negrita), como en la tabla. En una sola línea:
            // con letra muy grande la letra se encoge en vez de partir "Dom" en dos
            TextView tvDia = new AppCompatTextView(requireContext());
            tvDia.setText(dias[d]);
            tvDia.setMaxLines(1);
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                    tvDia, 8, 13, 1, TypedValue.COMPLEX_UNIT_SP);
            tvDia.setGravity(Gravity.CENTER);
            tvDia.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            boolean esHoy = d == resumen.getIndiceHoy();
            tvDia.setTextColor(ContextCompat.getColor(requireContext(),
                    esHoy ? R.color.texto_principal : R.color.texto_secundario));
            if (esHoy) tvDia.setTypeface(tvDia.getTypeface(), Typeface.BOLD);
            // Alto fijo en sp: el autoajuste necesita un alto conocido
            LinearLayout.LayoutParams paramsDia = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, Math.round(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP, 20, getResources().getDisplayMetrics())));
            paramsDia.topMargin = Math.round(6 * densidad);
            columna.addView(tvDia, paramsDia);

            grafico.addView(columna, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        }
    }

    /** Una barra con su número encima. La altura es proporcional al día con más tomas. */
    private View crearBarra(int valor, int maximo, int altoMaximo, int color) {
        float densidad = getResources().getDisplayMetrics().density;

        LinearLayout contenedor = new LinearLayout(requireContext());
        contenedor.setOrientation(LinearLayout.VERTICAL);
        contenedor.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);

        TextView tvNumero = new TextView(requireContext());
        tvNumero.setText(valor > 0 ? String.valueOf(valor) : "");
        tvNumero.setTextSize(11);
        tvNumero.setMaxLines(1);
        tvNumero.setTextColor(ContextCompat.getColor(requireContext(), R.color.texto_secundario));
        // WRAP_CONTENT: si no, toma el ancho de la barra (10dp) y con letra grande la cifra se corta
        contenedor.addView(tvNumero, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Si vale 0 no se dibuja; si es muy poco, al menos se ve una raya de 2dp
        int alto = valor == 0 ? 0
                : Math.max(Math.round(2 * densidad), Math.round((float) altoMaximo * valor / maximo));
        View barra = new View(requireContext());
        barra.setBackgroundResource(R.drawable.bg_barra_grafico);
        barra.setBackgroundTintList(ColorStateList.valueOf(color));
        contenedor.addView(barra, new LinearLayout.LayoutParams(Math.round(10 * densidad), alto));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        params.setMargins(Math.round(2 * densidad), 0, Math.round(2 * densidad), 0);
        contenedor.setLayoutParams(params);
        return contenedor;
    }

    /** Rellena la tarjeta azul con los datos reales, o la oculta si no hay registros. */
    private void ponerTarjetaCumplimiento(View vista, ResumenSemanal resumen) {
        View tarjeta = vista.findViewById(R.id.tarjetaCumplimiento);
        int porcentaje = resumen.getPorcentajeCumplimiento();
        if (porcentaje < 0) {
            tarjeta.setVisibility(View.GONE);
            return;
        }

        int hechas = resumen.getTotalHechas();
        int olvidadas = resumen.getTotalOlvidadas();
        ((TextView) vista.findViewById(R.id.tvPorcentaje))
                .setText(getString(R.string.historial_porcentaje, porcentaje));
        ((TextView) vista.findViewById(R.id.tvTomasSemana))
                .setText(getString(R.string.historial_tomas_semana, hechas, hechas + olvidadas));
        ((TextView) vista.findViewById(R.id.tvOlvidadas))
                .setText(getResources().getQuantityString(R.plurals.historial_olvidadas, olvidadas, olvidadas));
        tarjeta.setVisibility(View.VISIBLE);
    }

    /** Elige icono y color según el estado de la toma. */
    private void ponerIcono(ImageView icono, char estado) {
        int verde = ContextCompat.getColor(requireContext(), R.color.verde_menta);
        switch (estado) {
            case 'T': // Tomada: check verde
                icono.setImageResource(R.drawable.ic_toma_hecha);
                icono.setColorFilter(verde);
                break;
            case 'O': // Olvidada: aspa azul
                icono.setImageResource(R.drawable.ic_toma_olvidada);
                icono.setColorFilter(ContextCompat.getColor(requireContext(), R.color.toma_olvidada));
                break;
            case 'R': // Tarde: círculo verde hueco
                icono.setImageResource(R.drawable.ic_toma_pendiente);
                icono.setColorFilter(verde);
                break;
            default:  // Pendiente: círculo gris hueco
                icono.setImageResource(R.drawable.ic_toma_pendiente);
                icono.setColorFilter(ContextCompat.getColor(requireContext(), R.color.pestana_inactiva));
                break;
        }
    }
}
