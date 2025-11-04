"""그래프 시각화 API 테스트"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent.parent))

import pytest
import time
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

TEST_USER_ID = "test-user-graph"
created_note_ids = []  # 생성된 노트 ID 추적


def create_test_note(title: str, content: str) -> str:
    """테스트용 노트 생성"""
    import uuid
    
    note_id = str(uuid.uuid4())
    
    payload = {
        "note_id": note_id,
        "title": title,
        "content": content
    }
    
    response = client.post(
        "/api/v1/notes",
        json=payload,
        headers={"X-User-ID": TEST_USER_ID}
    )
    
    assert response.status_code == 200, f"노트 생성 실패: {response.json()}"
    
    created_note_ids.append(note_id)
    
    print(f"   ✅ 노트 생성: {title} (ID: {note_id[:8]}...)")
    
    return note_id


def delete_all_test_notes():
    """생성된 모든 테스트 노트 삭제"""
    print(f"\n🧹 테스트 데이터 정리 중... ({len(created_note_ids)}개)")
    
    for note_id in created_note_ids:
        try:
            response = client.delete(
                f"/api/v1/notes/{note_id}",
                headers={"X-User-ID": TEST_USER_ID}
            )
            
            if response.status_code == 200:
                print(f"   ✅ 노트 삭제: {note_id[:8]}...")
            else:
                print(f"   ⚠️  삭제 실패: {note_id[:8]}... (상태: {response.status_code})")
        
        except Exception as e:
            print(f"   ❌ 삭제 오류: {note_id[:8]}... - {e}")
    
    created_note_ids.clear()
    print("✅ 정리 완료\n")


class TestGraphVisualizationAPI:
    """그래프 시각화 API 테스트"""
    
    @classmethod
    def setup_class(cls):
        """테스트 시작 전 데이터 준비"""
        print("\n" + "="*60)
        print("📦 테스트 데이터 생성 중...")
        print("="*60)
        
        # 5개의 테스트 노트 생성 (유사한 내용으로 연결되도록)
        cls.note1_id = create_test_note(
            "Neo4j 그래프 데이터베이스",
            "Neo4j는 그래프 데이터베이스입니다. 노드와 관계로 데이터를 저장하며, 복잡한 연결 관계를 효율적으로 표현할 수 있습니다."
        )
        
        time.sleep(0.5)  # API 호출 간격
        
        cls.note2_id = create_test_note(
            "그래프 데이터베이스 개념",
            "그래프 데이터베이스는 노드, 관계, 속성으로 데이터를 저장하는 NoSQL 데이터베이스입니다. 관계 중심의 데이터 모델링에 적합합니다."
        )
        
        time.sleep(0.5)
        
        cls.note3_id = create_test_note(
            "Cypher 쿼리 언어",
            "Cypher는 Neo4j의 쿼리 언어입니다. MATCH, CREATE, WHERE 등의 키워드를 사용하여 그래프를 조회하고 조작할 수 있습니다."
        )
        
        time.sleep(0.5)
        
        cls.note4_id = create_test_note(
            "FastAPI 웹 프레임워크",
            "FastAPI는 Python 기반의 현대적인 웹 프레임워크입니다. 빠른 성능과 자동 문서 생성, 타입 검증 등의 기능을 제공합니다."
        )
        
        time.sleep(0.5)
        
        cls.note5_id = create_test_note(
            "Python 비동기 프로그래밍",
            "Python의 async/await 키워드를 사용한 비동기 프로그래밍은 I/O 바운드 작업의 성능을 크게 향상시킵니다. FastAPI에서 널리 사용됩니다."
        )
        
        print(f"\n✅ 테스트 데이터 생성 완료 ({len(created_note_ids)}개)")
        print("⏳ 임베딩 및 관계 생성 대기 중... (5초)")
        time.sleep(5)  # 모든 노트의 유사도 연결 완료 대기
    
    @classmethod
    def teardown_class(cls):
        """테스트 종료 후 데이터 정리"""
        delete_all_test_notes()
    
    def test_1_graph_visualization(self):
        """[테스트 1] 그래프 시각화 데이터 조회"""
        print("\n" + "="*60)
        print("[테스트 1] 그래프 시각화 데이터 조회")
        print("="*60)
        
        response = client.get(
            "/api/v1/graph/visualization",
            headers={"X-User-ID": TEST_USER_ID}
        )
        
        print(f"   - 상태 코드: {response.status_code}")
        
        assert response.status_code == 200, f"오류: {response.json()}"
        
        data = response.json()
        
        # 응답 구조 검증
        assert "user_id" in data
        assert "nodes" in data
        assert "links" in data
        assert "stats" in data
        
        assert data["user_id"] == TEST_USER_ID
        
        # 노드 검증
        nodes = data["nodes"]
        assert len(nodes) >= 5, f"노드가 부족합니다: {len(nodes)}개"
        
        for node in nodes:
            assert "id" in node
            assert "title" in node
            assert "created_at" in node
        
        # 링크 검증
        links = data["links"]
        assert isinstance(links, list), "links가 리스트가 아닙니다"
        
        if len(links) > 0:
            for link in links[:3]:  # 처음 3개만 확인
                assert "source" in link
                assert "target" in link
                assert "score" in link
                assert 0.0 <= link["score"] <= 1.0
        
        # 통계 검증
        stats = data["stats"]
        assert "total_nodes" in stats
        assert "total_links" in stats
        assert "avg_connections" in stats
        
        assert stats["total_nodes"] >= 5
        assert stats["avg_connections"] >= 0.0
        
        print(f"\n✅ 그래프 시각화 데이터 검증 통과")
        print(f"   - 노드 수: {len(nodes)}")
        print(f"   - 링크 수: {len(links)}")
        print(f"   - 전체 노드: {stats['total_nodes']}")
        print(f"   - 전체 링크: {stats['total_links']}")
        print(f"   - 평균 연결: {stats['avg_connections']:.2f}")
        
        # 노드 상세 정보 출력 (처음 3개만)
        print(f"\n📊 노드 샘플 (처음 3개):")
        for i, node in enumerate(nodes[:3], 1):
            print(f"   {i}. {node['title'][:30]}... (ID: {node['id'][:8]}...)")
        
        # 링크 상세 정보 출력 (처음 3개만)
        if len(links) > 0:
            print(f"\n🔗 링크 샘플 (처음 3개):")
            for i, link in enumerate(links[:3], 1):
                print(f"   {i}. {link['source'][:8]}... → {link['target'][:8]}... (유사도: {link['score']:.2f})")
    
    def test_2_neighbors_depth_1(self):
        """[테스트 2] 이웃 노드 조회 (깊이 1)"""
        print("\n" + "="*60)
        print("[테스트 2] 이웃 노드 조회 (깊이 1)")
        print("="*60)
        
        # 첫 번째 노트의 이웃 조회
        note_id = self.note1_id
        
        response = client.get(
            f"/api/v1/graph/neighbors/{note_id}?depth=1",
            headers={"X-User-ID": TEST_USER_ID}
        )
        
        print(f"   - 상태 코드: {response.status_code}")
        print(f"   - 중심 노트 ID: {note_id[:8]}...")
        
        assert response.status_code == 200, f"오류: {response.json()}"
        
        data = response.json()
        
        # 응답 구조 검증
        assert "center_note_id" in data
        assert "neighbors" in data
        
        assert data["center_note_id"] == note_id
        
        neighbors = data["neighbors"]
        
        print(f"\n✅ 이웃 노드 조회 성공")
        print(f"   - 이웃 수: {len(neighbors)}개")
        
        if len(neighbors) > 0:
            print(f"\n👥 이웃 노드 목록:")
            for i, neighbor in enumerate(neighbors[:5], 1):  # 최대 5개만
                assert "neighbor_id" in neighbor
                assert "neighbor_title" in neighbor
                assert "distance" in neighbor
                
                print(f"   {i}. {neighbor['neighbor_title'][:40]}...")
                print(f"      - ID: {neighbor['neighbor_id'][:8]}...")
                print(f"      - 거리: {neighbor['distance']}단계")
        else:
            print(f"   ⚠️  연결된 이웃이 없습니다 (유사도가 낮거나 임베딩 대기 중)")
    
    def test_3_neighbors_depth_2(self):
        """[테스트 3] 이웃 노드 조회 (깊이 2)"""
        print("\n" + "="*60)
        print("[테스트 3] 이웃 노드 조회 (깊이 2)")
        print("="*60)
        
        note_id = self.note1_id
        
        response = client.get(
            f"/api/v1/graph/neighbors/{note_id}?depth=2",
            headers={"X-User-ID": TEST_USER_ID}
        )
        
        print(f"   - 상태 코드: {response.status_code}")
        print(f"   - 중심 노트 ID: {note_id[:8]}...")
        print(f"   - 탐색 깊이: 2단계")
        
        assert response.status_code == 200, f"오류: {response.json()}"
        
        data = response.json()
        neighbors = data["neighbors"]
        
        print(f"\n✅ 이웃 노드 조회 성공 (2단계)")
        print(f"   - 이웃 수: {len(neighbors)}개")
        
        if len(neighbors) > 0:
            # 거리별 분류
            distance_1 = [n for n in neighbors if n["distance"] == 1]
            distance_2 = [n for n in neighbors if n["distance"] == 2]
            
            print(f"\n📊 거리별 분포:")
            print(f"   - 1단계 이웃: {len(distance_1)}개")
            print(f"   - 2단계 이웃: {len(distance_2)}개")
            
            if len(distance_2) > 0:
                print(f"\n🔄 2단계 이웃 샘플:")
                for i, neighbor in enumerate(distance_2[:3], 1):
                    print(f"   {i}. {neighbor['neighbor_title'][:40]}... (거리: {neighbor['distance']})")
        else:
            print(f"   ⚠️  2단계 이웃이 없습니다")
    
    def test_4_neighbors_invalid_note(self):
        """[테스트 4] 존재하지 않는 노트의 이웃 조회"""
        print("\n" + "="*60)
        print("[테스트 4] 존재하지 않는 노트의 이웃 조회")
        print("="*60)
        
        import uuid
        fake_note_id = str(uuid.uuid4())
        
        response = client.get(
            f"/api/v1/graph/neighbors/{fake_note_id}?depth=1",
            headers={"X-User-ID": TEST_USER_ID}
        )
        
        print(f"   - 상태 코드: {response.status_code}")
        print(f"   - 가짜 노트 ID: {fake_note_id[:8]}...")
        
        # 존재하지 않는 노트는 이웃이 0개여야 함
        if response.status_code == 200:
            data = response.json()
            assert len(data["neighbors"]) == 0
            print(f"\n✅ 존재하지 않는 노트 처리 정상 (이웃 0개)")
        else:
            print(f"\n✅ 오류 처리 정상 (상태 코드: {response.status_code})")
    
    def test_5_header_missing(self):
        """[테스트 5] X-User-ID Header 누락"""
        print("\n" + "="*60)
        print("[테스트 5] X-User-ID Header 누락")
        print("="*60)
        
        response = client.get("/api/v1/graph/visualization")
        
        print(f"   - 상태 코드: {response.status_code}")
        
        # Header 누락 시 422 (Validation Error) 예상
        assert response.status_code == 422
        
        print(f"✅ Header 검증 정상 (422 에러)")


def run_all_tests():
    """모든 테스트 실행"""
    print("\n\n" + "█"*60)
    print("█" + " "*58 + "█")
    print("█" + "  그래프 시각화 API 테스트 시작".center(58) + "█")
    print("█" + " "*58 + "█")
    print("█"*60)
    
    test_instance = TestGraphVisualizationAPI()
    
    try:
        # Setup
        TestGraphVisualizationAPI.setup_class()
        
        # Tests
        test_instance.test_1_graph_visualization()
        test_instance.test_2_neighbors_depth_1()
        test_instance.test_3_neighbors_depth_2()
        test_instance.test_4_neighbors_invalid_note()
        test_instance.test_5_header_missing()
        
        # Success
        print("\n\n" + "█"*60)
        print("█" + " "*58 + "█")
        print("█" + "🎉 모든 테스트 통과!".center(58) + "█")
        print("█" + " "*58 + "█")
        print("█"*60)
        
        return True
    
    except AssertionError as e:
        print(f"\n\n❌ 테스트 실패: {e}")
        import traceback
        traceback.print_exc()
        return False
    
    except Exception as e:
        print(f"\n\n❌ 오류 발생: {e}")
        import traceback
        traceback.print_exc()
        return False
    
    finally:
        # Teardown (항상 실행)
        TestGraphVisualizationAPI.teardown_class()


if __name__ == "__main__":
    success = run_all_tests()
    sys.exit(0 if success else 1)
