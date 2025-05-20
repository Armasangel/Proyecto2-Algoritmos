package com.example;

import java.util.HashSet;
import java.util.Set;

public class Usuario {
    private String id;
    private String nombre;
    private Set<String> amigos;
    private Set<String> juegosJugados;
    private Set<String> juegosGustados;
    
    public Usuario(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
        this.amigos = new HashSet<>();
        this.juegosJugados = new HashSet<>();
        this.juegosGustados = new HashSet<>();
    }
    
    // Getters y setters
    public String getId() {return id;}
    public void setId(String id) {this.id = id;}
    
    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
    
    public Set<String> getAmigos() {return amigos;}
    public void addAmigo(String amigoId) {this.amigos.add(amigoId);}
    
    public Set<String> getJuegosJugados() {return juegosJugados;}
    public void addJuegoJugado(String juegoId) {this.juegosJugados.add(juegoId);}
    
    public Set<String> getJuegosGustados() {return juegosGustados;}
    public void addJuegoGustado(String juegoId) {this.juegosGustados.add(juegoId);}
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Usuario usuario = (Usuario) o;
        return id.equals(usuario.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return "Usuario{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", amigos=" + amigos +
                ", juegosJugados=" + juegosJugados +
                ", juegosGustados=" + juegosGustados +
                '}';
    }
}

