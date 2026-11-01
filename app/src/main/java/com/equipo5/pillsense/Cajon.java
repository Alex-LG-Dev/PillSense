package com.equipo5.pillsense;

import java.util.ArrayList;
import java.util.List;


/** Un cajón del pastillero con sus medicamentos  */
public class Cajon {

    private final int numero;          // del 1 al 4
    private final String momento;      // "Mañana", "Mediodía", "Tarde" o "Noche"
    private final List<Medicamento> medicamentos;

    public Cajon(int numero, String momento) {
        this.numero = numero;
        this.momento = momento;
        this.medicamentos = new ArrayList<>();
    }

    public int getNumero() { return numero; }

    public String getMomento() { return momento; }

    public List<Medicamento> getMedicamentos() { return medicamentos; }

    public void anadirMedicamento(Medicamento med) {
        medicamentos.add(med);
    }
}