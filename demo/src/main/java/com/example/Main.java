package com.example;

public class Main {
    public static void main(String[] args) {
        Neo4jConnectionManager conexion = new Neo4jConnectionManager();
        conexion.probarConexion();
        conexion.cerrar();
    }
}
