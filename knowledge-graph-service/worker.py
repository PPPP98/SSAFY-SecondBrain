"""
워커 시작 스크립트

RabbitMQ 메시지를 수신하고 처리하는 워커를 시작합니다.

사용법:
    uv run python worker.py

기능:
- RabbitMQ 연결
- Exchange & Queue 선언
- 메시지 수신 및 처리
- 로그 기록

종료:
- Ctrl+C: 정상 종료
"""
import logging
from pathlib import Path

from app.workers.note_consumer import start_consumer

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[
        logging.StreamHandler(),
    ],
)

logger = logging.getLogger(__name__)

if __name__ == "__main__":
    logger.info("워커 시작 스크립트 실행 중...")
    logger.info("Ctrl+C를 눌러 종료할 수 있습니다.")
    start_consumer()