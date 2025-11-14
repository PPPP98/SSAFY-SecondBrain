from pydantic_settings import BaseSettings, SettingsConfigDict
from functools import lru_cache
from typing import Optional


class Settings(BaseSettings):
    """애플리케이션 환경 변수 설정"""

    # Neo4j 설정
    neo4j_uri: str
    neo4j_user: str
    neo4j_password: str

    # OpenAI 설정
    openai_api_key: str
    openai_base_url: Optional[str] = None
    openai_model: str

    # RabbitMQ 설정
    rabbitmq_host: str
    rabbitmq_port: int
    rabbitmq_user: str
    rabbitmq_password: str
    rabbitmq_vhost: str

    @property
    def rabbitmq_url(self) -> str:
        """RabbitMQ URL 생성"""
        return (
            f"amqp://{self.rabbitmq_user}:{self.rabbitmq_password}"
            f"@{self.rabbitmq_host}:{self.rabbitmq_port}/{self.rabbitmq_vhost}"
        )

    # 애플리케이션 설정
    similarity_threshold: float
    max_relationships: int

    # Summarize_Agent 설정
    summarize_model: str
    summarize_temperature: float

    # Search_Agent 설정
    search_agent_model: str
    search_agent_temperature: float
    top_k: int
    search_limit: int

    # external_service 설정
    secondbrain_api_url: str

    # Pydantic 설정
    model_config = SettingsConfigDict(
        env_file=".env",
        case_sensitive=False,
        extra="ignore",
    )


@lru_cache()
def get_settings() -> Settings:
    return Settings()
