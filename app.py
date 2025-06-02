from flask import Flask, render_template, request, jsonify
from flask_cors import CORS
from config import Config
from neo4j_connection import Neo4jConnectionManager
from personal_recommender import PersonalRecommenderService
from collaborative_recomendation import CollaborativeRecommenderService
from utils import handle_errors, setup_logging
import logging

logger = setup_logging()

app = Flask(__name__)
app.config.from_object(Config)

CORS(app, origins=Config.CORS_ORIGINS)

try:
    vgsales_connection = Neo4jConnectionManager(
        Config.VGSALES_URI, 
        Config.VGSALES_USER, 
        Config.VGSALES_PASSWORD
    )
    videogames_connection = Neo4jConnectionManager(
        Config.VIDEOGAMES_URI, 
        Config.VIDEOGAMES_USER, 
        Config.VIDEOGAMES_PASSWORD
    )
    
    personal_recommender = PersonalRecommenderService(vgsales_connection, videogames_connection)
    collaborative_recommender = CollaborativeRecommenderService(vgsales_connection, videogames_connection)
    
    logger.info("Conexiones y servicios inicializados correctamente")
    
except Exception as e:
    logger.error(f"Error inicializando conexiones: {e}")
    raise

@app.route('/')
def index():
    return render_template('game_recommender_index.html')

@app.route('/api/recommend/by-game', methods=['POST'])
@handle_errors
def recommend_by_game():
    """Endpoint para recomendaciones basadas en un juego"""
    data = request.get_json()
    
    if not data:
        return jsonify({
            'success': False,
            'error': 'Se requieren datos JSON'
        }), 400
    
    game_name = data.get('gameName')
    max_recommendations = data.get('maxRecommendations', 10)
    
    if not game_name:
        return jsonify({
            'success': False,
            'error': 'gameName es requerido'
        }), 400
    
    recommendations = personal_recommender.recommend_games_by_game(game_name, max_recommendations)
    
    return jsonify({
        'success': True,
        'recommendations': [rec.to_dict() for rec in recommendations]
    })

@app.route('/api/recommend/by-preferences', methods=['POST'])
@handle_errors
def recommend_by_preferences():
    """Endpoint para recomendaciones basadas en preferencias del usuario"""
    data = request.get_json()
    
    if not data:
        return jsonify({
            'success': False,
            'error': 'Se requieren datos JSON'
        }), 400
    
    user_id = data.get('userId')
    max_recommendations = data.get('maxRecommendations', 10)
    
    if not user_id:
        return jsonify({
            'success': False,
            'error': 'userId es requerido'
        }), 400
    
    recommendations = personal_recommender.recommend_games_by_user_preferences(user_id, max_recommendations)
    
    return jsonify({
        'success': True,
        'recommendations': [rec.to_dict() for rec in recommendations]
    })

@app.route('/api/recommend/by-friends', methods=['POST'])
@handle_errors
def recommend_by_friends():
    """Endpoint para recomendaciones basadas en amigos"""
    data = request.get_json()
    
    if not data:
        return jsonify({
            'success': False,
            'error': 'Se requieren datos JSON'
        }), 400
    
    user_id = data.get('userId')
    max_recommendations = data.get('maxRecommendations', 10)
    
    if not user_id:
        return jsonify({
            'success': False,
            'error': 'userId es requerido'
        }), 400
    
    recommendations = collaborative_recommender.recommend_games_by_friends(user_id, max_recommendations)
    
    return jsonify({
        'success': True,
        'recommendations': [rec.to_dict() for rec in recommendations]
    })

@app.route('/api/recommend/by-similar-users', methods=['POST'])
@handle_errors
def recommend_by_similar_users():
    """Endpoint para recomendaciones basadas en usuarios similares"""
    data = request.get_json()
    
    if not data:
        return jsonify({
            'success': False,
            'error': 'Se requieren datos JSON'
        }), 400
    
    user_id = data.get('userId')
    max_recommendations = data.get('maxRecommendations', 10)
    
    if not user_id:
        return jsonify({
            'success': False,
            'error': 'userId es requerido'
        }), 400
    
    recommendations = collaborative_recommender.recommend_games_by_similar_users(user_id, max_recommendations)
    
    return jsonify({
        'success': True,
        'recommendations': [rec.to_dict() for rec in recommendations]
    })

@app.route('/api/games/search', methods=['GET'])
@handle_errors
def search_games():
    """Endpoint para búsqueda de juegos"""
    query = request.args.get('q', '').strip()
    
    if not query:
        return jsonify({
            'success': False,
            'error': 'Parámetro de búsqueda q es requerido'
        }), 400
    
    if len(query) < 2:
        return jsonify({
            'success': False,
            'error': 'La búsqueda debe tener al menos 2 caracteres'
        }), 400
    
    games = personal_recommender.search_games(query)
    
    return jsonify({
        'success': True,
        'games': games
    })

@app.route('/api/health', methods=['GET'])
def health_check():
    """Endpoint de health check"""
    try:
        vgsales_ok = vgsales_connection.test_connection()
        videogames_ok = videogames_connection.test_connection()
        
        return jsonify({
            'success': True,
            'status': 'healthy',
            'connections': {
                'vgsales': vgsales_ok,
                'videogames': videogames_ok
            }
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'status': 'unhealthy',
            'error': str(e)
        }), 500

@app.errorhandler(404)
def not_found(error):
    return jsonify({
        'success': False,
        'error': 'Endpoint no encontrado'
    }), 404

@app.errorhandler(500)
def internal_error(error):
    return jsonify({
        'success': False,
        'error': 'Error interno del servidor'
    }), 500

if __name__ == '__main__':
    try:
        logger.info("Probando conexiones...")
        vgsales_connection.test_connection()
        videogames_connection.test_connection()
        logger.info("Conexiones a Neo4j establecidas correctamente")
        
        app.run(
            debug=Config.DEBUG,
            host='0.0.0.0',
            port=5000
        )
    except Exception as e:
        logger.error(f"Error al inicializar la aplicación: {e}")
        raise
    finally:
        try:
            vgsales_connection.close()
            videogames_connection.close()
            logger.info("Conexiones cerradas correctamente")
        except:
            pass
