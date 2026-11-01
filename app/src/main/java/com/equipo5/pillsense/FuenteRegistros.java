package com.equipo5.pillsense;

import java.util.List;

/**
 * De dónde salen los registros de tomas del Historial.
 * El gráfico solo conoce esta interfaz, así que conectar Firestore será añadir otra implementación.
 */
public interface FuenteRegistros {

    interface Escuchador {
        /** Registros entre las dos fechas pedidas. Se puede llamar varias veces si cambian los datos. */
        void alRecibir(List<RegistroToma> registros);

        void alFallar(String mensaje);
    }

    /** Empieza a escuchar los registros entre desde y hasta (incluidas), en formato "yyyy-MM-dd". */
    void escuchar(String desde, String hasta, Escuchador escuchador);

    /** Deja de escuchar. Hay que llamarlo cuando la pantalla desaparece. */
    void dejarDeEscuchar();
}
