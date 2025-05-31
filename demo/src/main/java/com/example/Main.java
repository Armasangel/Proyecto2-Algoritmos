package com.example;

import java.util.Scanner;
import java.util.List;

public class Main {
    private static Neo4jConnectionManager conexion;
    private static PersonalRecommenderService personalRecommender;
    private static CollaborativeRecommenderService collaborativeRecommender;
    private static Scanner scanner;

    public static void main(String[] args) {
        try {
            // Inicializar conexión
            conexion = new Neo4jConnectionManager();
            conexion.probarConexion();
            
            // Inicializar servicios
            personalRecommender = new PersonalRecommenderService(conexion);
            collaborativeRecommender = new CollaborativeRecommenderService(conexion);
            
            scanner = new Scanner(System.in);
            
            // Menú principal
            boolean salir = false;
            while (!salir) {
                mostrarMenu();
                int opcion = scanner.nextInt();
                scanner.nextLine(); // Limpiar buffer
                
                switch (opcion) {
                    case 1: 
                        recomendacionesPorPreferencias(); 
                        break;
                    case 2: 
                        recomendacionesPorJuego(); 
                        break;
                    case 3: 
                        recomendacionesPorAmigos(); 
                        break;
                    case 4: 
                        recomendacionesPorUsuariosSimilares(); 
                        break;
                    case 5: 
                        salir = true; 
                        break;
                    default: 
                        System.out.println("Opción no válida. Intente nuevamente.");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conexion != null) {
                conexion.cerrar();
            }
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    private static void mostrarMenu() {
        System.out.println("\n=== SISTEMA DE RECOMENDACIÓN DE VIDEOJUEGOS ===");
        System.out.println("1. Recomendaciones basadas en tus preferencias");
        System.out.println("2. Recomendaciones basadas en un juego que te gusta");
        System.out.println("3. Recomendaciones basadas en tus amigos");
        System.out.println("4. Recomendaciones basadas en usuarios similares");
        System.out.println("5. Salir");
        System.out.print("Seleccione una opción: ");
    }

    private static void recomendacionesPorPreferencias() {
        System.out.println("\n--- RECOMENDACIONES POR PREFERENCIAS ---");
        System.out.print("Ingrese su ID de usuario: ");
        String userId = scanner.nextLine();
        
        System.out.print("Número máximo de recomendaciones: ");
        int maxRecomendaciones = scanner.nextInt();
        scanner.nextLine(); // Limpiar buffer
        
        List<Recomendacion> recomendaciones = personalRecommender.recommendGamesByUserPreferences(userId, maxRecomendaciones);
        System.out.println("\nResultados:");
        personalRecommender.displayRecommendations(recomendaciones);
    }

    private static void recomendacionesPorJuego() {
        System.out.println("\n--- RECOMENDACIONES POR JUEGO ---");
        System.out.print("Ingrese el ID del juego que le gusta: ");
        String juegoId = scanner.nextLine();
        
        System.out.print("Número máximo de recomendaciones: ");
        int maxRecomendaciones = scanner.nextInt();
        scanner.nextLine(); // Limpiar buffer
        
        List<Recomendacion> recomendaciones = personalRecommender.recommendGamesByGame(juegoId, maxRecomendaciones);
        System.out.println("\nResultados:");
        personalRecommender.displayRecommendations(recomendaciones);
    }

    private static void recomendacionesPorAmigos() {
        System.out.println("\n--- RECOMENDACIONES POR AMIGOS ---");
        System.out.print("Ingrese su ID de usuario: ");
        String userId = scanner.nextLine();
        
        System.out.print("Número máximo de recomendaciones: ");
        int maxRecomendaciones = scanner.nextInt();
        scanner.nextLine(); // Limpiar buffer
        
        List<Recomendacion> recomendaciones = collaborativeRecommender.recommendGamesByFriends(userId, maxRecomendaciones);
        System.out.println("\nResultados:");
        collaborativeRecommender.displayRecommendations(recomendaciones);
    }

    private static void recomendacionesPorUsuariosSimilares() {
        System.out.println("\n--- RECOMENDACIONES POR USUARIOS SIMILARES ---");
        System.out.print("Ingrese su ID de usuario: ");
        String userId = scanner.nextLine();
        
        System.out.print("Número máximo de recomendaciones: ");
        int maxRecomendaciones = scanner.nextInt();
        scanner.nextLine(); // Limpiar buffer
        
        List<Recomendacion> recomendaciones = collaborativeRecommender.recommendGamesBySimilarUsers(userId, maxRecomendaciones);
        System.out.println("\nResultados:");
        collaborativeRecommender.displayRecommendations(recomendaciones);
    }
}
