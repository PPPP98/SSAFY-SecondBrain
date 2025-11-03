"""API 통합 테스트"""
import sys
from pathlib import Path
import uuid

# 프로젝트 루트를 path에 추가
sys.path.insert(0, str(Path(__file__).parent.parent))

import pytest
from fastapi.testclient import TestClient
from main import app

# TestClient 생성
client = TestClient(app)

# 테스트용 상수
TEST_USER_ID = "test-user-api"
TEST_NOTE_ID = str(uuid.uuid4())


# ===== 기본 헬스 체크 =====
def test_health_check():
    """헬스 체크 테스트"""
    print("\n" + "="*60)
    print("[테스트 1] 헬스 체크")
    print("="*60)
    
    response = client.get("/health")
    
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert data["service"] == "knowledge-graph-service"
    
    print(f"✅ 헬스 체크 성공")
    print(f"   - Status: {data['status']}")
    print(f"   - Service: {data['service']}")


def test_root():
    """루트 엔드포인트 테스트"""
    print("\n" + "="*60)
    print("[테스트 2] 루트 엔드포인트")
    print("="*60)
    
    response = client.get("/")
    
    assert response.status_code == 200
    data = response.json()
    assert "message" in data
    
    print(f"✅ 루트 엔드포인트 성공")
    print(f"   - Message: {data['message']}")


# ===== 노트 API 테스트 =====
def test_create_note():
    """노트 생성 테스트"""
    print("\n" + "="*60)
    print("[테스트 3] 노트 생성 API")
    print("="*60)
    
    payload = {
        "note_id": TEST_NOTE_ID,
        "title": "API 테스트 노트",
        "content": "이것은 API 통합 테스트용 노트입니다. Neo4j 기반 지식 그래프 서비스입니다."
    }
    
    response = client.post(
        "/api/v1/notes",
        json=payload,
        headers={"X-User-ID": TEST_USER_ID}
    )
    
    print(f"   - Status: {response.status_code}")
    
    assert response.status_code == 200, f"오류: {response.json()}"
    
    data = response.json()
    assert data["note_id"] == TEST_NOTE_ID
    assert data["user_id"] == TEST_USER_ID
    assert data["embedding_dimension"] == 1536
    
    print(f"✅ 노트 생성 성공")
    print(f"   - Note ID: {data['note_id']}")
    print(f"   - Embedding Dim: {data['embedding_dimension']}")
    print(f"   - Linked Notes: {data['linked_notes_count']}")


def test_get_note():
    """노트 조회 테스트"""
    print("\n" + "="*60)
    print("[테스트 4] 노트 조회 API")
    print("="*60)
    
    response = client.get(
        f"/api/v1/notes/{TEST_NOTE_ID}",
        headers={"X-User-ID": TEST_USER_ID}
    )
    
    assert response.status_code == 200
    data = response.json()
    
    assert data["note_id"] == TEST_NOTE_ID
    assert data["title"] == "API 테스트 노트"
    assert "similar_notes" in data
    
    print(f"✅ 노트 조회 성공")
    print(f"   - Title: {data['title']}")
    print(f"   - Similar Notes: {len(data['similar_notes'])}")


def test_list_notes():
    """노트 목록 조회 테스트"""
    print("\n" + "="*60)
    print("[테스트 5] 노트 목록 조회 API")
    print("="*60)
    
    response = client.get(
        "/api/v1/notes?limit=20&skip=0",
        headers={"X-User-ID": TEST_USER_ID}
    )
    
    assert response.status_code == 200
    data = response.json()
    
    assert data["user_id"] == TEST_USER_ID
    assert "notes" in data
    assert "total" in data
    assert data["limit"] == 20
    assert data["skip"] == 0
    
    print(f"✅ 노트 목록 조회 성공")
    print(f"   - Total: {data['total']}")
    print(f"   - Returned: {len(data['notes'])}")


def test_search_notes():
    """제목 검색 API 테스트"""
    print("\n" + "="*60)
    print("[테스트 6] 제목 검색 API")
    print("="*60)
    
    response = client.get(
        "/api/v1/search/by-title?title=API&limit=20",
        headers={"X-User-ID": TEST_USER_ID}
    )
    
    assert response.status_code == 200
    data = response.json()
    
    assert data["user_id"] == TEST_USER_ID
    assert "notes" in data
    
    print(f"✅ 제목 검색 성공")
    print(f"   - Search Term: 'API'")
    print(f"   - Found: {len(data['notes'])}")
    print(f"   - Total: {data['total']}")


def test_get_stats():
    """통계 조회 API 테스트"""
    print("\n" + "="*60)
    print("[테스트 7] 통계 조회 API")
    print("="*60)
    
    response = client.get(
        "/api/v1/stats",
        headers={"X-User-ID": TEST_USER_ID}
    )
    
    assert response.status_code == 200
    data = response.json()
    
    assert data["user_id"] == TEST_USER_ID
    assert "total_notes" in data
    assert "total_relationships" in data
    assert "avg_connections" in data
    
    print(f"✅ 통계 조회 성공")
    print(f"   - Total Notes: {data['total_notes']}")
    print(f"   - Total Relationships: {data['total_relationships']}")
    print(f"   - Avg Connections: {data['avg_connections']:.2f}")


def test_missing_header():
    """Header 없을 시 테스트"""
    print("\n" + "="*60)
    print("[테스트 8] Header 없을 시 에러")
    print("="*60)
    
    # X-User-ID Header 없음
    response = client.get("/api/v1/notes")
    
    assert response.status_code == 422
    
    print(f"✅ Header 검증 성공")
    print(f"   - Status: {response.status_code}")


def test_note_not_found():
    """존재하지 않는 노트 조회"""
    print("\n" + "="*60)
    print("[테스트 9] 존재하지 않는 노트 조회")
    print("="*60)
    
    fake_note_id = str(uuid.uuid4())
    
    response = client.get(
        f"/api/v1/notes/{fake_note_id}",
        headers={"X-User-ID": TEST_USER_ID}
    )
    
    assert response.status_code == 404
    
    print(f"✅ 404 에러 처리 성공")
    print(f"   - Status: {response.status_code}")


def test_delete_note():
    """노트 삭제 테스트"""
    print("\n" + "="*60)
    print("[테스트 10] 노트 삭제 API")
    print("="*60)
    
    response = client.delete(
        f"/api/v1/notes/{TEST_NOTE_ID}",
        headers={"X-User-ID": TEST_USER_ID}
    )
    
    assert response.status_code == 200
    data = response.json()
    
    assert data["status"] == "success"
    
    print(f"✅ 노트 삭제 성공")
    print(f"   - Message: {data['message']}")
    
    # 삭제 확인
    response = client.get(
        f"/api/v1/notes/{TEST_NOTE_ID}",
        headers={"X-User-ID": TEST_USER_ID}
    )
    
    assert response.status_code == 404
    print(f"✅ 삭제 확인 완료")


def run_all_tests():
    """모든 테스트 실행"""
    print("\n\n" + "█"*60)
    print("█" + " "*58 + "█")
    print("█" + "  API 통합 테스트 시작".center(58) + "█")
    print("█" + " "*58 + "█")
    print("█"*60)
    
    try:
        test_health_check()
        test_root()
        test_create_note()
        test_get_note()
        test_list_notes()
        test_search_notes()
        test_get_stats()
        test_missing_header()
        test_note_not_found()
        test_delete_note()
        
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
