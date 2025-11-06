# scripts/test_rabbitmq_connection.py

"""
RabbitMQ 연결 테스트 스크립트
EC2의 RabbitMQ 서버에 정상 연결되는지 확인합니다.
"""

import sys
import logging
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

import pika
from app.core.config import get_settings

settings = get_settings()


# 로깅 설정
logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)


def test_rabbitmq_connection():
    """RabbitMQ 연결 테스트"""

    print("\n" + "=" * 70)
    print("🧪 RabbitMQ 연결 테스트")
    print("=" * 70)

    # 연결 정보 출력
    logger.info(f"\n📡 RabbitMQ 연결 정보:")
    logger.info(f"   Host: {settings.rabbitmq_host}")
    logger.info(f"   Port: {settings.rabbitmq_port}")
    logger.info(f"   User: {settings.rabbitmq_user}")
    logger.info(f"   VHost: {settings.rabbitmq_vhost}")
    logger.info(f"   🔗 URL: {settings.rabbitmq_url}")

    try:
        # RabbitMQ 연결 시도
        logger.info(f"\n🔄 연결 시도 중...")

        connection = pika.BlockingConnection(pika.URLParameters(settings.rabbitmq_url))

        logger.info("✅ RabbitMQ 연결 성공!")

        # Channel 생성
        channel = connection.channel()
        logger.info(f"✅ Channel 생성 성공!")

        # 연결 정보
        logger.info(f"\n📊 연결 상태:")
        logger.info(f"   Connection OK: {not connection.is_closed}")
        logger.info(f"   Channel: OK")

        # 연결 종료
        connection.close()
        logger.info(f"\n✅ 연결 정상 종료")

        print("\n" + "=" * 70)
        print("✅ 테스트 통과: RabbitMQ 연결 성공!")
        print("=" * 70 + "\n")

        return True

    except pika.exceptions.AMQPConnectionError as e:
        logger.error(f"\n❌ 연결 실패 (AMQP 오류):")
        logger.error(f"   {e}")
        logger.error(f"\n💡 확인 사항:")
        logger.error(f"   1. EC2 RabbitMQ 서버가 실행 중인가?")
        logger.error(f"   2. 올바른 HOST/PORT를 입력했는가?")
        logger.error(f"   3. USER/PASSWORD가 정확한가?")
        logger.error(f"   4. 보안 그룹에서 5672 포트가 열려있는가?")
        logger.error(f"\n   설정된 값:")
        logger.error(f"   URL: {settings.rabbitmq_url}")

        print("\n" + "=" * 70)
        print("❌ 테스트 실패")
        print("=" * 70 + "\n")
        return False

    except Exception as e:
        logger.error(f"\n❌ 예상치 못한 오류:")
        logger.error(f"   {type(e).__name__}: {e}")

        print("\n" + "=" * 70)
        print("❌ 테스트 실패")
        print("=" * 70 + "\n")
        return False


if __name__ == "__main__":
    success = test_rabbitmq_connection()
    sys.exit(0 if success else 1)
