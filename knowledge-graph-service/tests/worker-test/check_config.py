# scripts/check_config.py

"""설정 확인 스크립트"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from app.core.config import get_settings

settings = get_settings()

print("\n" + "="*70)
print("⚙️  애플리케이션 설정 확인")
print("="*70)

print("\n🗄️  Neo4j 설정:")
print(f"   URI: {settings.neo4j_uri}")
print(f"   USER: {settings.neo4j_user}")

print("\n🤖 OpenAI 설정:")
print(f"   API Key: {settings.openai_api_key[:10]}..." if settings.openai_api_key else "   ❌ 미설정")
print(f"   Model: {settings.openai_model}")

print("\n🐰 RabbitMQ 설정:")
print(f"   HOST: {settings.rabbitmq_host}")
print(f"   PORT: {settings.rabbitmq_port}")
print(f"   USER: {settings.rabbitmq_user}")
print(f"   VHOST: {settings.rabbitmq_vhost}")
print(f"   🔗 생성된 URL: {settings.rabbitmq_url}")

print("\n⚙️  애플리케이션 설정:")
print(f"   SIMILARITY_THRESHOLD: {settings.similarity_threshold}")
print(f"   MAX_RELATIONSHIPS: {settings.max_relationships}")

print("\n" + "="*70 + "\n")
