package com.example;

import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.util.*;
import java.util.stream.Collectors;

public class PersonalRecommenderService {
    private final Neo4jConnectionManager connectionManager;
    private final Map<String, Set<String>> genreGamesMap;
    private final Map<String, Set<String>> platformGamesMap;
    private final Map<String, Set<String>> developerGamesMap;
    
    public PersonalRecommenderService(Neo4jConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.genreGamesMap = new HashMap<>();
        this.platformGamesMap = new HashMap<>();
        this.developerGamesMap = new HashMap<>();
        
        // Inicializar los mapas con datos de la base de datos
        initializeCategoryMaps();
    }

    private void initializeCategoryMaps() {
        // Obtener juegos por género
        Result genreResult = connectionManager.executeQuery(
            "MATCH (game:Videojuego)-[:BELONGS_TO_GENRE]->(genre:Genre) " +
            "RETURN game.id as gameId, game.nombre as gameName, genre.name as genreName"
        );
        
        while (genreResult.hasNext()) {
            Record record = genreResult.next();
            String gameId = record.get("gameId").asString();
            String genreName = record.get("genreName").asString();
            
            genreGamesMap.computeIfAbsent(genreName, k -> new HashSet<>()).add(gameId);
        }
        
        // Obtener juegos por plataforma
        Result platformResult = connectionManager.executeQuery(
            "MATCH (game:Videojuego)-[:AVAILABLE_ON]->(platform:Platform) " +
            "RETURN game.id as gameId, platform.name as platformName"
        );
        
        while (platformResult.hasNext()) {
            Record record = platformResult.next();
            String gameId = record.get("gameId").asString();
            String platformName = record.get("platformName").asString();
            
            platformGamesMap.computeIfAbsent(platformName, k -> new HashSet<>()).add(gameId);
        }
        
        // Obtener juegos por desarrollador
        Result developerResult = connectionManager.executeQuery(
            "MATCH (game:Videojuego)-[:DEVELOPED_BY]->(developer:Developer) " +
            "RETURN game.id as gameId, developer.name as developerName"
        );
        
        while (developerResult.hasNext()) {
            Record record = developerResult.next();
            String gameId = record.get("gameId").asString();
            String developerName = record.get("developerName").asString();
            
            developerGamesMap.computeIfAbsent(developerName, k -> new HashSet<>()).add(gameId);
        }
    }

    public List<Recomendacion> recommendGamesByGame(String baseGameId, int maxRecommendations) {
        // Buscar el juego base
        Value baseGameNode = findGameNode(baseGameId);
        if (baseGameNode == null) {
            System.out.println("Juego base no encontrado en la base de datos");
            return Collections.emptyList();
        }
        
        // Obtener atributos del juego base
        List<Value> baseAttributes = getGameAttributes(baseGameNode);
        
        // Mapa para almacenar puntuaciones de juegos
        Map<String, Integer> gameScores = new HashMap<>();
        Map<String, String> gameNames = new HashMap<>();
        
        // Para cada atributo, encontrar juegos que lo comparten
        for (Value attribute : baseAttributes) {
            List<Map<String, Object>> gamesWithAttribute = getGamesWithAttribute(attribute);
            
            // Actualizar puntuaciones
            for (Map<String, Object> gameInfo : gamesWithAttribute) {
                String gameId = (String) gameInfo.get("id");
                String gameName = (String) gameInfo.get("nombre");
                
                if (!gameId.equals(baseGameId)) {
                    gameScores.put(gameId, gameScores.getOrDefault(gameId, 0) + 1);
                    gameNames.put(gameId, gameName);
                }
            }
        }
        
        // Convertir a lista de recomendaciones
        List<Recomendacion> recommendations = gameScores.entrySet().stream()
            .map(entry -> new Recomendacion(
                entry.getKey(), 
                gameNames.get(entry.getKey()), 
                entry.getValue(), 
                Recomendacion.TipoRecomendacion.PERSONAL))
            .sorted(Comparator.comparing(Recomendacion::getPuntuacion).reversed())
            .limit(maxRecommendations)
            .collect(Collectors.toList());
        
        return recommendations;
    }

    public List<Recomendacion> recommendGamesByUserPreferences(String userId, int maxRecommendations) {
        // Obtener géneros preferidos del usuario
        Set<String> preferredGenres = getUserPreferredGenres(userId);
        
        // Obtener plataformas preferidas del usuario
        Set<String> preferredPlatforms = getUserPreferredPlatforms(userId);
        
        // Mapa para almacenar puntuaciones de juegos
        Map<String, Integer> gameScores = new HashMap<>();
        Map<String, String> gameNames = new HashMap<>();
        
        // Obtener juegos que el usuario ha jugado o le han gustado
        Set<String> userGames = getUserGames(userId);
        
        // Consulta para obtener juegos que coinciden con las preferencias del usuario
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("MATCH (game:Videojuego) ");
        
        // Añadir condiciones de género si hay géneros preferidos
        if (!preferredGenres.isEmpty()) {
            queryBuilder.append("MATCH (game)-[:BELONGS_TO_GENRE]->(genre:Genre) ");
            queryBuilder.append("WHERE genre.name IN $genres ");
        }
        
        // Añadir condiciones de plataforma si hay plataformas preferidas
        if (!preferredPlatforms.isEmpty()) {
            if (!preferredGenres.isEmpty()) {
                queryBuilder.append("AND ");
            } else {
                queryBuilder.append("WHERE ");
            }
            queryBuilder.append("(game)-[:AVAILABLE_ON]->(:Platform) WHERE platform.name IN $platforms ");
        }
        
        // Excluir juegos que el usuario ya ha jugado
        if (!userGames.isEmpty()) {
            if (preferredGenres.isEmpty() && preferredPlatforms.isEmpty()) {
                queryBuilder.append("WHERE ");
            } else {
                queryBuilder.append("AND ");
            }
            queryBuilder.append("NOT game.id IN $userGames ");
        }
        
        queryBuilder.append("RETURN game.id as gameId, game.nombre as gameName");
        
        // Parámetros para la consulta
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("genres", preferredGenres.toArray());
        parameters.put("platforms", preferredPlatforms.toArray());
        parameters.put("userGames", userGames.toArray());
        
        // Ejecutar la consulta
        Result result = connectionManager.executeQuery(queryBuilder.toString(), Values.value(parameters));
        
        // Procesar resultados
        while (result.hasNext()) {
            Record record = result.next();
            String gameId = record.get("gameId").asString();
            String gameName = record.get("gameName").asString();
            
            // Calcular puntuación basada en cuántos criterios cumple
            int score = 0;
            
            // Verificar coincidencias de género
            for (String genre : preferredGenres) {
                if (genreGamesMap.getOrDefault(genre, Collections.emptySet()).contains(gameId)) {
                    score += 2; // Mayor peso a coincidencias de género
                }
            }
            
            // Verificar coincidencias de plataforma
            for (String platform : preferredPlatforms) {
                if (platformGamesMap.getOrDefault(platform, Collections.emptySet()).contains(gameId)) {
                    score += 1; // Menor peso a coincidencias de plataforma
                }
            }
            
            gameScores.put(gameId, score);
            gameNames.put(gameId, gameName);
        }
        
        // Convertir a lista de recomendaciones
        List<Recomendacion> recommendations = gameScores.entrySet().stream()
            .map(entry -> new Recomendacion(
                entry.getKey(), 
                gameNames.get(entry.getKey()), 
                entry.getValue(), 
                Recomendacion.TipoRecomendacion.PERSONAL))
            .sorted(Comparator.comparing(Recomendacion::getPuntuacion).reversed())
            .limit(maxRecommendations)
            .collect(Collectors.toList());
        
        return recommendations;
    }

    private Set<String> getUserPreferredGenres(String userId) {
        Set<String> genres = new HashSet<>();
        
        String query = 
            "MATCH (user:User {id: $userId})-[:LIKES]->(game:Videojuego)-[:BELONGS_TO_GENRE]->(genre:Genre) " +
            "RETURN DISTINCT genre.name as genreName";
        
        Result result = connectionManager.executeQuery(query, Values.parameters("userId", userId));
        
        while (result.hasNext()) {
            genres.add(result.next().get("genreName").asString());
        }
        
        return genres;
    }

    private Set<String> getUserPreferredPlatforms(String userId) {
        Set<String> platforms = new HashSet<>();
        
        String query = 
            "MATCH (user:User {id: $userId})-[:LIKES]->(game:Videojuego)-[:AVAILABLE_ON]->(platform:Platform) " +
            "RETURN DISTINCT platform.name as platformName";
        
        Result result = connectionManager.executeQuery(query, Values.parameters("userId", userId));
        
        while (result.hasNext()) {
            platforms.add(result.next().get("platformName").asString());
        }
        
        return platforms;
    }

    private Set<String> getUserGames(String userId) {
        Set<String> games = new HashSet<>();
        
        String query = 
            "MATCH (user:User {id: $userId})-[:PLAYED|LIKES]->(game:Videojuego) " +
            "RETURN DISTINCT game.id as gameId";
        
        Result result = connectionManager.executeQuery(query, Values.parameters("userId", userId));
        
        while (result.hasNext()) {
            games.add(result.next().get("gameId").asString());
        }
        
        return games;
    }

    private Value findGameNode(String gameId) {
        Result result = connectionManager.executeQuery(
            "MATCH (game:Videojuego {id: $id}) RETURN game",
            Values.parameters("id", gameId)
        );
        
        if (result.hasNext()) {
            return result.next().get("game");
        }
        return null;
    }

    private List<Value> getGameAttributes(Value gameNode) {
        List<Value> attributes = new ArrayList<>();
        
        Result result = connectionManager.executeQuery(
            "MATCH (game:Videojuego)-[r]-(attribute) " +
            "WHERE ID(game) = $gameId " +
            "RETURN type(r) as relationType, attribute",
            Values.parameters("gameId", gameNode.asNode().id())
        );
        
        while (result.hasNext()) {
            Record record = result.next();
            attributes.add(record.get("attribute"));
        }
        
        return attributes;
    }

    private List<Map<String, Object>> getGamesWithAttribute(Value attributeNode) {
        List<Map<String, Object>> games = new ArrayList<>();
        
        Result result = connectionManager.executeQuery(
            "MATCH (game:Videojuego)-[]-(attribute) " +
            "WHERE ID(attribute) = $attributeId " +
            "RETURN game.id as id, game.nombre as nombre",
            Values.parameters("attributeId", attributeNode.asNode().id())
        );
        
        while (result.hasNext()) {
            Record record = result.next();
            Map<String, Object> gameInfo = new HashMap<>();
            gameInfo.put("id", record.get("id").asString());
            gameInfo.put("nombre", record.get("nombre").asString());
            games.add(gameInfo);
        }
        
        return games;
    }

    public void displayRecommendations(List<Recomendacion> recommendations) {
        System.out.println("Recomendaciones personalizadas basadas en tus preferencias:");
        for (int i = 0; i < recommendations.size(); i++) {
            Recomendacion rec = recommendations.get(i);
            System.out.println((i+1) + ". " + rec.getJuegoNombre() + " (Puntuación: " + rec.getPuntuacion() + ")");
        }
    }
}