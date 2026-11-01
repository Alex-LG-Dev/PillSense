package com.equipo5.pillsense;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Pruebas de los cálculos del gráfico semanal (T043). */
public class ResumenSemanalTest {

    private static Calendar dia(int anio, int mes, int diaMes) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Madrid"), Locale.ROOT);
        c.clear();
        c.set(anio, mes - 1, diaMes, 12, 0);
        return c;
    }

    private static RegistroToma r(String fecha, String estado) {
        return new RegistroToma(fecha, estado);
    }

    @Test
    public void laSemanaVaDeLunesADomingo() {
        // Miércoles 28 de octubre de 2026
        String[] fechas = ResumenSemanal.fechasDeLaSemana(dia(2026, 10, 28));
        assertArrayEquals(new String[]{
                "2026-10-26", "2026-10-27", "2026-10-28", "2026-10-29",
                "2026-10-30", "2026-10-31", "2026-11-01"}, fechas);
    }

    @Test
    public void elDomingoPerteneceALaSemanaQueTermina() {
        ResumenSemanal resumen = ResumenSemanal.calcular(null, dia(2026, 11, 1));
        assertEquals("2026-10-26", resumen.getLunes());
        assertEquals("2026-11-01", resumen.getDomingo());
        assertEquals(6, resumen.getIndiceHoy());
    }

    @Test
    public void elLunesEmpiezaUnaSemanaNueva() {
        ResumenSemanal resumen = ResumenSemanal.calcular(null, dia(2026, 11, 2));
        assertEquals("2026-11-02", resumen.getLunes());
        assertEquals("2026-11-08", resumen.getDomingo());
        assertEquals(0, resumen.getIndiceHoy());
    }

    @Test
    public void alCambiarDeSemanaSoloCuentanLosRegistrosDeLaSemanaNueva() {
        List<RegistroToma> registros = Arrays.asList(
                r("2026-11-01", RegistroToma.TOMADA),    // domingo de la semana anterior
                r("2026-11-02", RegistroToma.OLVIDADA)); // lunes de la semana nueva

        ResumenSemanal anterior = ResumenSemanal.calcular(registros, dia(2026, 11, 1));
        assertEquals(1, anterior.getTotalHechas());
        assertEquals(0, anterior.getTotalOlvidadas());
        assertEquals(1, anterior.getHechas(6));

        ResumenSemanal nueva = ResumenSemanal.calcular(registros, dia(2026, 11, 2));
        assertEquals(0, nueva.getTotalHechas());
        assertEquals(1, nueva.getTotalOlvidadas());
        assertEquals(1, nueva.getOlvidadas(0));
    }

    @Test
    public void laSemanaPuedeCambiarDeMesYDeAnio() {
        // Jueves 31 de diciembre de 2026
        String[] fechas = ResumenSemanal.fechasDeLaSemana(dia(2026, 12, 31));
        assertEquals("2026-12-28", fechas[0]);
        assertEquals("2027-01-03", fechas[6]);
    }

    @Test
    public void elCambioDeHoraNoDescuadraLaSemana() {
        // El 25 de octubre de 2026 (domingo) se atrasa la hora en España
        String[] fechas = ResumenSemanal.fechasDeLaSemana(dia(2026, 10, 25));
        assertEquals("2026-10-19", fechas[0]);
        assertEquals("2026-10-25", fechas[6]);

        String[] siguiente = ResumenSemanal.fechasDeLaSemana(dia(2026, 10, 26));
        assertEquals("2026-10-26", siguiente[0]);
        assertEquals("2026-11-01", siguiente[6]);
    }

    @Test
    public void agrupaPorDiaYEstado() {
        List<RegistroToma> registros = Arrays.asList(
                r("2026-10-26", RegistroToma.TOMADA),
                r("2026-10-26", RegistroToma.TOMADA),
                r("2026-10-26", RegistroToma.OLVIDADA),
                r("2026-10-28", RegistroToma.TOMADA),
                r("2026-11-01", RegistroToma.OLVIDADA),
                r("2026-11-01", RegistroToma.OLVIDADA));

        ResumenSemanal resumen = ResumenSemanal.calcular(registros, dia(2026, 10, 28));

        assertEquals(2, resumen.getHechas(0));
        assertEquals(1, resumen.getOlvidadas(0));
        assertEquals(1, resumen.getHechas(2));
        assertEquals(0, resumen.getOlvidadas(2));
        assertEquals(0, resumen.getHechas(6));
        assertEquals(2, resumen.getOlvidadas(6));
        assertEquals(3, resumen.getTotalHechas());
        assertEquals(3, resumen.getTotalOlvidadas());
        assertEquals(2, resumen.getMaximoDia());
        assertEquals(50, resumen.getPorcentajeCumplimiento());
    }

    @Test
    public void losDiasSinRegistrosQuedanACero() {
        List<RegistroToma> registros = Collections.singletonList(r("2026-10-29", RegistroToma.TOMADA));
        ResumenSemanal resumen = ResumenSemanal.calcular(registros, dia(2026, 10, 28));

        for (int d = 0; d < ResumenSemanal.DIAS; d++) {
            if (d == 3) continue; // jueves
            assertEquals(0, resumen.getHechas(d));
            assertEquals(0, resumen.getOlvidadas(d));
        }
        assertEquals(1, resumen.getHechas(3));
        assertFalse(resumen.estaVacio());
    }

    @Test
    public void sinRegistrosLaSemanaEstaVacia() {
        ResumenSemanal resumen = ResumenSemanal.calcular(Collections.emptyList(), dia(2026, 10, 28));
        assertTrue(resumen.estaVacio());
        assertEquals(0, resumen.getMaximoDia());
        assertEquals(-1, resumen.getPorcentajeCumplimiento());
    }

    @Test
    public void lasPendientesNoCuentanComoOlvidadas() {
        List<RegistroToma> registros = Arrays.asList(
                r("2026-10-28", RegistroToma.PENDIENTE),
                r("2026-10-28", RegistroToma.PENDIENTE));
        ResumenSemanal resumen = ResumenSemanal.calcular(registros, dia(2026, 10, 28));

        assertEquals(0, resumen.getTotalOlvidadas());
        assertEquals(0, resumen.getTotalHechas());
        assertTrue(resumen.estaVacio());
    }

    @Test
    public void lasTardiasSeGuardanAparteHastaConfirmarComoContarlas() {
        List<RegistroToma> registros = Arrays.asList(
                r("2026-10-27", RegistroToma.TARDE),
                r("2026-10-27", RegistroToma.TOMADA));
        ResumenSemanal resumen = ResumenSemanal.calcular(registros, dia(2026, 10, 28));

        assertEquals(1, resumen.getHechas(1));
        assertEquals(0, resumen.getOlvidadas(1));
        assertEquals(1, resumen.getTardias(1));
        assertEquals(1, resumen.getTotalTardias());
    }

    @Test
    public void seIgnoranRegistrosIncompletosODeOtrasSemanas() {
        List<RegistroToma> registros = Arrays.asList(
                null,
                r(null, RegistroToma.TOMADA),
                r("2026-10-28", null),
                r("2026-10-28", "desconocido"),
                r("2026-10-25", RegistroToma.TOMADA),   // domingo anterior
                r("2026-11-02", RegistroToma.OLVIDADA), // lunes siguiente
                r("28/10/2026", RegistroToma.TOMADA));  // formato que no es el acordado

        ResumenSemanal resumen = ResumenSemanal.calcular(registros, dia(2026, 10, 28));
        assertTrue(resumen.estaVacio());
        assertEquals(0, resumen.getTotalTardias());
    }
}
