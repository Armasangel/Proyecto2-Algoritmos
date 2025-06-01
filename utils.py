import logging
from functools import wraps
from flask import jsonify

def setup_logging():
    """Configura el sistema de logging"""
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    return logging.getLogger(__name__)

def handle_errors(f):
    """Decorador para manejo de errores en rutas Flask"""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        try:
            return f(*args, **kwargs)
        except Exception as e:
            logging.error(f"Error en {f.__name__}: {str(e)}")
            return jsonify({
                'success': False,
                'error': str(e)
            }), 500
    return decorated_function

def validate_request_data(required_fields):
    """Decorador para validar datos de request"""
    def decorator(f):
        @wraps(f)
        def decorated_function(*args, **kwargs):
            from flask import request
            
            if not request.is_json:
                return jsonify({
                    'success': False,
                    'error': 'Content-Type debe ser application/json'
                }), 400
            
            data = request.get_json()
            
            for field in required_fields:
                if field not in data or not data[field]:
                    return jsonify({
                        'success': False,
                        'error': f'Campo requerido: {field}'
                    }), 400
            
            return f(*args, **kwargs)
        return decorated_function
    return decorator
