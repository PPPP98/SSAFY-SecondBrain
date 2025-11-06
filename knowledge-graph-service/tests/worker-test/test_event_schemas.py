# scripts/test_event_schemas.py

"""
이벤트 스키마 검증 테스트

이 테스트 스크립트는 app/schemas/event.py의 스키마 모델들을 검증합니다.

테스트 항목:
1. NoteCreatedEvent: 노트 생성 이벤트 검증
2. NoteUpdatedEvent: 노트 수정 이벤트 검증 (Optional 필드)
3. NoteDeletedEvent: 노트 삭제 이벤트 검증
4. EventType Enum: 이벤트 타입 열거형 검증
5. JSON 직렬화/역직렬화: JSON ↔ Python 객체 변환
6. 유효성 검사: 필수 필드 검증 및 타입 체크
"""

import sys
import json
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from app.schemas.event import (
    EventType,
    NoteCreatedEvent,
    NoteUpdatedEvent,
    NoteDeletedEvent
)


def test_event_schemas():
    """이벤트 스키마 검증 테스트"""
    
    print("\n" + "="*70)
    print("🧪 이벤트 스키마 검증 테스트")
    print("="*70)
    
    all_tests = []
    
    # ===== 테스트 1: NoteCreatedEvent 생성 =====
    print("\n[테스트 1] NoteCreatedEvent 생성")
    print("-" * 70)
    
    try:
        event1 = NoteCreatedEvent(
            note_id="550e8400-e29b-41d4-a716-446655440000",
            user_id="user-123",
            title="Python 기초",
            content="Python은 읽기 쉬운 프로그래밍 언어입니다."
        )
        
        print("✅ 이벤트 생성 성공")
        print(f"   Event Type: {event1.event_type}")
        print(f"   Note ID: {event1.note_id[:8]}...")
        print(f"   User ID: {event1.user_id}")
        print(f"   Title: {event1.title}")
        print(f"   Content: {event1.content[:30]}...")
        
        test1_pass = True
        all_tests.append(("NoteCreatedEvent 생성", test1_pass))
    
    except Exception as e:
        print(f"❌ 오류: {e}")
        test1_pass = False
        all_tests.append(("NoteCreatedEvent 생성", test1_pass))
    
    # ===== 테스트 2: NoteCreatedEvent JSON 직렬화 =====
    print("\n[테스트 2] NoteCreatedEvent JSON 직렬화")
    print("-" * 70)
    
    try:
        event1_json = event1.model_dump_json()
        print(f"✅ JSON 직렬화 성공")
        print(f"   {event1_json[:100]}...")
        
        # JSON 파싱
        parsed = json.loads(event1_json)
        print(f"✅ JSON 파싱 성공")
        print(f"   event_type: {parsed['event_type']}")
        print(f"   note_id: {parsed['note_id'][:8]}...")
        
        test2_pass = True
        all_tests.append(("JSON 직렬화", test2_pass))
    
    except Exception as e:
        print(f"❌ 오류: {e}")
        test2_pass = False
        all_tests.append(("JSON 직렬화", test2_pass))
    
    # ===== 테스트 3: NoteCreatedEvent JSON 역직렬화 =====
    print("\n[테스트 3] NoteCreatedEvent JSON 역직렬화")
    print("-" * 70)
    
    try:
        event1_restored = NoteCreatedEvent.model_validate_json(event1_json)
        print(f"✅ 역직렬화 성공")
        print(f"   Note ID: {event1_restored.note_id[:8]}...")
        print(f"   User ID: {event1_restored.user_id}")
        
        # 원본과 복원본 비교
        assert event1.note_id == event1_restored.note_id
        assert event1.user_id == event1_restored.user_id
        assert event1.title == event1_restored.title
        print(f"✅ 원본과 복원본 동일 확인")
        
        test3_pass = True
        all_tests.append(("JSON 역직렬화", test3_pass))
    
    except Exception as e:
        print(f"❌ 오류: {e}")
        test3_pass = False
        all_tests.append(("JSON 역직렬화", test3_pass))
    
    # ===== 테스트 4: NoteUpdatedEvent (Optional 필드) =====
    print("\n[테스트 4] NoteUpdatedEvent (Optional 필드)")
    print("-" * 70)
    
    try:
        # 제목만 수정
        event2a = NoteUpdatedEvent(
            note_id="550e8400-e29b-41d4-a716-446655440000",
            user_id="user-123",
            title="Python 심화"  # title만 지정
        )
        
        print("✅ 제목만 수정 이벤트 생성")
        print(f"   Event Type: {event2a.event_type}")
        print(f"   Title: {event2a.title}")
        print(f"   Content (None): {event2a.content}")
        
        # 내용만 수정
        event2b = NoteUpdatedEvent(
            note_id="550e8400-e29b-41d4-a716-446655440000",
            user_id="user-123",
            content="Python 심화 내용입니다."  # content만 지정
        )
        
        print(f"✅ 내용만 수정 이벤트 생성")
        print(f"   Title (None): {event2b.title}")
        print(f"   Content: {event2b.content[:30]}...")
        
        test4_pass = True
        all_tests.append(("Optional 필드", test4_pass))
    
    except Exception as e:
        print(f"❌ 오류: {e}")
        test4_pass = False
        all_tests.append(("Optional 필드", test4_pass))
    
    # ===== 테스트 5: NoteDeletedEvent =====
    print("\n[테스트 5] NoteDeletedEvent")
    print("-" * 70)
    
    try:
        event3 = NoteDeletedEvent(
            note_id="550e8400-e29b-41d4-a716-446655440000",
            user_id="user-123"
        )
        
        print("✅ 이벤트 생성 성공")
        print(f"   Event Type: {event3.event_type}")
        print(f"   Note ID: {event3.note_id[:8]}...")
        print(f"   User ID: {event3.user_id}")
        
        test5_pass = True
        all_tests.append(("NoteDeletedEvent", test5_pass))
    
    except Exception as e:
        print(f"❌ 오류: {e}")
        test5_pass = False
        all_tests.append(("NoteDeletedEvent", test5_pass))
    
    # ===== 테스트 6: EventType Enum =====
    print("\n[테스트 6] EventType Enum")
    print("-" * 70)
    
    try:
        print("✅ EventType 값들:")
        for event_type in EventType:
            print(f"   - {event_type.name}: {event_type.value}")
        
        # 값으로 접근
        created = EventType.NOTE_CREATED
        assert created.value == "note.created"
        print(f"✅ EventType.NOTE_CREATED = {created.value}")
        
        test6_pass = True
        all_tests.append(("EventType Enum", test6_pass))
    
    except Exception as e:
        print(f"❌ 오류: {e}")
        test6_pass = False
        all_tests.append(("EventType Enum", test6_pass))
    
    # ===== 테스트 7: 필수 필드 검증 =====
    print("\n[테스트 7] 필수 필드 검증 (오류 처리)")
    print("-" * 70)
    
    try:
        # note_id 누락
        try:
            invalid_event = NoteCreatedEvent(
                user_id="user-123",
                title="Test",
                content="Test content"
                # note_id 누락!
            )
            print("❌ 유효성 검사 실패 (오류가 발생해야 함)")
            test7_pass = False
        except Exception as validation_error:
            print(f"✅ 필수 필드 검증 통과 (예상된 오류)")
            print(f"   {str(validation_error)[:80]}...")
            test7_pass = True
        
        all_tests.append(("필수 필드 검증", test7_pass))
    
    except Exception as e:
        print(f"❌ 예상치 못한 오류: {e}")
        test7_pass = False
        all_tests.append(("필수 필드 검증", test7_pass))
    
    # ===== 테스트 8: 타입 검증 =====
    print("\n[테스트 8] 타입 검증 (오류 처리)")
    print("-" * 70)
    
    try:
        # user_id를 숫자로 입력 (문자열이어야 함)
        try:
            invalid_event = NoteCreatedEvent(
                note_id="550e8400-e29b-41d4-a716-446655440000",
                user_id=12345,  # 숫자 대신 문자열 필요
                title="Test",
                content="Test content"
            )
            print("❌ 타입 검증 실패 (오류가 발생해야 함)")
            test8_pass = False
        except Exception as type_error:
            print(f"✅ 타입 검증 통과 (예상된 오류)")
            print(f"   {str(type_error)[:80]}...")
            test8_pass = True
        
        all_tests.append(("타입 검증", test8_pass))
    
    except Exception as e:
        print(f"❌ 예상치 못한 오류: {e}")
        test8_pass = False
        all_tests.append(("타입 검증", test8_pass))
    
    # ===== 테스트 9: dict 변환 =====
    print("\n[테스트 9] dict 변환")
    print("-" * 70)
    
    try:
        event_dict = event1.model_dump()
        
        print("✅ dict 변환 성공")
        print(f"   Keys: {list(event_dict.keys())}")
        print(f"   event_type: {event_dict['event_type']}")
        print(f"   note_id: {event_dict['note_id'][:8]}...")
        
        # dict에서 새로운 객체 생성
        event_from_dict = NoteCreatedEvent(**event_dict)
        print(f"✅ dict에서 객체 생성 성공")
        
        test9_pass = True
        all_tests.append(("dict 변환", test9_pass))
    
    except Exception as e:
        print(f"❌ 오류: {e}")
        test9_pass = False
        all_tests.append(("dict 변환", test9_pass))
    
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
        print(f"✅ 모든 테스트 통과! ({len(all_tests)}/{len(all_tests)})")
    else:
        failed_count = sum(1 for _, result in all_tests if not result)
        print(f"❌ {failed_count}개 테스트 실패")
    print("="*70 + "\n")
    
    return all_pass


if __name__ == "__main__":
    success = test_event_schemas()
    sys.exit(0 if success else 1)
