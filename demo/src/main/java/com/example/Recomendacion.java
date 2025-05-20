package com.example;

public class Recomendacion {
    private String juegoId;
    private String juegoNombre;
    private int puntuacion;
    private TipoRecomendacion tipo;
    
    public enum TipoRecomendacion {
        PERSONAL,
        COLABORATIVA
    }
    
    public Recomendacion(String juegoId, String juegoNombre, int puntuacion, TipoRecomendacion tipo) {
        this.juegoId = juegoId;
        this.juegoNombre = juegoNombre;
        this.puntuacion = puntuacion;
        this.tipo = tipo;
    }
    
    // Getters y setters
    public String getJuegoId() {return juegoId;}
    public void setJuegoId(String juegoId) {this.juegoId = juegoId;}
    
    public String getJuegoNombre() {return juegoNombre;}
    public void setJuegoNombre(String juegoNombre) {this.juegoNombre = juegoNombre;}
    
    public int getPuntuacion() {return puntuacion;}
    public void setPuntuacion(int puntuacion) {this.puntuacion = puntuacion;}
    
    public TipoRecomendacion getTipo() {return tipo;}
    public void setTipo(TipoRecomendacion tipo) {this.tipo = tipo;}
    
    @Override
    public String toString() {
        return juegoNombre + " (Puntuación: " + puntuacion + ", Tipo: " + tipo + ")";
    }
}