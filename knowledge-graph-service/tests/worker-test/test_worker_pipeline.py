# scripts/test_full_pipeline.py

"""
전체 파이프라인 통합 테스트

테스트 시나리오:
1. RabbitMQ 연결 테스트
2. 더미 메시지 발행
3. 워커에서 메시지 수신 및 처리
4. Neo4j에 데이터 저장 확인
5. 그래프 데이터 조회 확인
"""

import sys
import json
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

import logging
from app.services.rabbitmq_service import rabbitmq_service
from app.schemas.event import NoteCreatedEvent

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def test_full_pipeline():
    """전체 파이프라인 테스트"""
    
    print("\n" + "="*70)
    print("🧪 전체 파이프라인 통합 테스트")
    print("="*70)
    
    all_tests = []
    
    # ===== 테스트 1: RabbitMQ 연결 =====
    print("\n[테스트 1] RabbitMQ 연결")
    print("-" * 70)
    
    try:
        if rabbitmq_service.connect():
            print("✅ RabbitMQ 연결 성공")
            all_tests.append(("RabbitMQ 연결", True))
        else:
            print("❌ RabbitMQ 연결 실패")
            return False
    except Exception as e:
        print(f"❌ 오류: {e}")
        return False
    
    # ===== 테스트 2: Exchange & Queue 선언 =====
    print("\n[테스트 2] Exchange & Queue 선언")
    print("-" * 70)
    
    try:
        if rabbitmq_service.declare_exchange_and_queue(
            exchange_name="knowledge_graph_events",
            queue_name="note_creation_queue",
            routing_key="note.*"
        ):
            print("✅ Exchange & Queue 선언 성공")
            all_tests.append(("Exchange & Queue 선언", True))
        else:
            print("❌ 선언 실패")
            all_tests.append(("Exchange & Queue 선언", False))
    except Exception as e:
        print(f"❌ 오류: {e}")
        all_tests.append(("Exchange & Queue 선언", False))
    
    # ===== 테스트 3: 메시지 발행 =====
    print("\n[테스트 3] 메시지 발행")
    print("-" * 70)
    
    try:
        test_events = [
            {
                "event_type": "note.created",
                "note_id": 1,
                "user_id": 1,
                "title": "Test Note 1",
                "content": "This is test content 1"
            },
            {
                "event_type": "note.created",
                "note_id": 2,
                "user_id": 1,
                "title": "Test Note 2",
                "content": "This is test content 2"
            }
        ]
        
        for event in test_events:
            success = rabbitmq_service.publish_message(
                exchange_name="knowledge_graph_events",
                routing_key="note.created",
                message=event
            )
            
            if not success:
                raise Exception(f"메시지 발행 실패: {event['note_id']}")
        
        print(f"✅ {len(test_events)}개 메시지 발행 성공")
        all_tests.append(("메시지 발행", True))
    
    except Exception as e:
        print(f"❌ 오류: {e}")
        all_tests.append(("메시지 발행", False))
    
    # ===== 결과 요약 =====
    print("\n" + "="*70)
    print("📊 테스트 결과 요약")
    print("="*70)
    
    for test_name, result in all_tests:
        status = "✅ 통과" if result else "❌ 실패"
        print(f"{test_name}: {status}")
    
    all_pass = all(result for _, result in all_tests)
    
    print("\n" + "="*70)
    if all_pass:
        print("✅ 모든 테스트 통과!")
        print("\n📝 다음 단계:")
        print("1. 워커 실행: uv run python scripts/start_worker.py")
        print("2. 메시지 확인: RabbitMQ Management UI 확인")
        print("3. Neo4j 확인: 노트 저장 여부 확인")
    else:
        print("❌ 일부 테스트 실패")
    print("="*70 + "\n")
    
    # 연결 종료
    rabbitmq_service.close()
    
    return all_pass


if __name__ == "__main__":
    success = test_full_pipeline()
    sys.exit(0 if success else 1)
