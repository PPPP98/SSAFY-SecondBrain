"""CRUD 기능 테스트"""
import sys
from pathlib import Path
import logging
import uuid

# 프로젝트 루트를 path에 추가
sys.path.insert(0, str(Path(__file__).parent.parent))

from app.crud import note as note_crud

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)

# 테스트용 상수
TEST_USER_ID = "test-user-123"

# 더미 임베딩 (1536차원)
DUMMY_EMBEDDING = [0.1] * 1536


def generate_test_id(prefix: str) -> str:
    """테스트용 고유 ID 생성"""
    return f"{prefix}-{uuid.uuid4().hex[:8]}"


def test_create_note():
    """노트 생성 테스트"""
    print("\n" + "="*60)
    print("[테스트 1] 노트 생성")
    print("="*60)
    
    test_note_id = generate_test_id("test-note")  # 👈 고유 ID 생성
    
    note_id = note_crud.create_note(
        note_id=test_note_id,
        user_id=TEST_USER_ID,
        title="Neo4j 기초",
        embedding=DUMMY_EMBEDDING
    )
    
    assert note_id == test_note_id
    print(f"✅ 노트 생성 성공: {note_id}")
    return test_note_id


def test_get_note():
    """노트 조회 테스트"""
    print("\n" + "="*60)
    print("[테스트 2] 노트 조회")
    print("="*60)
    
    test_note_id = generate_test_id("test-note")  # 👈 새로운 고유 ID
    
    # 먼저 노트 생성
    note_crud.create_note(
        note_id=test_note_id,
        user_id=TEST_USER_ID,
        title="Neo4j 기초",
        embedding=DUMMY_EMBEDDING
    )
    
    # 조회
    note = note_crud.get_note(user_id=TEST_USER_ID, note_id=test_note_id)
    
    assert note is not None
    assert note["note_id"] == test_note_id
    assert note["user_id"] == TEST_USER_ID
    assert note["title"] == "Neo4j 기초"
    
    print(f"✅ 노트 조회 성공")
    print(f"   - note_id: {note['note_id']}")
    print(f"   - title: {note['title']}")
    print(f"   - created_at: {note['created_at']}")


def test_get_all_notes():
    """노트 목록 조회 테스트"""
    print("\n" + "="*60)
    print("[테스트 3] 노트 목록 조회 (페이지네이션)")
    print("="*60)
    
    test_user = generate_test_id("test-user")  # 👈 테스트용 유저
    
    # 여러 노트 생성
    for i in range(1, 4):
        note_crud.create_note(
            note_id=generate_test_id(f"note-{i}"),  # 👈 각각 고유 ID
            user_id=test_user,
            title=f"노트 제목 {i}",
            embedding=DUMMY_EMBEDDING
        )
    
    # 목록 조회
    notes, total = note_crud.get_all_notes(
        user_id=test_user,
        limit=10,
        skip=0
    )
    
    assert len(notes) >= 3
    assert total >= 3
    
    print(f"✅ 노트 목록 조회 성공")
    print(f"   - 조회된 노트: {len(notes)}개")
    print(f"   - 전체 노트: {total}개")
    for i, note in enumerate(notes[:3], 1):
        print(f"   {i}. {note['title']}")


def test_pagination():
    """페이지네이션 테스트"""
    print("\n" + "="*60)
    print("[테스트 4] 페이지네이션")
    print("="*60)
    
    test_user = generate_test_id("test-user")
    
    # 5개 노트 생성
    for i in range(1, 6):
        note_crud.create_note(
            note_id=generate_test_id(f"pagination-{i}"),
            user_id=test_user,
            title=f"페이지네이션 테스트 {i}",
            embedding=DUMMY_EMBEDDING
        )
    
    # 첫 페이지 (limit=2, skip=0)
    page1, total = note_crud.get_all_notes(
        user_id=test_user,
        limit=2,
        skip=0
    )
    
    print(f"✅ 페이지 1: {len(page1)}개 (전체: {total}개)")
    
    # 두 번째 페이지 (limit=2, skip=2)
    page2, _ = note_crud.get_all_notes(
        user_id=test_user,
        limit=2,
        skip=2
    )
    
    print(f"✅ 페이지 2: {len(page2)}개")
    
    # 페이지 간 노트가 다른지 확인
    page1_ids = {n["note_id"] for n in page1}
    page2_ids = {n["note_id"] for n in page2}
    
    if page1_ids.isdisjoint(page2_ids):  # 교집합이 없으면
        print(f"✅ 페이지 분리 정상")
    else:
        print(f"⚠️  페이지 분리 문제 (중복 노트 있음)")


def test_search_by_title():
    """제목 검색 테스트"""
    print("\n" + "="*60)
    print("[테스트 5] 제목 검색")
    print("="*60)
    
    test_user = generate_test_id("test-user")
    
    # 검색 가능한 노트 생성
    note_crud.create_note(
        note_id=generate_test_id("search-note"),
        user_id=test_user,
        title="Neo4j 검색 테스트",
        embedding=DUMMY_EMBEDDING
    )
    
    # 검색
    results = note_crud.get_note_by_title(
        user_id=test_user,
        title="Neo4j"
    )
    
    print(f"✅ 검색 완료: 'Neo4j' 포함 노트 {len(results)}개")
    for note in results:
        print(f"   - {note['title']}")


def test_count_user_notes():
    """유저 노트 개수 테스트"""
    print("\n" + "="*60)
    print("[테스트 6] 유저 노트 개수")
    print("="*60)
    
    test_user = generate_test_id("test-user")
    
    # 노트 3개 생성
    for i in range(1, 4):
        note_crud.create_note(
            note_id=generate_test_id(f"count-{i}"),
            user_id=test_user,
            title=f"카운트 테스트 {i}",
            embedding=DUMMY_EMBEDDING
        )
    
    count = note_crud.count_user_notes(user_id=test_user)
    
    print(f"✅ 유저 {test_user[:20]}... 노트 개수: {count}개")


def test_get_stats():
    """통계 조회 테스트"""
    print("\n" + "="*60)
    print("[테스트 7] 그래프 통계")
    print("="*60)
    
    test_user = generate_test_id("test-user")
    
    # 노트 생성
    note_crud.create_note(
        note_id=generate_test_id("stats-note"),
        user_id=test_user,
        title="통계 테스트",
        embedding=DUMMY_EMBEDDING
    )
    
    stats = note_crud.get_stats(user_id=test_user)
    
    assert stats["user_id"] == test_user
    
    print(f"✅ 통계 조회 성공")
    print(f"   - 전체 노트: {stats['total_notes']}개")
    print(f"   - 관계: {stats['total_relationships']}개")
    print(f"   - 평균 연결: {stats['avg_connections']:.2f}개")


def test_delete_note():
    """노트 삭제 테스트"""
    print("\n" + "="*60)
    print("[테스트 8] 노트 삭제")
    print("="*60)
    
    test_user = generate_test_id("test-user")
    test_id = generate_test_id("delete-note")
    
    # 노트 생성
    note_crud.create_note(
        note_id=test_id,
        user_id=test_user,
        title="삭제할 노트",
        embedding=DUMMY_EMBEDDING
    )
    
    # 존재 확인
    note = note_crud.get_note(user_id=test_user, note_id=test_id)
    assert note is not None
    print(f"✅ 노트 생성 확인")
    
    # 삭제
    deleted = note_crud.delete_note(user_id=test_user, note_id=test_id)
    assert deleted is True
    print(f"✅ 노트 삭제 성공")
    
    # 삭제 확인
    note = note_crud.get_note(user_id=test_user, note_id=test_id)
    assert note is None
    print(f"✅ 삭제 확인 완료")


def test_user_isolation():
    """유저 격리 테스트 (다른 유저는 노트를 볼 수 없음)"""
    print("\n" + "="*60)
    print("[테스트 9] 유저 격리 (보안 테스트)")
    print("="*60)
    
    user1 = generate_test_id("user-1")
    user2 = generate_test_id("user-2")
    note_id = generate_test_id("isolation-note")
    
    # user1이 노트 생성
    note_crud.create_note(
        note_id=note_id,
        user_id=user1,
        title="User1의 노트",
        embedding=DUMMY_EMBEDDING
    )
    
    # user1 노트 조회 (성공해야 함)
    note = note_crud.get_note(user_id=user1, note_id=note_id)
    assert note is not None
    print(f"✅ User1이 자신의 노트 조회 가능")
    
    # user2가 user1의 노트 조회 (실패해야 함)
    note = note_crud.get_note(user_id=user2, note_id=note_id)
    assert note is None
    print(f"✅ User2가 User1의 노트를 볼 수 없음 (격리됨)")


def run_all_tests():
    """모든 테스트 실행"""
    print("\n\n" + "█"*60)
    print("█" + " "*58 + "█")
    print("█" + "  Neo4j CRUD 전체 테스트 시작".center(58) + "█")
    print("█" + " "*58 + "█")
    print("█"*60)
    
    try:
        test_create_note()
        test_get_note()
        test_get_all_notes()
        test_pagination()
        test_search_by_title()
        test_count_user_notes()
        test_get_stats()
        test_delete_note()
        test_user_isolation()
        
        print("\n\n" + "█"*60)
        print("█" + " "*58 + "█")
        print("█" + "🎉 모든 테스트 통과!".center(58) + "█")
        print("█" + " "*58 + "█")
        print("█"*60 + "\n")
        
        return True
    
    except AssertionError as e:
        print(f"\n\n❌ 테스트 실패: {e}")
        return False
    
    except Exception as e:
        print(f"\n\n❌ 오류 발생: {e}")
        import traceback
        traceback.print_exc()
        return False


if __name__ == "__main__":
    success = run_all_tests()
    sys.exit(0 if success else 1)
