# tests/test_similarity_search.py
import asyncio
from app.agents.search_agent.nodes import Nodes
from app.agents.search_agent.state import State

async def test_similarity_search():
    """Similarity Search 노드 기본 테스트"""
    
    TEST_USER_ID = 1  # 실제 사용자 ID
    
    print("\n" + "="*80)
    print("🧪 Similarity Search 노드 테스트")
    print(f"테스트 사용자 ID: {TEST_USER_ID}")
    print("="*80 + "\n")
    
    test_cases = [
        {
            "name": "시간 필터 없음 - AI 검색",
            "state": {
                "user_id": TEST_USER_ID,
                "query": "인공지능 머신러닝 알고리즘",
                "original_query": "AI 알고리즘",
                "filters": {},
                "search_type": "similarity"
            }
        },
        {
            "name": "시간 필터 있음 - React 검색",
            "state": {
                "user_id": TEST_USER_ID,
                "query": "React Hooks 사용 방법과 패턴",
                "original_query": "어제 React Hook 사용법",
                "filters": {
                    "timespan": {
                        "start": "2025-11-13T00:00:00+09:00",
                        "end": "2025-11-13T23:59:59+09:00",
                        "description": "어제 (2025-11-13)"
                    }
                },
                "search_type": "similarity"
            }
        },
        {
            "name": "긴 쿼리 - 프론트엔드 최적화",
            "state": {
                "user_id": TEST_USER_ID,
                "query": "프론트엔드 성능 최적화 기법과 구현 전략",
                "original_query": "프론트엔드 성능 최적화 방법",
                "filters": {},
                "search_type": "similarity"
            }
        }
    ]
    
    passed = 0
    failed = 0
    
    for i, test in enumerate(test_cases, 1):
        print(f"\n[테스트 {i}/{len(test_cases)}] {test['name']}")
        print("="*80)
        
        try:
            # 노드 실행
            result = await Nodes.similarity_search_node(test["state"])
            
            documents = result.get("documents", [])
            
            # 결과 출력
            print(f"✅ 실행 성공")
            print(f"\n📊 결과:")
            print(f"  - 검색 쿼리: {test['state']['query']}")
            print(f"  - 검색된 노트: {len(documents)}개 (Top-3)")
            
            if test["state"]["filters"].get("timespan"):
                ts = test["state"]["filters"]["timespan"]
                print(f"\n📅 시간 필터:")
                print(f"  - 설명: {ts['description']}")
            
            if documents:
                print(f"\n📝 검색 결과:")
                for j, doc in enumerate(documents, 1):
                    print(f"  [{j}] {doc['title']}")
                    print(f"      ID: {doc['note_id']}")
                    print(f"      유사도: {doc['similarity_score']:.3f}")
                    print(f"      생성일: {doc.get('created_at', 'N/A')}")
            else:
                print(f"\n  ⚠️  검색 결과 없음")
            
            passed += 1
        
        except Exception as e:
            print(f"❌ 실행 실패")
            print(f"에러: {str(e)}")
            import traceback
            traceback.print_exc()
            failed += 1
    
    # 결과 요약
    print(f"\n{'='*80}")
    print(f"📊 테스트 결과")
    print(f"{'='*80}")
    print(f"✅ 성공: {passed}/{len(test_cases)}")
    print(f"❌ 실패: {failed}/{len(test_cases)}")
    
    return passed == len(test_cases)

if __name__ == "__main__":
    success = asyncio.run(test_similarity_search())
    exit(0 if success else 1)
