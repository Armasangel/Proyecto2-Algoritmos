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
        this("neo4j+s://6bc72245.databases.neo4j.io", "neo4j", "20z23qOU77VA4J4Em7y5D-0uMFc6f87tB5Q8upJSTsk");
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
    Session session = driver.session();
    return session.run(query, parameters);
    // Nota: Ahora el llamador es responsable de cerrar la sesión
    }

    public Result executeQuery(String query) {
        return executeQuery(query, Values.parameters());
    }

    public Driver getDriver() {
        return driver;
    }
}
