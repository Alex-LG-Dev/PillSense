package com.equipo5.pillsense;

/**
 * Lo que pasó con una toma en un día concreto (T043).
 * Provisional: los nombres de campos se cerrarán cuando el equipo acuerde la estructura de Firestore.
 */
public class RegistroToma {

    // Estados posibles de un registro
    public static final String TOMADA = "tomada";
    public static final String OLVIDADA = "olvidada";
    public static final String TARDE = "tarde";         // cómo contarla está pendiente de confirmar
    public static final String PENDIENTE = "pendiente"; // todavía no ha pasado la hora límite

    private String fecha;   // "yyyy-MM-dd"
    private String estado;  // uno de los estados de arriba

    // Firebase necesita un constructor vacío para leer los datos
    public RegistroToma() { }

    public RegistroToma(String fecha, String estado) {
        this.fecha = fecha;
        this.estado = estado;
    }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
