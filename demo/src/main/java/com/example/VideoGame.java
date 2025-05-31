package com.example;

import java.util.HashSet;
import java.util.Set;

public class VideoGame {
    private String titulo;
    private int maxJugadores;
    private boolean multiplataforma;
    private Set<String> generos;
    private String publicador;
    private double puntajeCritica;
    private double ventas;
    private String consola;
    private String rating;
    private int anioLanzamiento;
    
    public VideoGame(String titulo, int anioLanzamiento, int maxJugadores, boolean multiplataforma, String publicador, double puntajeCritica, double ventas, String consola, String rating){ 
        this.titulo = titulo;
        this.anioLanzamiento = anioLanzamiento;
        this.generos = new HashSet<>();
        this.maxJugadores = maxJugadores;
        this.multiplataforma = multiplataforma;
        this.publicador = publicador;
        this.puntajeCritica = puntajeCritica;
        this.ventas = ventas;
        this.consola = consola;
        this.rating = rating;
    }
    
    public String getTitulo() { return titulo; }
    public int getMaxJugadores() { return maxJugadores; }
    public boolean isMultiplataforma() { return multiplataforma; }
    public Set<String> getGeneros() { return generos; }
    public String getPublicador() { return publicador; }
    public double getPuntajeCritica() { return puntajeCritica; }
    public double getVentas() { return ventas; }
    public String getConsola() { return consola; }
    public int getAnioLanzamiento() {return anioLanzamiento;}
    public String getRating() { return rating; }

    public void setTitulo(String titulo) { this.titulo = titulo; }
    public void setMaxJugadores(int maxJugadores) { this.maxJugadores = maxJugadores; }
    public void setMultiplataforma(boolean multiplataforma) { this.multiplataforma = multiplataforma;}
    public void setGeneros(Set<String> generos){this.generos = generos;}
    public void setPublicador(String publicador) { this.publicador = publicador; }
    public void setPuntajeCritica(double puntajeCritica) { this.puntajeCritica = puntajeCritica;}
    public void setVentas(double ventas) { this.ventas = ventas; }
    public void setConsola(String consola) { this.consola = consola; }
    public void setAnioLanzamiento(int anioLanzamiento){this.anioLanzamiento = anioLanzamiento;}
    public void setRating(String rating) { this.rating = rating; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoGame videoGame = (VideoGame) o;
        return titulo.equals(videoGame.titulo);
    }
    
    @Override
    public int hashCode() {
        return titulo.hashCode();
    }
}
