from typing import List, Dict
from collections import defaultdict
import logging
from models import Recommendation, RecommendationType
from neo4j_connection import Neo4jConnectionManager

logger = logging.getLogger(__name__)

class CollaborativeRecommenderService:
    def __init__(self, vgsales_connection: Neo4jConnectionManager, videogames_connection: Neo4jConnectionManager):
        self.vgsales_conn = vgsales_connection
        self.videogames_conn = videogames_connection
    
    def recommend_games_by_friends(self, user_id: str, max_recommendations: int) -> List[Recommendation]:
        """Recomienda juegos basándose en los gustos de amigos"""
        try:
            game_scores = defaultdict(int)
            game_names = {}
            
            direct_friends_query = """
            MATCH (user:User {id: $userId})-[:FRIENDS_WITH]->(friend:User)-[:LIKES]->(game:VideoGame)
            WHERE NOT (user)-[:PLAYED|LIKES]->(game)
            RETURN game.Name as gameName, count(friend) as friendCount
            ORDER BY friendCount DESC
            """
            
            vgsales_friends = self.vgsales_conn.execute_query(direct_friends_query, {'userId': user_id})
            
            for record in vgsales_friends:
                game_name = record['gameName']
                friend_count = record['friendCount']
                game_scores[game_name] = friend_count
                game_names[game_name] = game_name

            if len(game_scores) < max_recommendations:
                extended_query = """
                MATCH (user:User {id: $userId})-[:FRIENDS_WITH]->(:User)-[:FRIENDS_WITH]->(friendOfFriend:User)
                WHERE NOT (user)-[:FRIENDS_WITH]->(friendOfFriend) AND NOT user = friendOfFriend
                WITH DISTINCT friendOfFriend
                MATCH (friendOfFriend)-[:LIKES]->(game:VideoGame)
                WHERE NOT (user)-[:PLAYED|LIKES]->(game)
                RETURN game.Name as gameName, count(friendOfFriend) as fofCount
                ORDER BY fofCount DESC
                """
                
                extended_friends = self.vgsales_conn.execute_query(extended_query, {'userId': user_id})
                
                for record in extended_friends:
                    if len(game_scores) >= max_recommendations * 2:
                        break
                    
                    game_name = record['gameName']
                    fof_count = record['fofCount']
                    
                    game_scores[game_name] = game_scores.get(game_name, 0) + fof_count // 2
                    game_names[game_name] = game_name
            
            recommendations = []
            sorted_games = sorted(game_scores.items(), key=lambda x: x[1], reverse=True)
            
            for game_name, score in sorted_games[:max_recommendations]:
                recommendations.append(Recommendation(
                    game_id=game_name,
                    game_name=game_names[game_name],
                    score=score,
                    recommendation_type=RecommendationType.COLLABORATIVE
                ))
            
            logger.info(f"Generadas {len(recommendations)} recomendaciones por amigos para usuario '{user_id}'")
            return recommendations
            
        except Exception as e:
            logger.error(f"Error generando recomendaciones por amigos: {e}")
            raise
    
    def recommend_games_by_similar_users(self, user_id: str, max_recommendations: int) -> List[Recommendation]:
        """Recomienda juegos basándose en usuarios similares"""
        try:
            game_scores = defaultdict(int)
            game_names = {}
            
            similar_users_query = """
            MATCH (user:User {id: $userId})-[:LIKES]->(game:VideoGame)<-[:LIKES]-(otherUser:User)
            WHERE user <> otherUser
            WITH otherUser, count(game) AS commonGames
            WHERE commonGames > 0
            MATCH (otherUser)-[:LIKES]->(rec:VideoGame)
            WHERE NOT (user)-[:PLAYED|LIKES]->(rec)
            RETURN rec.Name AS gameName, sum(commonGames) AS score
            ORDER BY score DESC
            """
            
            similar_users = self.vgsales_conn.execute_query(similar_users_query, {'userId': user_id})
            
            for record in similar_users:
                game_name = record['gameName']
                score = record['score']
                game_scores[game_name] = score
                game_names[game_name] = game_name
            
            recommendations = []
            sorted_games = sorted(game_scores.items(), key=lambda x: x[1], reverse=True)
            
            for game_name, score in sorted_games[:max_recommendations]:
                recommendations.append(Recommendation(
                    game_id=game_name,
                    game_name=game_names[game_name],
                    score=score,
                    recommendation_type=RecommendationType.COLLABORATIVE
                ))
            
            logger.info(f"Generadas {len(recommendations)} recomendaciones por usuarios similares para usuario '{user_id}'")
            return recommendations
            
        except Exception as e:
            logger.error(f"Error generando recomendaciones por usuarios similares: {e}")
            raise
