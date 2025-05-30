package com.example;

import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.util.*;
import java.util.stream.Collectors;

public class CollaborativeRecommenderService {
    private final Neo4jConnectionManager connectionManager;

    public CollaborativeRecommenderService(Neo4jConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public List<Recomendacion> recommendGamesByFriends(String userId, int maxRecommendations) {
        Map<String, Integer> gameScores = new HashMap<>();
        Map<String, String> gameNames = new HashMap<>();

        // Juegos que gustan a amigos directos pero que el usuario no ha jugado
        String query =
            "MATCH (user:User {id: $userId})-[:FRIENDS_WITH]->(friend:User)-[:LIKES]->(game:Videojuego) " +
            "WHERE NOT (user)-[:PLAYED|LIKES]->(game) " +
            "RETURN game.id as gameId, game.nombre as gameName, count(friend) as friendCount " +
            "ORDER BY friendCount DESC";

        Result result = connectionManager.executeQuery(query, Values.parameters("userId", userId));

        while (result.hasNext()) {
            Record record = result.next();
            String gameId = record.get("gameId").asString();
            String gameName = record.get("gameName").asString();
            int friendCount = record.get("friendCount").asInt();

            gameScores.put(gameId, friendCount);
            gameNames.put(gameId, gameName);
        }

        // Si hay pocas recomendaciones, buscar con amigos de amigos
        if (gameScores.size() < maxRecommendations) {
            String extendedQuery =
                "MATCH (user:User {id: $userId})-[:FRIENDS_WITH]->(:User)-[:FRIENDS_WITH]->(friendOfFriend:User) " +
                "WHERE NOT (user)-[:FRIENDS_WITH]->(friendOfFriend) AND NOT user = friendOfFriend " +
                "WITH DISTINCT friendOfFriend " +
                "MATCH (friendOfFriend)-[:LIKES]->(game:Videojuego) " +
                "WHERE NOT (user)-[:PLAYED|LIKES]->(game) " +
                "RETURN game.id as gameId, game.nombre as gameName, count(friendOfFriend) as fofCount " +
                "ORDER BY fofCount DESC";

            Result extendedResult = connectionManager.executeQuery(extendedQuery, Values.parameters("userId", userId));

            while (extendedResult.hasNext() && gameScores.size() < maxRecommendations * 2) {
                Record record = extendedResult.next();
                String gameId = record.get("gameId").asString();
                String gameName = record.get("gameName").asString();
                int fofCount = record.get("fofCount").asInt();

                // Peso menor para amigos de amigos
                gameScores.put(gameId, gameScores.getOrDefault(gameId, 0) + fofCount / 2);
                gameNames.put(gameId, gameName);
            }
        }

        return gameScores.entrySet().stream()
            .map(entry -> new Recomendacion(
                entry.getKey(),
                gameNames.get(entry.getKey()),
                entry.getValue(),
                Recomendacion.TipoRecomendacion.COLABORATIVA))
            .sorted(Comparator.comparing(Recomendacion::getPuntuacion).reversed())
            .limit(maxRecommendations)
            .collect(Collectors.toList());
    }

    public void displayRecommendations(List<Recomendacion> recommendations) {
        System.out.println("Recomendaciones colaborativas basadas en tus amigos:");
        for (int i = 0; i < recommendations.size(); i++) {
            Recomendacion rec = recommendations.get(i);
            System.out.println((i + 1) + ". " + rec.getJuegoNombre() + " (Puntuación: " + rec.getPuntuacion() + ")");
        }
    }

    public List<Recomendacion> recommendGamesBySimilarUsers(String userId, int maxRecommendations) {
        Map<String, Integer> gameScores = new HashMap<>();
        Map<String, String> gameNames = new HashMap<>();

        String query =
            "MATCH (user:User {id: $userId})-[:LIKES]->(game:Videojuego)<-[:LIKES]-(otherUser:User) " +
            "WHERE user <> otherUser " +
            "WITH otherUser, count(game) AS commonGames " +
            "WHERE commonGames > 0 " +
            "MATCH (otherUser)-[:LIKES]->(rec:Videojuego) " +
            "WHERE NOT (user)-[:PLAYED|LIKES]->(rec) " +
            "RETURN rec.id AS gameId, rec.nombre AS gameName, sum(commonGames) AS score " +
            "ORDER BY score DESC";

        Result result = connectionManager.executeQuery(query, Values.parameters("userId", userId));

        while (result.hasNext()) {
            Record record = result.next();
            String gameId = record.get("gameId").asString();
            String gameName = record.get("gameName").asString();
            int score = record.get("score").asInt();

            gameScores.put(gameId, score);
            gameNames.put(gameId, gameName);
        }

        return gameScores.entrySet().stream()
            .map(entry -> new Recomendacion(
                entry.getKey(),
                gameNames.get(entry.getKey()),
                entry.getValue(),
                Recomendacion.TipoRecomendacion.COLABORATIVA))
            .sorted(Comparator.comparing(Recomendacion::getPuntuacion).reversed())
            .limit(maxRecommendations)
            .collect(Collectors.toList());
    }
}