package com.equipo5.pillsense;


public class Medicamento {

    private String id;
    private String nombre;
    private String dosis;          // por ejemplo "20 mg"
    private int cajon;             // del 1 al 4
    private String imagenBase64;   // foto en Base64 (sin Firebase Storage)

    // Firebase necesita un constructor vacío para leer los datos
    public Medicamento() { }

    public Medicamento(String id, String nombre, String dosis, int cajon, String imagenBase64) {
        this.id = id;
        this.nombre = nombre;
        this.dosis = dosis;
        this.cajon = cajon;
        this.imagenBase64 = imagenBase64;
    }

    // Firebase también necesita getters y setters públicos
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDosis() { return dosis; }
    public void setDosis(String dosis) { this.dosis = dosis; }

    public int getCajon() { return cajon; }
    public void setCajon(int cajon) { this.cajon = cajon; }

    public String getImagenBase64() { return imagenBase64; }
    public void setImagenBase64(String imagenBase64) { this.imagenBase64 = imagenBase64; }
}