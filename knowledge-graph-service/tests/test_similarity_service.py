"""유사도 서비스 테스트"""
import sys
from pathlib import Path
import logging
import uuid
import random
import math


# 프로젝트 루트를 path에 추가
sys.path.insert(0, str(Path(__file__).parent.parent))


from app.crud import note as note_crud
from app.services.similarity_service import similarity_service
from app.core.constants import VectorConfig


# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)


def generate_dummy_embedding(seed: int = 0, variation: float = 0.1) -> list:
    """
    테스트용 더미 임베딩 생성 (유사한 값들)
    
    Args:
        seed: 시드값 (기본 벡터 결정)
        variation: 변화도 (0~1, 0이면 동일, 1이면 완전히 다름)
    
    Returns:
        1536차원 벡터 (유사도가 높게 생성됨)
    """
    # 기본 벡터 생성 (seed 기준)
    random.seed(seed)
    base_embedding = [random.uniform(-1, 1) for _ in range(VectorConfig.EMBEDDING_DIMENSION)]
    
    # 벡터 정규화
    magnitude = math.sqrt(sum(x**2 for x in base_embedding))
    base_embedding = [x / magnitude for x in base_embedding]
    
    # variation을 적용해서 유사한 벡터 생성
    random.seed(seed + 1000)  # variation을 위한 다른 시드
    result = []
    for val in base_embedding:
        # 기본값에 약간의 노이즈 추가
        noise = random.uniform(-variation, variation)
        result.append(val + noise)
    
    # 최종 정규화
    magnitude = math.sqrt(sum(x**2 for x in result))
    result = [x / magnitude for x in result]
    
    return result


def generate_test_id(prefix: str) -> str:
    """테스트용 고유 ID 생성"""
    return f"{prefix}-{uuid.uuid4().hex[:8]}"


def test_find_similar_notes():
    """유사 노트 검색 테스트"""
    print("\n" + "="*60)
    print("[테스트 1] 유사 노트 검색")
    print("="*60)
    
    test_user = generate_test_id("test-user")
    
    # 1. 여러 노트 생성 (seed=1로 모두 유사하게)
    note_ids = []
    for i in range(1, 6):
        note_id = generate_test_id(f"sim-note-{i}")
        note_ids.append(note_id)
        
        # 👈 seed=1, variation=0.15로 유사한 임베딩 생성
        embedding = generate_dummy_embedding(seed=1, variation=0.15)
        
        note_crud.create_note(
            note_id=note_id,
            user_id=test_user,
            title=f"유사도 테스트 노트 {i}",
            embedding=embedding
        )
    
    print(f"✅ 노트 생성 완료: {len(note_ids)}개")
    
    # 2. 첫 번째 노트의 임베딩으로 유사 노트 검색
    embedding = generate_dummy_embedding(seed=1, variation=0.15)
    
    similar_notes = similarity_service.find_similar_notes(
        user_id=test_user,
        note_id=note_ids[0],
        embedding=embedding
    )
    
    print(f"✅ 유사 노트 검색 완료: {len(similar_notes)}개 발견")
    for note in similar_notes:
        print(f"   - {note['title']} (유사도: {note['similarity_score']:.4f})")
    
    assert len(similar_notes) > 0, "유사 노트가 검색되지 않음"


def test_create_similarity_relationships():
    """유사도 관계 생성 테스트"""
    print("\n" + "="*60)
    print("[테스트 2] 유사도 관계 생성")
    print("="*60)
    
    test_user = generate_test_id("test-user")
    
    # 1. 여러 노트 생성 (모두 유사하게)
    note_ids = []
    for i in range(1, 6):
        note_id = generate_test_id(f"rel-note-{i}")
        note_ids.append(note_id)
        
        # 👈 seed=2, variation=0.2로 유사한 임베딩 생성
        embedding = generate_dummy_embedding(seed=2, variation=0.2)
        
        note_crud.create_note(
            note_id=note_id,
            user_id=test_user,
            title=f"관계 생성 테스트 {i}",
            embedding=embedding
        )
    
    print(f"✅ 노트 생성 완료: {len(note_ids)}개")
    
    # 2. 첫 번째 노트로 관계 생성
    embedding = generate_dummy_embedding(seed=2, variation=0.2)
    
    count = similarity_service.create_similarity_relationships(
        user_id=test_user,
        note_id=note_ids[0],
        embedding=embedding
    )
    
    print(f"✅ 관계 생성 완료: {count}개 관계 생성됨")
    assert count > 0, "관계가 생성되지 않음"
    
    # 3. 생성된 관계 확인
    related_count = similarity_service.get_related_notes_count(
        user_id=test_user,
        note_id=note_ids[0]
    )
    
    print(f"✅ 관계 확인: {related_count}개 관계 연결됨")
    assert related_count > 0, "관계가 생성되지 않음"


def test_delete_similarity_relationships():
    """유사도 관계 삭제 테스트"""
    print("\n" + "="*60)
    print("[테스트 3] 유사도 관계 삭제")
    print("="*60)
    
    test_user = generate_test_id("test-user")
    main_note_id = generate_test_id("main-note")
    
    # 1. 노트 생성 및 관계 생성 (모두 유사하게)
    note_ids = [main_note_id]
    for i in range(1, 4):
        note_id = generate_test_id(f"del-note-{i}")
        note_ids.append(note_id)
        
        # 👈 seed=3, variation=0.2로 유사한 임베딩 생성
        embedding = generate_dummy_embedding(seed=3, variation=0.2)
        note_crud.create_note(
            note_id=note_id,
            user_id=test_user,
            title=f"삭제 테스트 {i}",
            embedding=embedding
        )
    
    # 메인 노트 생성
    main_embedding = generate_dummy_embedding(seed=3, variation=0.2)
    note_crud.create_note(
        note_id=main_note_id,
        user_id=test_user,
        title="메인 노트",
        embedding=main_embedding
    )
    
    # 관계 생성
    count = similarity_service.create_similarity_relationships(
        user_id=test_user,
        note_id=main_note_id,
        embedding=main_embedding
    )
    print(f"✅ 관계 생성: {count}개")
    assert count > 0, "관계가 생성되지 않음"
    
    # 2. 관계 삭제
    deleted_count = similarity_service.delete_similarity_relationships(
        user_id=test_user,
        note_id=main_note_id
    )
    print(f"✅ 관계 삭제: {deleted_count}개 삭제됨")
    
    # 3. 삭제 확인
    remaining_count = similarity_service.get_related_notes_count(
        user_id=test_user,
        note_id=main_note_id
    )
    print(f"✅ 삭제 확인: {remaining_count}개 관계 남음")
    assert remaining_count == 0, "관계가 완전히 삭제되지 않음"


def test_get_related_notes_count():
    """연결된 노트 개수 조회 테스트"""
    print("\n" + "="*60)
    print("[테스트 4] 연결된 노트 개수 조회")
    print("="*60)
    
    test_user = generate_test_id("test-user")
    
    # 1. 노트 생성 (모두 유사하게)
    note_ids = []
    for i in range(1, 4):
        note_id = generate_test_id(f"count-note-{i}")
        note_ids.append(note_id)
        
        # 👈 seed=4, variation=0.2로 유사한 임베딩 생성
        embedding = generate_dummy_embedding(seed=4, variation=0.2)
        note_crud.create_note(
            note_id=note_id,
            user_id=test_user,
            title=f"개수 테스트 {i}",
            embedding=embedding
        )
    
    # 2. 관계 생성
    main_embedding = generate_dummy_embedding(seed=4, variation=0.2)
    count = similarity_service.create_similarity_relationships(
        user_id=test_user,
        note_id=note_ids[0],
        embedding=main_embedding
    )
    print(f"✅ 관계 생성: {count}개")
    assert count > 0, "관계가 생성되지 않음"
    
    # 3. 개수 조회
    related_count = similarity_service.get_related_notes_count(
        user_id=test_user,
        note_id=note_ids[0]
    )
    print(f"✅ 연결된 노트 개수: {related_count}개")
    assert related_count == count, "개수 불일치"


def test_get_user_similarity_stats():
    """유저 유사도 통계 테스트"""
    print("\n" + "="*60)
    print("[테스트 5] 유저 유사도 통계")
    print("="*60)
    
    test_user = generate_test_id("test-user")
    
    # 1. 여러 노트 생성 (모두 유사하게) - note_id 저장
    note_ids = []  # 👈 추가: note_id 저장할 리스트
    for i in range(1, 6):
        note_id = generate_test_id(f"stats-note-{i}")
        note_ids.append(note_id)  # 👈 저장
        
        # 👈 seed=5, variation=0.2로 모두 유사하게
        embedding = generate_dummy_embedding(seed=5, variation=0.2)
        
        note_crud.create_note(
            note_id=note_id,
            user_id=test_user,
            title=f"통계 테스트 {i}",
            embedding=embedding
        )
    
    print(f"✅ 노트 생성 완료: {len(note_ids)}개")
    
    # 2. 관계 생성 (모든 노트에 대해) - 저장된 note_id 사용
    for i, note_id in enumerate(note_ids[:3]):  # 👈 처음 3개만 사용
        # 👈 seed=5, variation=0.2로 유사하게
        embedding = generate_dummy_embedding(seed=5, variation=0.2)
        
        count = similarity_service.create_similarity_relationships(
            user_id=test_user,
            note_id=note_id,  # 👈 저장된 note_id 사용
            embedding=embedding
        )
        print(f"   노트 {i+1}: {count}개 관계 생성")
    
    # 3. 통계 조회
    stats = similarity_service.get_user_similarity_stats(user_id=test_user)
    
    print(f"✅ 통계 조회 완료:")
    print(f"   - 전체 노트: {stats['total_notes']}개")
    print(f"   - 관계: {stats['total_relationships']}개")
    print(f"   - 평균 유사도: {stats['avg_similarity_score']:.4f}")
    
    assert stats['total_notes'] > 0, "노트가 없음"
    assert stats['total_relationships'] > 0, "관계가 없음"


def test_user_isolation_similarity():
    """유저 격리 테스트 (유사도 검색에서도 격리)"""
    print("\n" + "="*60)
    print("[테스트 6] 유저 격리 (유사도 검색)")
    print("="*60)
    
    user1 = generate_test_id("user-1")
    user2 = generate_test_id("user-2")
    
    # 1. User1이 노트 생성 (seed=6으로 유사하게)
    user1_notes = []
    for i in range(1, 4):
        note_id = generate_test_id(f"user1-note-{i}")
        user1_notes.append(note_id)
        # 👈 seed=6, variation=0.2로 유사하게
        embedding = generate_dummy_embedding(seed=6, variation=0.2)
        
        note_crud.create_note(
            note_id=note_id,
            user_id=user1,
            title=f"User1 노트 {i}",
            embedding=embedding
        )
    
    # 2. User2가 노트 생성 (seed=7로 다르게)
    user2_notes = []
    for i in range(1, 4):
        note_id = generate_test_id(f"user2-note-{i}")
        user2_notes.append(note_id)
        # 👈 seed=7, variation=0.2로 다르게
        embedding = generate_dummy_embedding(seed=7, variation=0.2)
        
        note_crud.create_note(
            note_id=note_id,
            user_id=user2,
            title=f"User2 노트 {i}",
            embedding=embedding
        )
    
    # 3. User1이 관계 생성
    embedding = generate_dummy_embedding(seed=6, variation=0.2)
    user1_relationships = similarity_service.create_similarity_relationships(
        user_id=user1,
        note_id=user1_notes[0],
        embedding=embedding
    )
    print(f"✅ User1 관계 생성: {user1_relationships}개")
    assert user1_relationships > 0, "User1 관계 생성 실패"
    
    # 4. User2가 관계 생성
    embedding = generate_dummy_embedding(seed=7, variation=0.2)
    user2_relationships = similarity_service.create_similarity_relationships(
        user_id=user2,
        note_id=user2_notes[0],
        embedding=embedding
    )
    print(f"✅ User2 관계 생성: {user2_relationships}개")
    assert user2_relationships > 0, "User2 관계 생성 실패"
    
    # 5. User2가 User1의 노트로 검색 시도
    similar_notes = similarity_service.find_similar_notes(
        user_id=user2,
        note_id=user2_notes[0],
        embedding=generate_dummy_embedding(seed=7, variation=0.2)
    )
    
    # User1의 노트가 검색되면 안 됨
    user1_note_ids = set(user1_notes)
    found_user1_notes = [n for n in similar_notes if n['note_id'] in user1_note_ids]
    
    assert len(found_user1_notes) == 0, "User2가 User1의 노트를 볼 수 있음!"
    print(f"✅ 유저 격리 확인: User2가 User1의 노트를 볼 수 없음")


def run_all_tests():
    """모든 테스트 실행"""
    print("\n\n" + "█"*60)
    print("█" + " "*58 + "█")
    print("█" + "  유사도 서비스 전체 테스트 시작".center(58) + "█")
    print("█" + " "*58 + "█")
    print("█"*60)
    
    try:
        test_find_similar_notes()
        test_create_similarity_relationships()
        test_delete_similarity_relationships()
        test_get_related_notes_count()
        test_get_user_similarity_stats()
        test_user_isolation_similarity()
        
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
