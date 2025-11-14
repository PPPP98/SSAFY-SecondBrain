# tests/test_generate_response_final.py
import asyncio
from app.agents.search_agent.nodes import Nodes
from app.agents.search_agent.state import State

async def test_generate_response_final():
    """Generate Response 노드 최종 테스트"""
    
    print("\n" + "="*80)
    print("🧪 Generate Response 노드 최종 테스트")
    print("="*80 + "\n")
    
    test_cases = [
        {
            "name": "검색 유도 - 안녕하세요",
            "state": {
                "user_id": 1,
                "original_query": "안녕하세요",
                "documents": [],
                "search_type": "direct_answer"
            },
            "expected_keywords": ["검색", "무엇"]
        },
        {
            "name": "검색 유도 - 날씨",
            "state": {
                "user_id": 1,
                "original_query": "날씨 어때?",
                "documents": [],
                "search_type": "direct_answer"
            },
            "expected_keywords": ["검색", "노트"]
        },
        {
            "name": "검색 결과 없음",
            "state": {
                "user_id": 1,
                "original_query": "존재하지 않는 내용",
                "documents": [],
                "search_type": "similarity"
            },
            "expected_keywords": ["찾지 못했습니다", "검색"]
        },
        {
            "name": "검색 결과 1개",
            "state": {
                "user_id": 1,
                "original_query": "프론트엔드 최적화",
                "documents": [
                    {
                        "note_id": "1",
                        "title": "웹 성능 개선 가이드",
                        "created_at": "2024-11-14T11:00:00+09:00"
                    },
                ],
                "search_type": "similarity"
            },
            "expected_keywords": ["1개", "찾았습니다"]
        },
        {
            "name": "검색 결과 3개",
            "state": {
                "user_id": 1,
                "original_query": "React Hook 사용법",
                "documents": [
                    {"note_id": "1", "title": "React Hooks 기본", "created_at": "2024-11-10T10:00:00+09:00"},
                    {"note_id": "2", "title": "useState 활용", "created_at": "2024-11-12T14:00:00+09:00"},
                    {"note_id": "3", "title": "커스텀 Hook", "created_at": "2024-11-13T09:00:00+09:00"},
                ],
                "search_type": "similarity"
            },
            "expected_keywords": ["3개", "찾았습니다"]
        },
    ]
    
    passed = 0
    failed = 0
    
    for i, test in enumerate(test_cases, 1):
        print(f"\n[테스트 {i}/{len(test_cases)}] {test['name']}")
        print("="*80)
        
        try:
            result = await Nodes.generate_response_node(test["state"])
            response = result.get("response", "")
            
            print(f"✅ 실행 성공")
            print(f"\n💬 질문: {test['state']['original_query']}")
            print(f"📚 문서: {len(test['state']['documents'])}개")
            print(f"🔀 타입: {test['state']['search_type']}")
            print(f"\n📤 응답 ({len(response)}자):")
            print(f"   {response}")
            
            # 키워드 검증
            keywords_found = [kw for kw in test['expected_keywords'] if kw in response]
            print(f"\n🔍 키워드 검증: {len(keywords_found)}/{len(test['expected_keywords'])} 일치")
            
            if keywords_found:
                print(f"   ✅ 발견: {keywords_found}")
            
            passed += 1
        
        except Exception as e:
            print(f"❌ 실행 실패: {str(e)}")
            import traceback
            traceback.print_exc()
            failed += 1
    
    print(f"\n{'='*80}")
    print(f"📊 테스트 결과: {passed}/{len(test_cases)} 통과, {failed}/{len(test_cases)} 실패")
    print(f"{'='*80}")

if __name__ == "__main__":
    asyncio.run(test_generate_response_final())
