package com.example;

import org.neo4j.driver.*;

public class Neo4jConnectionManager implements AutoCloseable {
    private final Driver driver;
     
    public Neo4jConnectionManager(String uri, String user, String password) {
        this.driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    }
    
    public Session getSession() {
        return driver.session();
    }
    
    public boolean testConnection() {
        try (Session session = driver.session()) {
            session.run("RETURN 1").consume();
            return true;
        } catch (Exception e) {
            System.err.println("Error al conectar con Neo4j: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public void close() {
        if (driver != null) {
            driver.close();
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