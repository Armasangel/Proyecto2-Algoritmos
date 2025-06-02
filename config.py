import os
from dotenv import load_dotenv

load_dotenv()

class Config:

    VGSALES_URI = os.getenv('VGSALES_URI', 'neo4j+s://6bc72245.databases.neo4j.io')
    VGSALES_USER = os.getenv('VGSALES_USER', 'neo4j')
    VGSALES_PASSWORD = os.getenv('VGSALES_PASSWORD', '20z23qOU77VA4J4Em7y5D-0uMFc6f87tB5Q8upJSTsk')

    VIDEOGAMES_URI = os.getenv('VIDEOGAMES_URI', 'neo4j+s://6bc72245.databases.neo4j.io')
    VIDEOGAMES_USER = os.getenv('VIDEOGAMES_USER', 'neo4j')
    VIDEOGAMES_PASSWORD = os.getenv('VIDEOGAMES_PASSWORD', '20z23qOU77VA4J4Em7y5D-0uMFc6f87tB5Q8upJSTsk')

    SECRET_KEY = os.getenv('SECRET_KEY', 'dev-secret-key')
    DEBUG = os.getenv('FLASK_DEBUG', 'True').lower() == 'true'

    CORS_ORIGINS = os.getenv('CORS_ORIGINS', '*').split(',')
