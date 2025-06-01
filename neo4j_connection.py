from neo4j import GraphDatabase
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class Neo4jConnectionManager:
    def __init__(self, uri, username, password):
        self.uri = uri
        self.username = username
        self.password = password
        self.driver = None
        self._connect()
    
    def _connect(self):
        try:
            self.driver = GraphDatabase.driver(self.uri, auth=(self.username, self.password))
            logger.info(f"Conexión establecida con Neo4j en {self.uri}")
        except Exception as e:
            logger.error(f"Error conectando a Neo4j: {e}")
            raise
    
    def test_connection(self):
        try:
            with self.driver.session() as session:
                result = session.run("RETURN '¡Hola desde Neo4j!' as greeting")
                greeting = result.single()["greeting"]
                logger.info(f"Prueba de conexión exitosa: {greeting}")
                return True
        except Exception as e:
            logger.error(f"Error en prueba de conexión: {e}")
            return False
    
    def execute_query(self, query, parameters=None):
        if parameters is None:
            parameters = {}
        
        try:
            with self.driver.session() as session:
                result = session.run(query, parameters)
                return result.data()
        except Exception as e:
            logger.error(f"Error ejecutando query: {e}")
            raise
    
    def execute_write_transaction(self, query, parameters=None):
        if parameters is None:
            parameters = {}
        
        try:
            with self.driver.session() as session:
                result = session.write_transaction(lambda tx: tx.run(query, parameters))
                return result
        except Exception as e:
            logger.error(f"Error ejecutando transacción de escritura: {e}")
            raise
    
    def close(self):
        if self.driver:
            self.driver.close()
            logger.info("Conexión Neo4j cerrada")
