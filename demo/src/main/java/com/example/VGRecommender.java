package com.example;

import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import java.util.*;
import java.util.stream.Collectors;

public class VGRecommender {
    public class VideoGameRecommender {
        private final Driver driver;
    
        private Map<String, Set<String>> genreGamesMap;
        private Map<String, Set<String>> platformGamesMap;
        private Map<String, Set<String>> developerGamesMap;
        private Map<String, Set<String>> multiplayerGamesMap;
    
        public VideoGameRecommender(String uri, String user, String password) {
            this.driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
            
            this.genreGamesMap = new HashMap<>();
            this.platformGamesMap = new HashMap<>();
            this.developerGamesMap = new HashMap<>();
            this.multiplayerGamesMap = new HashMap<>();
            
            populateCategoryMaps();
        }
    
        private void populateCategoryMaps() {
            try (Session session = driver.session()) {
                //Juegos por género
                Result genreResult = session.run(
                    "MATCH (game:Videojuego)-[:BELONGS_TO_GENRE]->(genre:Genre) " +
                    "RETURN game.nombre as gameName, genre.name as genreName"
                );
                
                while (genreResult.hasNext()) {
                    Record record = genreResult.next();
                    String gameName = record.get("gameName").asString();
                    String genreName = record.get("genreName").asString();
                    
                    genreGamesMap.computeIfAbsent(genreName, k -> new HashSet<>()).add(gameName);
                }
                
                //Juegos por plataforma
                Result platformResult = session.run(
                    "MATCH (game:Videojuego)-[:AVAILABLE_ON]->(platform:Platform) " +
                    "RETURN game.nombre as gameName, platform.name as platformName"
                );
                
                while (platformResult.hasNext()) {
                    Record record = platformResult.next();
                    String gameName = record.get("gameName").asString();
                    String platformName = record.get("platformName").asString();
                    
                    platformGamesMap.computeIfAbsent(platformName, k -> new HashSet<>()).add(gameName);
                }
                
                //Juegos por desarrollador
                Result developerResult = session.run(
                    "MATCH (game:Videojuego)-[:DEVELOPED_BY]->(developer:Developer) " +
                    "RETURN game.nombre as gameName, developer.name as developerName"
                );
                
                while (developerResult.hasNext()) {
                    Record record = developerResult.next();
                    String gameName = record.get("gameName").asString();
                    String developerName = record.get("developerName").asString();
                    
                    developerGamesMap.computeIfAbsent(developerName, k -> new HashSet<>()).add(gameName);
                }
                
                //Juegos multijugador
                Result multiplayerResult = session.run(
                    "MATCH (game:Videojuego)-[:HAS_FEATURE]->(feature:Feature) " +
                    "WHERE feature.name = 'Multiplayer' " +
                    "RETURN game.nombre as gameName"
                );
                
                while (multiplayerResult.hasNext()) {
                    Record record = multiplayerResult.next();
                    String gameName = record.get("gameName").asString();
                    
                    multiplayerGamesMap.computeIfAbsent("Multiplayer", k -> new HashSet<>()).add(gameName);
                }
            }
        }

        public void close() {
            driver.close();
        }
    
        //busca juegos por nombre
        private Value findGameNode(String gameName) {
            try (Session session = driver.session()) {
                Result result = session.run(
                    "MATCH (game:Videojuego {nombre: $nombre}) RETURN game",
                    Values.parameters("nombre", gameName)
                );
                
                if (result.hasNext()) {
                    return result.next().get("game");
                }
                return null;
            }
        }
    
        //Obtiene todos los atributos conectados a un videojuego
        private List<Value> getGameAttributes(Value gameNode) {
            List<Value> attributes = new ArrayList<>();
            
            try (Session session = driver.session()) {
                Result result = session.run(
                    "MATCH (game:Videojuego)-[r]-(attribute) " +
                    "WHERE ID(game) = $gameId " +
                    "RETURN type(r) as relationType, attribute",
                    Values.parameters("gameId", gameNode.asNode().id())
                );
                
                while (result.hasNext()) {
                    Record record = result.next();
                    attributes.add(record.get("attribute"));
                }
            }
            
            return attributes;
        }
    
        //juegos con 1 atributo especifico
        private List<String> getGamesWithAttribute(Value attributeNode) {
            List<String> games = new ArrayList<>();
            
            try (Session session = driver.session()) {
                Result result = session.run(
                    "MATCH (game:Videojuego)-[]-(attribute) " +
                    "WHERE ID(attribute) = $attributeId " +
                    "RETURN game.nombre as nombreJuego",
                    Values.parameters("attributeId", attributeNode.asNode().id())
                );
                
                while (result.hasNext()) {
                    Record record = result.next();
                    games.add(record.get("nombreJuego").asString());
                }
            }
            
            return games;
        }
    
        //Recomienda juegos con un juego de base
        public List<String> recommendGames(String baseGame, int maxRecommendations) {
            Value baseGameNode = findGameNode(baseGame);
            if (baseGameNode == null) {
                System.out.println("Videojuego base no encontrado en la base de datos");
                return Collections.emptyList();
            }
            List<Value> baseAttributes = getGameAttributes(baseGameNode);

            Map<String, Integer> scoredGames = new HashMap<>();
            
            for (Value attribute : baseAttributes) {
                List<String> gamesWithAttribute = getGamesWithAttribute(attribute);
                
                for (String game : gamesWithAttribute) {
                    if (!game.equals(baseGame)) {
                        scoredGames.put(game, scoredGames.getOrDefault(game, 0) + 1);
                    }
                }
            }
            
            List<Map.Entry<String, Integer>> sortedGames = new ArrayList<>(scoredGames.entrySet());
            sortedGames.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
            
            List<String> recommendations = sortedGames.stream()
                .limit(maxRecommendations)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            
            displayRecommendations(recommendations, scoredGames);
            
            return recommendations;
        }
        
        //Recomendar juegos basado en gustos de amigos
        public List<String> recommendGamesByFriends(String userId, int maxRecommendations) {
            Map<String, Integer> scoredGames = new HashMap<>();
            
            try (Session session = driver.session()) {                Result result = session.run(
                    "MATCH (user:User {id: $userId})-[:FRIENDS_WITH]->(friend:User)-[:LIKES]->(game:Videojuego) " +
                    "WHERE NOT (user)-[:PLAYED|LIKES]->(game) " +
                    "RETURN game.nombre as gameName, count(friend) as friendCount " +
                    "ORDER BY friendCount DESC " +
                    "LIMIT $limit",
                    Values.parameters("userId", userId, "limit", maxRecommendations)
                );
                
                while (result.hasNext()) {
                    Record record = result.next();
                    String gameName = record.get("gameName").asString();
                    int friendCount = record.get("friendCount").asInt();
                    scoredGames.put(gameName, friendCount);
                }
            }
            
            //Lista ordenada
            List<Map.Entry<String, Integer>> sortedGames = new ArrayList<>(scoredGames.entrySet());
            sortedGames.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
            
            List<String> recommendations = sortedGames.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            
            //Mostrar recomendaciones
            System.out.println("Recomendaciones basadas en tus amigos:");
            for (int i = 0; i < recommendations.size(); i++) {
                String game = recommendations.get(i);
                int score = scoredGames.get(game);
                System.out.println((i+1) + ". " + game + " (Puntuación: " + score + " amigos)");
            }
            
            return recommendations;
        }
        
        //Recomendar juegos del mismo genero 
        public List<String> recommendGamesByGenre(String baseGame, int maxRecommendations) {
            Set<String> genres = new HashSet<>();
            Set<String> recommendations = new HashSet<>();
            
            try (Session session = driver.session()) {
                Result genreResult = session.run(
                    "MATCH (game:Videojuego {nombre: $nombre})-[:BELONGS_TO_GENRE]->(genre:Genre) " +
                    "RETURN genre.name as genreName",
                    Values.parameters("nombre", baseGame)
                );
                
                while (genreResult.hasNext()) {
                    genres.add(genreResult.next().get("genreName").asString());
                }
                
                //Para cada género, añadir juegos a las recomendaciones
                for (String genre : genres) {
                    Set<String> gamesInGenre = genreGamesMap.getOrDefault(genre, Collections.emptySet());
                    for (String game : gamesInGenre) {
                        if (!game.equals(baseGame)) {
                            recommendations.add(game);
                        }
                    }
                }
            }
            
            return new ArrayList<>(recommendations).subList(0, Math.min(maxRecommendations, recommendations.size()));
        }
    
         //Mostrar las recomendaciones al usuario
        private void displayRecommendations(List<String> recommendations, Map<String, Integer> scores) {
            System.out.println("Recomendaciones basadas en sus preferencias:");
            for (int i = 0; i < recommendations.size(); i++) {
                String game = recommendations.get(i);
                int score = scores.get(game);
                System.out.println((i+1) + ". " + game + " (Puntuación: " + score + ")");
            }
        }   
    } 
}
