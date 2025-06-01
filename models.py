from enum import Enum
from typing import List, Optional
from dataclasses import dataclass

class RecommendationType(Enum):
    PERSONAL = "PERSONAL"
    COLLABORATIVE = "COLLABORATIVE"

@dataclass
class Recommendation:
    game_id: str
    game_name: str
    score: int
    recommendation_type: RecommendationType
    
    def to_dict(self):
        return {
            'gameId': self.game_id,
            'gameName': self.game_name,
            'score': self.score,
            'type': self.recommendation_type.value
        }

@dataclass
class VideoGame:
    id: str
    name: str
    genre: Optional[str] = None
    platform: Optional[str] = None
    developer: Optional[str] = None
    year: Optional[int] = None
    rating: Optional[float] = None
    
    def to_dict(self):
        return {
            'id': self.id,
            'name': self.name,
            'genre': self.genre,
            'platform': self.platform,
            'developer': self.developer,
            'year': self.year,
            'rating': self.rating
        }

@dataclass
class User:
    id: str
    name: str
    friends: List[str]
    played_games: List[str]
    liked_games: List[str]
    
    def to_dict(self):
        return {
            'id': self.id,
            'name': self.name,
            'friends': self.friends,
            'playedGames': self.played_games,
            'likedGames': self.liked_games
        }
