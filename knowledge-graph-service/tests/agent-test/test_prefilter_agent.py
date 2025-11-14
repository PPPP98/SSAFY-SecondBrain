# tests/test_pre_filter_2way.py
import asyncio
from app.agents.search_agent.nodes import Nodes
from app.agents.search_agent.state import State

async def test_2way_pre_filter():
    """2-way Pre-Filter 테스트 (시간 값 확인)"""
    
    test_cases = [
        {
            "query": "어제 작성한 노트",
            "expected_type": "simple_lookup",
        },
        {
            "query": "오늘 쓴 거",
            "expected_type": "simple_lookup",
        },
        {
            "query": "AI 알고리즘",
            "expected_type": "similarity",
        },
        {
            "query": "어제 React Hook 사용법",
            "expected_type": "similarity",
        },
    ]
    
    print(f"\n{'='*80}")
    print(f"🧪 2-Way Pre-Filter 테스트 (상세)")
    print(f"{'='*80}\n")
    
    for i, test in enumerate(test_cases, 1):
        print(f"\n[{i}] {test['query']}")
        print(f"{'='*80}")
        
        state: State = {
            "query": test["query"],
            "user_id": 1,
            "authorizations": "Bearer test"
        }
        
        result = await Nodes.pre_filter_node(state)
        
        print(f"📌 원본: {result['original_query']}")
        print(f"🔀 타입: {result['search_type']}")
        
        # ✨ 시간 필터 상세 출력
        if result['filters'].get('timespan'):
            ts = result['filters']['timespan']
            print(f"📅 시간 필터:")
            print(f"   - 설명: {ts.get('description', 'N/A')}")
            print(f"   - 시작: {ts.get('start', 'N/A')}")  # ← 추가!
            print(f"   - 종료: {ts.get('end', 'N/A')}")    # ← 추가!
            
            # ✨ 값 검증
            if ts.get('start') and ts.get('end'):
                # ISO 8601 형식 확인
                try:
                    from datetime import datetime
                    datetime.fromisoformat(ts['start'].replace('+09:00', '+09:00'))
                    datetime.fromisoformat(ts['end'].replace('+09:00', '+09:00'))
                    print(f"   ✅ ISO 8601 형식 검증 통과")
                except Exception as e:
                    print(f"   ❌ ISO 8601 형식 오류: {e}")
            else:
                print(f"   ❌ start 또는 end 값이 없음!")
        
        if result['search_type'] == 'similarity':
            print(f"✏️  재작성: {result['query']}")
        
        # 검증
        if result['search_type'] == test['expected_type']:
            print(f"✅ 통과")
        else:
            print(f"❌ 실패")

if __name__ == "__main__":
    asyncio.run(test_2way_pre_filter())

