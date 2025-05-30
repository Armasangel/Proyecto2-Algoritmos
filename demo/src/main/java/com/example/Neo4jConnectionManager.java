package com.example;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.Result;

public class Neo4jConnectionManager {
    private final Driver driver;

    public Neo4jConnectionManager() {
        this("bolt://localhost:7687", "neo4j", "12345678");
    }

    public Neo4jConnectionManager(String uri, String usuario, String contraseña) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(usuario, contraseña));
    }

    public void cerrar() {
        driver.close();
    }

    public void probarConexion() {
        try (Session session = driver.session()) {
            String saludo = session.run("RETURN '¡Hola desde Neo4j!'").single().get(0).asString();
            System.out.println(saludo);
        }
    }

    public Result executeQuery(String query, Value parameters) {
        try (Session session = driver.session()) {
            return session.run(query, parameters);
        }
    }

    public Result executeQuery(String query) {
        return executeQuery(query, Values.parameters());
    }
}
