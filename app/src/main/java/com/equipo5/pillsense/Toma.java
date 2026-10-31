package com.equipo5.pillsense;

/** Un horario de toma. Se guarda en Realtime Database en tomas. */
public class Toma {

    private String id;
    private String idMedicamento;      // id del medicamento
    private String nombreMedicamento;  // copia del nombre, para enseñarlo sin buscarlo otra vez
    private int cajon;
    private String hora;
    private String horaLimite;         // "09:00": a partir de aquí la toma cuenta como olvidada

    // Firebase necesita un constructor vacío para leer los datos
    public Toma() { }

    public Toma(String id, String idMedicamento, String nombreMedicamento,
                int cajon, String hora, String horaLimite) {
        this.id = id;
        this.idMedicamento = idMedicamento;
        this.nombreMedicamento = nombreMedicamento;
        this.cajon = cajon;
        this.hora = hora;
        this.horaLimite = horaLimite;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdMedicamento() { return idMedicamento; }
    public void setIdMedicamento(String idMedicamento) { this.idMedicamento = idMedicamento; }

    public String getNombreMedicamento() { return nombreMedicamento; }
    public void setNombreMedicamento(String nombreMedicamento) { this.nombreMedicamento = nombreMedicamento; }

    public int getCajon() { return cajon; }
    public void setCajon(int cajon) { this.cajon = cajon; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public String getHoraLimite() { return horaLimite; }
    public void setHoraLimite(String horaLimite) { this.horaLimite = horaLimite; }
}