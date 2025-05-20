package com.example;

import java.util.HashSet;
import java.util.Set;

public class VideoGame {
    private String id;
    private String nombre;
    private int anioLanzamiento;
    private Set<String> generos;
    private Set<String> plataformas;
    private Set<String> desarrolladores;
    private boolean esMultijugador;
    
    public VideoGame(String id, String nombre, int anioLanzamiento) {
        this.id = id;
        this.nombre = nombre;
        this.anioLanzamiento = anioLanzamiento;
        this.generos = new HashSet<>();
        this.plataformas = new HashSet<>();
        this.desarrolladores = new HashSet<>();
        this.esMultijugador = false;
    }
    
    // Getters y setters
    public String getId() {return id;}
    public void setId(String id) {this.id = id;}
    
    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
    
    public int getAnioLanzamiento() {return anioLanzamiento;}
    public void setAnioLanzamiento(int anioLanzamiento) {this.anioLanzamiento = anioLanzamiento;}
    
    public Set<String> getGeneros() {return generos;}
    public void addGenero(String genero) {this.generos.add(genero);}
    
    public Set<String> getPlataformas() {return plataformas;}
    public void addPlataforma(String plataforma) {this.plataformas.add(plataforma);}
    
    public Set<String> getDesarrolladores() {return desarrolladores;}
    public void addDesarrollador(String desarrollador) {this.desarrolladores.add(desarrollador);}
    
    public boolean isEsMultijugador() {return esMultijugador;}
    public void setEsMultijugador(boolean esMultijugador) {this.esMultijugador = esMultijugador;}
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        VideoGame videoGame = (VideoGame) o;
        return id.equals(videoGame.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return "VideoGame{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", anioLanzamiento=" + anioLanzamiento +
                ", generos=" + generos +
                ", plataformas=" + plataformas +
                ", desarrolladores=" + desarrolladores +
                ", esMultijugador=" + esMultijugador +
                '}';
    }
}

