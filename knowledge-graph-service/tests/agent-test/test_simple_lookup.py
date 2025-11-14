# tests/test_simple_lookup.py
import asyncio
from app.agents.search_agent.nodes import Nodes
from app.agents.search_agent.state import State

async def test_simple_lookup():
    """Simple Lookup 노드 기본 테스트"""
    
    # 실제 user_id 사용
    TEST_USER_ID = 1  # ← 실제 사용자 ID로 변경
    
    print("\n" + "="*80)
    print("🧪 Simple Lookup 노드 테스트")
    print(f"테스트 사용자 ID: {TEST_USER_ID}")
    print("="*80 + "\n")
    
    test_cases = [
        {
            "name": "시간 필터 있음 (어제)",
            "state": {
                "user_id": TEST_USER_ID,
                "query": "",
                "original_query": "어제 작성한 노트",
                "filters": {
                    "timespan": {
                        "start": "2025-11-13T00:00:00+09:00",
                        "end": "2025-11-13T23:59:59+09:00",
                        "description": "어제 (2025-11-13)"
                    }
                },
                "search_type": "simple_lookup"
            }
        },
        {
            "name": "시간 필터 있음 (오늘)",
            "state": {
                "user_id": TEST_USER_ID,
                "query": "",
                "original_query": "오늘 작성한 노트",
                "filters": {
                    "timespan": {
                        "start": "2025-11-14T00:00:00+09:00",
                        "end": "2025-11-14T23:59:59+09:00",
                        "description": "오늘 (2025-11-14)"
                    }
                },
                "search_type": "simple_lookup"
            }
        },
        {
            "name": "시간 필터 있음 (이번 주)",
            "state": {
                "user_id": TEST_USER_ID,
                "query": "",
                "original_query": "이번 주 메모",
                "filters": {
                    "timespan": {
                        "start": "2025-11-11T00:00:00+09:00",
                        "end": "2025-11-14T23:59:59+09:00",
                        "description": "이번 주 (11월 11일~14일)"
                    }
                },
                "search_type": "simple_lookup"
            }
        },
        {
            "name": "시간 필터 없음 (최근 10개)",
            "state": {
                "user_id": TEST_USER_ID,
                "query": "",
                "original_query": "최근 노트",
                "filters": {},
                "search_type": "simple_lookup"
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
            result = await Nodes.simple_lookup_node(test["state"])
            
            documents = result.get("documents", [])
            
            # 결과 출력
            print(f"✅ 실행 성공")
            print(f"\n📊 결과:")
            print(f"  - 검색된 노트: {len(documents)}개")
            print(f"  - 최대 제한: 10개")
            
            if test["state"]["filters"].get("timespan"):
                ts = test["state"]["filters"]["timespan"]
                print(f"\n📅 시간 필터:")
                print(f"  - 설명: {ts['description']}")
                print(f"  - 시작: {ts['start']}")
                print(f"  - 종료: {ts['end']}")
            
            if documents:
                print(f"\n📝 노트 목록:")
                for j, doc in enumerate(documents, 1):
                    print(f"  [{j}] {doc['note_id']}")
                    print(f"      제목: {doc['title']}")
                    print(f"      생성일: {doc.get('created_at', 'N/A')}")
                    if j >= 5:
                        print(f"  ... 외 {len(documents) - 5}개")
                        break
            else:
                print(f"\n  ⚠️  검색 결과 없음 (데이터가 없거나 조건에 맞지 않음)")
            
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
    success = asyncio.run(test_simple_lookup())
    exit(0 if success else 1)
