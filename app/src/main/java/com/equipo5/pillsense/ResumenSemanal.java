package com.equipo5.pillsense;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Cuenta las tomas hechas y olvidadas de cada día de una semana (de lunes a domingo).
 * No sabe nada de Firebase ni de Android, así que se puede probar con pruebas unitarias.
 */
public class ResumenSemanal {

    public static final int DIAS = 7;

    private final String[] fechas;   // "yyyy-MM-dd" de lunes (0) a domingo (6)
    private final int indiceHoy;
    private final int[] hechas = new int[DIAS];
    private final int[] olvidadas = new int[DIAS];
    private final int[] tardias = new int[DIAS];  // se guardan aparte hasta confirmar cómo contarlas

    private ResumenSemanal(String[] fechas, int indiceHoy) {
        this.fechas = fechas;
        this.indiceHoy = indiceHoy;
    }

    /** Agrupa los registros de la semana del día de referencia. Los de otras semanas se ignoran. */
    public static ResumenSemanal calcular(List<RegistroToma> registros, Calendar referencia) {
        ResumenSemanal resumen = new ResumenSemanal(fechasDeLaSemana(referencia), diasDesdeLunes(referencia));
        if (registros == null) return resumen;

        for (RegistroToma registro : registros) {
            if (registro == null) continue;
            int dia = resumen.indiceDe(registro.getFecha());
            if (dia < 0) continue;

            String estado = registro.getEstado();
            if (RegistroToma.TOMADA.equals(estado)) {
                resumen.hechas[dia]++;
            } else if (RegistroToma.OLVIDADA.equals(estado)) {
                resumen.olvidadas[dia]++;
            } else if (RegistroToma.TARDE.equals(estado)) {
                resumen.tardias[dia]++;
            }
            // Las pendientes (y cualquier estado desconocido) no cuentan
        }
        return resumen;
    }

    /** Las 7 fechas "yyyy-MM-dd" de la semana del día de referencia, empezando en lunes. */
    public static String[] fechasDeLaSemana(Calendar referencia) {
        Calendar dia = (Calendar) referencia.clone();
        dia.add(Calendar.DAY_OF_MONTH, -diasDesdeLunes(referencia));

        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        formato.setTimeZone(referencia.getTimeZone());

        String[] fechas = new String[DIAS];
        for (int i = 0; i < DIAS; i++) {
            fechas[i] = formato.format(dia.getTime());
            dia.add(Calendar.DAY_OF_MONTH, 1);
        }
        return fechas;
    }

    // Calendar empieza en domingo (1), así que lo pasamos a lunes = 0 ... domingo = 6
    private static int diasDesdeLunes(Calendar dia) {
        return (dia.get(Calendar.DAY_OF_WEEK) + 5) % 7;
    }

    private int indiceDe(String fecha) {
        if (fecha == null) return -1;
        for (int i = 0; i < DIAS; i++) {
            if (fechas[i].equals(fecha)) return i;
        }
        return -1;
    }

    public String getFecha(int dia) { return fechas[dia]; }
    public String getLunes() { return fechas[0]; }
    public String getDomingo() { return fechas[DIAS - 1]; }
    public int getIndiceHoy() { return indiceHoy; }

    public int getHechas(int dia) { return hechas[dia]; }
    public int getOlvidadas(int dia) { return olvidadas[dia]; }
    public int getTardias(int dia) { return tardias[dia]; }

    public int getTotalHechas() { return sumar(hechas); }
    public int getTotalOlvidadas() { return sumar(olvidadas); }
    public int getTotalTardias() { return sumar(tardias); }

    /** El valor más alto de un día (hechas u olvidadas), para escalar las barras. */
    public int getMaximoDia() {
        int maximo = 0;
        for (int i = 0; i < DIAS; i++) {
            maximo = Math.max(maximo, Math.max(hechas[i], olvidadas[i]));
        }
        return maximo;
    }

    /** No hay nada que dibujar: ninguna toma hecha ni olvidada en toda la semana. */
    public boolean estaVacio() {
        return getTotalHechas() + getTotalOlvidadas() == 0;
    }

    /** Porcentaje de hechas sobre hechas + olvidadas, o -1 si no hay ninguna. */
    public int getPorcentajeCumplimiento() {
        int total = getTotalHechas() + getTotalOlvidadas();
        if (total == 0) return -1;
        return Math.round(100f * getTotalHechas() / total);
    }

    private static int sumar(int[] valores) {
        int total = 0;
        for (int v : valores) total += v;
        return total;
    }
}
