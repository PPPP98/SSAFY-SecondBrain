from pydantic_settings import BaseSettings, SettingsConfigDict
from functools import lru_cache

class Settings(BaseSettings):
    """애플리케이션 설정"""
    
    # Neo4j 설정
    neo4j_uri: str
    neo4j_user: str
    neo4j_password: str
    
    # OpenAI 설정
    openai_api_key: str
    openai_model: str
    
    # 애플리케이션 설정
    similarity_threshold: float
    max_relationships: int
    
    # Pydantic v2 설정
    model_config = SettingsConfigDict(
        env_file=".env",
        case_sensitive=False,
        extra="ignore"
    )

@lru_cache()
def get_settings() -> Settings:
    return Settings()