package com.equipo5.pillsense;

import java.util.Collections;

/** Fuente provisional sin datos: se usa hasta conectar Firestore, para no enseñar registros inventados. */
public class FuenteRegistrosVacia implements FuenteRegistros {

    @Override
    public void escuchar(String desde, String hasta, Escuchador escuchador) {
        escuchador.alRecibir(Collections.emptyList());
    }

    @Override
    public void dejarDeEscuchar() { }
}
