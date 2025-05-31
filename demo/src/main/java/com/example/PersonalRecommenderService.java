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
        // Juegos por género
        Session genreSession = connectionManager.getDriver().session();
        Result genreResult = genreSession.run(
            "MATCH (game:Videojuego)-[:BELONGS_TO_GENRE]->(genre:Genre) " +
            "RETURN game.nombre as gameName, genre.name as genreName"
        );
    
        while (genreResult.hasNext()) {
            Record record = genreResult.next();
            String gameName = record.get("gameName").asString();
            String genreName = record.get("genreName").asString();
        
            genreGamesMap.computeIfAbsent(genreName, k -> new HashSet<>()).add(gameName);
        }
        genreSession.close();
    
        // Juegos por plataforma
        Session platformSession = connectionManager.getDriver().session();
        Result platformResult = platformSession.run(
            "MATCH (game:Videojuego)-[:AVAILABLE_ON]->(platform:Platform) " +
            "RETURN platform.name as platformName"
        );
    
        while (platformResult.hasNext()) {
            Record record = platformResult.next();
            String gameName = record.get("gameName").asString();
            String platformName = record.get("platformName").asString();
        
            platformGamesMap.computeIfAbsent(platformName, k -> new HashSet<>()).add(gameName);
        }
        platformSession.close();
    
        // Juegos por desarrollador
        Session developerSession = connectionManager.getDriver().session();
        Result developerResult = developerSession.run(
            "MATCH (game:Videojuego)-[:DEVELOPED_BY]->(developer:Developer) " +
            "RETURN developer.name as developerName"
        );
    
        while (developerResult.hasNext()) {
            Record record = developerResult.next();
            String gameName = record.get("gameName").asString();
            String developerName = record.get("developerName").asString();
        
            developerGamesMap.computeIfAbsent(developerName, k -> new HashSet<>()).add(gameName);
        }
        developerSession.close();
    }

    public List<Recomendacion> recommendGamesByGame(String tituloJuego, int maxRecommendations) {
        // Buscar el juego base
        Value baseGameNode = findGameNode(tituloJuego);
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
                String gameName = (String) gameInfo.get("nombre");
                
                if (!gameName.equals(tituloJuego)) {
                    gameScores.put(gameName, gameScores.getOrDefault(gameName, 0) + 1);
                    gameNames.put(gameName, gameName);
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
            String gameName = record.get("gameName").asString();
            
            // Calcular puntuación basada en cuántos criterios cumple
            int score = 0;
            
            // Verificar coincidencias de género
            for (String genre : preferredGenres) {
                if (genreGamesMap.getOrDefault(genre, Collections.emptySet()).contains(gameName)) {
                    score += 2; // Mayor peso a coincidencias de género
                }
            }
            
            // Verificar coincidencias de plataforma
            for (String platform : preferredPlatforms) {
                if (platformGamesMap.getOrDefault(platform, Collections.emptySet()).contains(gameName)) {
                    score += 1; // Menor peso a coincidencias de plataforma
                }
            }
            
            gameScores.put(gameName, score);
            gameNames.put(gameName, gameName);
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
            "RETURN DISTINCT game.name as gameName";
        
        Result result = connectionManager.executeQuery(query, Values.parameters("userId", userId));
        
        while (result.hasNext()) {
            games.add(result.next().get("gameName").asString());
        }
        
        return games;
    }

    private Value findGameNode(String gameName) {
        Result result = connectionManager.executeQuery(
            "MATCH (game:Videojuego {name: $name}) RETURN game",
            Values.parameters("name", gameName)
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
            "WHERE NAME(game) = $gameName " +
            "RETURN type(r) as relationType, attribute",
            Values.parameters("gameName", gameNode.asNode().id())
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

    public List<Recomendacion> recommendGamesByGenre(String genero, int maxRecommendations) {
    String query = "MATCH (game:Videojuego)-[:BELONGS_TO_GENRE]->(genre:Genre {name: $genero}) " +
                   "RETURN game.titulo as titulo, game.puntajeCritica as puntaje " +
                   "ORDER BY game.puntajeCritica DESC " +
                   "LIMIT $limit";
    
    Result result = connectionManager.executeQuery(query, 
        Values.parameters("genero", genero, "limit", maxRecommendations));
    
    List<Recomendacion> recomendaciones = new ArrayList<>();
    while (result.hasNext()) {
        Record record = result.next();
        String titulo = record.get("titulo").asString();
        double puntaje = record.get("puntaje").asDouble();
        
        recomendaciones.add(new Recomendacion(
            titulo, 
            titulo, 
            (int)(puntaje * 10), // Convertir a escala de 0-100
            Recomendacion.TipoRecomendacion.PERSONAL));
    }
    
    return recomendaciones;
}
}
