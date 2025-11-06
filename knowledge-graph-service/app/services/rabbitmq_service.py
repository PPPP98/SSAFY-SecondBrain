import pika
import json
import logging
from typing import Callable, Optional

from app.core.config import get_settings

settings = get_settings()

logger = logging.getLogger(__name__)


class RabbitMQService:
    """
    RabbitMQ 서비스 클래스

    RabbitMQ와의 모든 상호작용을 담당합니다.

    주요 메서드:
    - connect(): RabbitMQ 연결
    - declare_exchange_and_queue(): Exchange, Queue, Binding 선언
    - publish_message(): 메시지 발행 (Publisher)
    - consume_messages(): 메시지 수신 (Consumer)
    - close(): 연결 종료

    사용 예시:
    ```
    # 1. 서비스 초기화
    rabbitmq_service = RabbitMQService()

    # 2. 연결
    rabbitmq_service.connect()

    # 3. Exchange/Queue 설정
    rabbitmq_service.declare_exchange_and_queue(
        exchange_name="knowledge_graph_events",
        queue_name="note_creation_queue",
        routing_key="note.created"
    )

    # 4a. 메시지 발행 (Publisher)
    rabbitmq_service.publish_message(
        exchange_name="knowledge_graph_events",
        routing_key="note.created",
        message={"note_id": "...", ...}
    )

    # 4b. 메시지 수신 (Consumer)
    def callback(ch, method, properties, body):
        message = json.loads(body)
        print(f"메시지 수신: {message}")
        ch.basic_ack(delivery_tag=method.delivery_tag)

    rabbitmq_service.consume_messages(
        queue_name="note_creation_queue",
        callback=callback
    )

    # 5. 연결 종료
    rabbitmq_service.close()
    ```
    """

    def __init__(self):
        """
        RabbitMQService 초기화

        변수:
        - connection: pika.BlockingConnection 인스턴스
        - channel: RabbitMQ 채널
        """
        self.connection: Optional[pika.BlockingConnection] = None
        self.channel: Optional[pika.adapters.blocking_connection.BlockingChannel] = None

    def connect(self) -> bool:
        """
        RabbitMQ에 연결

        동작:
        1. config.py의 rabbitmq_url을 사용하여 연결
        2. Channel 생성

        반환:
        - True: 연결 성공
        - False: 연결 실패

        예외 처리:
        - pika.exceptions.AMQPConnectionError: 연결 실패
        - 기타 예외: 예상치 못한 오류
        """
        try:
            logger.debug(f"🔄 RabbitMQ 연결 시도...")

            # RabbitMQ에 연결
            connection = pika.BlockingConnection(
                pika.URLParameters(settings.rabbitmq_url)
            )
            self.connection = connection

            # Channel 생성
            self.channel = connection.channel()

            logger.debug("✅ RabbitMQ 연결 성공")
            return True

        except pika.exceptions.AMQPConnectionError as e:
            logger.error(f"❌ RabbitMQ 연결 실패 (AMQP 오류):")
            logger.error(f"reason : {e}")
            return False

        except Exception as e:
            logger.error(f"❌ 예상치 못한 오류:")
            logger.error(f"   {type(e).__name__}: {e}")
            return False

    def declare_exchange_and_queue(
        self,
        exchange_name: str,
        queue_name: str,
        routing_key: str,
    ) -> bool:
        """
        Exchange, Queue, Binding 선언

        동작 순서:
        1. Topic 타입 Exchange 선언 (없으면 생성, 있으면 skip)
        2. Queue 선언 (없으면 생성, 있으면 skip)
        3. Queue를 Exchange에 바인딩 (routing_key 설정)

        Args:
        - exchange_name: Exchange 이름 (예: "knowledge_graph_events")
        - queue_name: Queue 이름 (예: "note_creation_queue")
        - routing_key: 라우팅 키 (예: "note.created")

        Return:
        - True: 성공
        - False: 실패
        """
        try:
            # 1. Exchange 선언 (Topic 타입, durable=True)
            logger.debug(f"📡 Exchange 선언: {exchange_name}")
            self.channel.exchange_declare(
                exchange=exchange_name,
                exchange_type="topic",  # Topic 타입 (패턴 매칭)
                durable=True,  # 서버 재시작 후에도 유지
            )
            logger.debug(f"✅ Exchange 선언 완료")

            # 2. Queue 선언 (durable=True)
            logger.debug(f"📦 Queue 선언: {queue_name}")
            self.channel.queue_declare(
                queue=queue_name,
                durable=True,  # 서버 재시작 후에도 유지
            )
            logger.debug(f"✅ Queue 선언 완료")

            # 3. Queue를 Exchange에 바인딩
            logger.debug(f"🔗 Binding 설정: {queue_name} ← {exchange_name}")
            self.channel.queue_bind(
                queue=queue_name,
                exchange=exchange_name,
                routing_key=routing_key,
            )
            logger.debug(f"✅ Binding 완료")
            logger.debug(f"   Exchange: {exchange_name}")
            logger.debug(f"   Queue: {queue_name}")
            logger.debug(f"   Routing Key: {routing_key}")

            return True

        except Exception as e:
            logger.error(f"❌ Exchange/Queue 설정 실패:")
            logger.error(f"   {type(e).__name__}: {e}")
            return False

    def publish_message(
        self,
        exchange_name: str,
        routing_key: str,
        message: dict,
    ) -> bool:
        """
        메시지를 Exchange로 발행

        발행 흐름:
        1. 메시지를 JSON으로 직렬화
        2. Exchange와 routing_key를 지정
        3. Delivery mode를 Persistent로 설정 (메시지 지속성)

        Args:
        - exchange_name: 메시지를 보낼 Exchange
        - routing_key: 라우팅 키 (예: "note.created")
        - message: 메시지 내용 (dict)

        Return:
        - True: 발행 성공
        - False: 발행 실패

        사용 예시:
        ```
        success = rabbitmq_service.publish_message(
            exchange_name="knowledge_graph_events",
            routing_key="note.created",
            message={
                "event_type": "note.created",
                "note_id": "550e8400-e29b-41d4-a716-446655440000",
                "user_id": "user-123",
                "title": "Python 기초",
                "content": "..."
            }
        )
        ```
        """
        try:
            # 메시지를 JSON으로 직렬화
            message_json = json.dumps(message)

            logger.debug(f"📤 메시지 발행:")
            logger.debug(f"   Exchange: {exchange_name}")
            logger.debug(f"   Routing Key: {routing_key}")
            logger.debug(f"   Message: {message_json[:80]}...")

            # Exchange로 메시지 발행
            self.channel.basic_publish(
                exchange=exchange_name,
                routing_key=routing_key,
                body=message_json,
                properties=pika.BasicProperties(
                    delivery_mode=pika.DeliveryMode.Persistent,  # 지속성 설정
                    content_type="application/json",
                ),
            )

            logger.info(f"✅ 메시지 발행 완료")
            return True

        except Exception as e:
            logger.error(f"❌ 메시지 발행 실패:")
            logger.error(f"   {type(e).__name__}: {e}")
            return False

    def consume_messages(
        self,
        queue_name: str,
        callback: Callable,
    ) -> None:
        """
        Queue에서 메시지를 수신하고 처리

        동작:
        1. QoS (Quality of Service) 설정: 한 번에 1개의 메시지만 처리
        2. 콜백 함수 등록
        3. 메시지 수신 대기 (무한 루프)

        Args:
        - queue_name: 수신할 Queue 이름
        - callback: 메시지 수신 시 호출될 함수

        """
        try:
            # QoS 설정: 한 번에 1개의 메시지만 처리
            self.channel.basic_qos(prefetch_count=1)
            logger.debug(f"⚙️  QoS 설정: prefetch_count=1")

            # 콜백 함수 등록
            self.channel.basic_consume(
                queue=queue_name,
                on_message_callback=callback,
                auto_ack=False,  # 수동 ack
            )

            logger.debug(f"🔄 {queue_name}에서 메시지 수신 대기...")
            logger.debug(f"Ctrl+C를 눌러 중지하세요\n")

            # 메시지 수신 시작 (무한 루프)
            self.channel.start_consuming()

        except KeyboardInterrupt:
            logger.debug("🛑 메시지 수신 중지 (Ctrl+C)")
            self.close()

        except Exception as e:
            logger.error(f"❌ 메시지 수신 중 오류:")
            logger.error(f"   {type(e).__name__}: {e}")
            self.close()

    def close(self) -> None:
        """
        RabbitMQ 연결 종료

        사용 시점:
        - 애플리케이션 종료 시
        - 오류 발생으로 인한 정리 시
        """
        try:
            if self.connection and not self.connection.is_closed:
                self.connection.close()
                logger.info("✅ RabbitMQ 연결 종료")

        except Exception as e:
            logger.error(f"❌ 연결 종료 중 오류:")
            logger.error(f"   {type(e).__name__}: {e}")

# Singleton
rabbitmq_service = RabbitMQService()
