from typing import List, Dict, Set
from collections import defaultdict
import logging
from models import Recommendation, RecommendationType, VideoGame
from neo4j_connection import Neo4jConnectionManager

logger = logging.getLogger(__name__)

class PersonalRecommenderService:
    def __init__(self, vgsales_connection: Neo4jConnectionManager, videogames_connection: Neo4jConnectionManager):
        self.vgsales_conn = vgsales_connection
        self.videogames_conn = videogames_connection
        self.genre_games_map = defaultdict(set)
        self.platform_games_map = defaultdict(set)
        self.developer_games_map = defaultdict(set)
        
        self._initialize_category_maps()
    
    def _initialize_category_maps(self):
        """Inicializa los mapas de categorías con datos de ambas bases de datos"""
        try:
            vgsales_genres = self.vgsales_conn.execute_query(
                "MATCH (game:VideoGame)-[:BELONGS_TO_GENRE]->(genre:Genre) "
                "RETURN game.Name as gameName, genre.name as genreName"
            )
            
            for record in vgsales_genres:
                game_name = record['gameName']
                genre_name = record['genreName']
                self.genre_games_map[genre_name].add(game_name)
            
            vgsales_platforms = self.vgsales_conn.execute_query(
                "MATCH (game:VideoGame)-[:AVAILABLE_ON]->(platform:Platform) "
                "RETURN game.Name as gameName, platform.name as platformName"
            )
            
            for record in vgsales_platforms:
                game_name = record['gameName']
                platform_name = record['platformName']
                self.platform_games_map[platform_name].add(game_name)
            
            videogames_devs = self.videogames_conn.execute_query(
                "MATCH (game:Videojuego)-[:DEVELOPED_BY]->(developer:Developer) "
                "RETURN game.nombre as gameName, developer.name as developerName"
            )
            
            for record in videogames_devs:
                game_name = record['gameName']
                developer_name = record['developerName']
                self.developer_games_map[developer_name].add(game_name)
            
            logger.info("Mapas de categorías inicializados correctamente")
            
        except Exception as e:
            logger.error(f"Error inicializando mapas de categorías: {e}")
            raise
    
    def recommend_games_by_game(self, base_game_name: str, max_recommendations: int) -> List[Recommendation]:
        """
        Recomienda juegos basándose en un juego base.
        Busca el nodo del videojuego, obtiene sus atributos relacionados,
        cuenta atributos compartidos y asigna puntuaciones.
        """
        try:
            base_game = self._find_game_node(base_game_name)
            if not base_game:
                logger.warning(f"Juego base '{base_game_name}' no encontrado")
                return []
            
            base_attributes = self._get_game_attributes(base_game_name)
            
            game_scores = defaultdict(int)
            game_names = {}
            
            for attribute_type, attribute_value in base_attributes.items():
                games_with_attribute = self._get_games_with_attribute(attribute_type, attribute_value)
                
                for game_name in games_with_attribute:
                    if game_name != base_game_name:
                        game_scores[game_name] += 1
                        game_names[game_name] = game_name
            
            recommendations = []
            sorted_games = sorted(game_scores.items(), key=lambda x: x[1], reverse=True)
            
            for game_name, score in sorted_games[:max_recommendations]:
                recommendations.append(Recommendation(
                    game_id=game_name,
                    game_name=game_names[game_name],
                    score=score,
                    recommendation_type=RecommendationType.PERSONAL
                ))
            
            logger.info(f"Generadas {len(recommendations)} recomendaciones para '{base_game_name}'")
            return recommendations
            
        except Exception as e:
            logger.error(f"Error generando recomendaciones por juego: {e}")
            raise
    
    def recommend_games_by_user_preferences(self, user_id: str, max_recommendations: int) -> List[Recommendation]:
        """Recomienda juegos basándose en las preferencias del usuario"""
        try:
            preferred_genres = self._get_user_preferred_genres(user_id)
            
            preferred_platforms = self._get_user_preferred_platforms(user_id)
            
            user_games = self._get_user_games(user_id)
            
            game_scores = defaultdict(int)
            game_names = {}
            
            for genre in preferred_genres:
                for game_name in self.genre_games_map.get(genre, set()):
                    if game_name not in user_games:
                        game_scores[game_name] += 2  
                        game_names[game_name] = game_name
            
            for platform in preferred_platforms:
                for game_name in self.platform_games_map.get(platform, set()):
                    if game_name not in user_games:
                        game_scores[game_name] += 1  
                        game_names[game_name] = game_name
            
            recommendations = []
            sorted_games = sorted(game_scores.items(), key=lambda x: x[1], reverse=True)
            
            for game_name, score in sorted_games[:max_recommendations]:
                recommendations.append(Recommendation(
                    game_id=game_name,
                    game_name=game_names[game_name],
                    score=score,
                    recommendation_type=RecommendationType.PERSONAL
                ))
            
            logger.info(f"Generadas {len(recommendations)} recomendaciones para usuario '{user_id}'")
            return recommendations
            
        except Exception as e:
            logger.error(f"Error generando recomendaciones por preferencias: {e}")
            raise
    
    def search_games(self, query: str) -> List[Dict]:
        """Busca juegos por nombre en ambas bases de datos"""
        try:
            games = []
            
            vgsales_games = self.vgsales_conn.execute_query(
                "MATCH (game:VideoGame) "
                "WHERE toLower(game.Name) CONTAINS toLower($query) "
                "RETURN game.Name as name "
                "LIMIT 10",
                {'query': query}
            )
            
            for record in vgsales_games:
                games.append({'name': record['name'], 'source': 'vgsales'})
            
            videogames_games = self.videogames_conn.execute_query(
                "MATCH (game:Videojuego) "
                "WHERE toLower(game.nombre) CONTAINS toLower($query) "
                "RETURN game.nombre as name "
                "LIMIT 10",
                {'query': query}
            )
            
            for record in videogames_games:
                games.append({'name': record['name'], 'source': 'videogames'})
            
            return games
            
        except Exception as e:
            logger.error(f"Error buscando juegos: {e}")
            raise
    
    def _find_game_node(self, game_name: str) -> Dict:
        """Busca un juego en ambas bases de datos"""
        vgsales_result = self.vgsales_conn.execute_query(
            "MATCH (game:VideoGame {Name: $name}) RETURN game",
            {'name': game_name}
        )
        
        if vgsales_result:
            return vgsales_result[0]['game']
        
        videogames_result = self.videogames_conn.execute_query(
            "MATCH (game:Videojuego {nombre: $name}) RETURN game",
            {'name': game_name}
        )
        
        if videogames_result:
            return videogames_result[0]['game']
        
        return None
    
    def _get_game_attributes(self, game_name: str) -> Dict[str, str]:
        """Obtiene todos los atributos relacionados a un videojuego"""
        attributes = {}
        
        vgsales_attrs = self.vgsales_conn.execute_query(
            "MATCH (game:VideoGame {Name: $name})-[r]-(attr) "
            "RETURN type(r) as relationType, labels(attr) as labels, attr",
            {'name': game_name}
        )
        
        for record in vgsales_attrs:
            rel_type = record['relationType']
            labels = record['labels']
            attr = record['attr']
            
            if 'Genre' in labels:
                attributes['genre'] = attr.get('name', '')
            elif 'Platform' in labels:
                attributes['platform'] = attr.get('name', '')
        
        videogames_attrs = self.videogames_conn.execute_query(
            "MATCH (game:Videojuego {nombre: $name})-[r]-(attr) "
            "RETURN type(r) as relationType, labels(attr) as labels, attr",
            {'name': game_name}
        )
        
        for record in videogames_attrs:
            rel_type = record['relationType']
            labels = record['labels']
            attr = record['attr']
            
            if 'Developer' in labels:
                attributes['developer'] = attr.get('name', '')
        
        return attributes
    
    def _get_games_with_attribute(self, attribute_type: str, attribute_value: str) -> Set[str]:
        """Obtiene juegos que comparten un atributo específico"""
        if attribute_type == 'genre':
            return self.genre_games_map.get(attribute_value, set())
        elif attribute_type == 'platform':
            return self.platform_games_map.get(attribute_value, set())
        elif attribute_type == 'developer':
            return self.developer_games_map.get(attribute_value, set())
        
        return set()
    
    def _get_user_preferred_genres(self, user_id: str) -> Set[str]:
        """Obtiene géneros preferidos del usuario"""
        try:

            vgsales_genres = self.vgsales_conn.execute_query(
                "MATCH (user:User {id: $userId})-[:LIKES]->(game:VideoGame)-[:BELONGS_TO_GENRE]->(genre:Genre) "
                "RETURN DISTINCT genre.name as genreName",
                {'userId': user_id}
            )
            
            videogames_genres = self.videogames_conn.execute_query(
                "MATCH (user:User {id: $userId})-[:LIKES]->(game:Videojuego)-[:BELONGS_TO_GENRE]->(genre:Genre) "
                "RETURN DISTINCT genre.name as genreName",
                {'userId': user_id}
            )
            
            genres = set()
            for record in vgsales_genres + videogames_genres:
                genres.add(record['genreName'])
            
            return genres
            
        except Exception as e:
            logger.error(f"Error obteniendo géneros preferidos: {e}")
            return set()
    
    def _get_user_preferred_platforms(self, user_id: str) -> Set[str]:
        """Obtiene plataformas preferidas del usuario"""
        try:
            vgsales_platforms = self.vgsales_conn.execute_query(
                "MATCH (user:User {id: $userId})-[:LIKES]->(game:VideoGame)-[:AVAILABLE_ON]->(platform:Platform) "
                "RETURN DISTINCT platform.name as platformName",
                {'userId': user_id}
            )
            
            platforms = set()
            for record in vgsales_platforms:
                platforms.add(record['platformName'])
            
            return platforms
            
        except Exception as e:
            logger.error(f"Error obteniendo plataformas preferidas: {e}")
            return set()
    
    def _get_user_games(self, user_id: str) -> Set[str]:
        """Obtiene juegos que el usuario ha jugado o le han gustado"""
        try:
            vgsales_games = self.vgsales_conn.execute_query(
                "MATCH (user:User {id: $userId})-[:PLAYED|LIKES]->(game:VideoGame) "
                "RETURN DISTINCT game.Name as gameName",
                {'userId': user_id}
            )
            
            videogames_games = self.videogames_conn.execute_query(
                "MATCH (user:User {id: $userId})-[:PLAYED|LIKES]->(game:Videojuego) "
                "RETURN DISTINCT game.nombre as gameName",
                {'userId': user_id}
            )
            
            games = set()
            for record in vgsales_games + videogames_games:
                games.add(record['gameName'])
            
            return games
            
        except Exception as e:
            logger.error(f"Error obteniendo juegos del usuario: {e}")
            return set()
