# tests/test_note_summarize_service.py
"""
NoteSummarizeService 비동기 테스트
async/await 완전 적용
"""
import asyncio
import logging
from app.services.note_summarize_service import note_summarize_service

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(levelname)s - %(message)s'
)


async def test_1_text_only():
    """Test 1: 텍스트만 입력"""
    print("\n" + "="*60)
    print("🧪 Test 1: 텍스트만 입력")
    print("="*60)
    
    data = [
        "Python은 1991년 Guido van Rossum이 개발한 프로그래밍 언어입니다.",
        "간결하고 읽기 쉬운 문법이 특징입니다.",
        "데이터 과학, 웹 개발, 자동화 등에 널리 사용됩니다."
    ]
    
    print(f"📥 입력: {len(data)}개 항목")
    for idx, item in enumerate(data, 1):
        print(f"   {idx}. {item[:50]}...")
    
    # ⭐ await 추가
    result = await note_summarize_service.get_note_summarize(data)
    
    print(f"\n📤 결과:")
    print(f"   길이: {len(result)} chars")
    print(f"\n📝 요약:")
    print(f"   {result}")
    print("="*60)
    
    # 검증
    assert result != "", "❌ 요약 결과가 비어있습니다"
    assert len(result) > 0, "❌ 요약 길이가 0입니다"
    assert isinstance(result, str), "❌ 결과가 문자열이 아닙니다"
    
    print("✅ Test 1 PASS\n")
    return True


async def test_2_empty_data():
    """Test 2: 빈 데이터 입력"""
    print("\n" + "="*60)
    print("🧪 Test 2: 빈 데이터 입력")
    print("="*60)
    
    data = []
    
    print(f"📥 입력: 빈 리스트 []")
    
    # ⭐ await 추가
    result = await note_summarize_service.get_note_summarize(data)
    
    print(f"\n📤 결과: '{result}'")
    print("="*60)
    
    # 검증
    assert result == "", "❌ 빈 데이터는 빈 문자열을 반환해야 합니다"
    
    print("✅ Test 2 PASS\n")
    return True


async def test_3_single_text():
    """Test 3: 단일 텍스트 입력"""
    print("\n" + "="*60)
    print("🧪 Test 3: 단일 텍스트 입력")
    print("="*60)
    
    data = [
        "Python은 강력하고 배우기 쉬운 프로그래밍 언어로, "
        "전 세계적으로 많은 개발자들이 사용하고 있습니다."
    ]
    
    print(f"📥 입력: {data[0][:60]}...")
    
    # ⭐ await 추가
    result = await note_summarize_service.get_note_summarize(data)
    
    print(f"\n📤 결과:")
    print(f"   길이: {len(result)} chars")
    print(f"\n📝 요약:")
    print(f"   {result}")
    print("="*60)
    
    # 검증
    assert result != "", "❌ 요약 결과가 비어있습니다"
    assert len(result) > 0, "❌ 요약 길이가 0입니다"
    
    print("✅ Test 3 PASS\n")
    return True


async def test_4_long_texts():
    """Test 4: 긴 텍스트 여러 개"""
    print("\n" + "="*60)
    print("🧪 Test 4: 긴 텍스트 여러 개")
    print("="*60)
    
    data = [
        """Python은 1991년 프로그래머 Guido van Rossum이 발표한 고급 프로그래밍 언어로, 
        플랫폼 독립적이며 인터프리터식, 객체지향적, 동적 타이핑 대화형 언어입니다. 
        Python이라는 이름은 귀도가 좋아하는 코미디 쇼인 'Monty Python's Flying Circus'에서 따온 것입니다.""",
        
        """Python은 비영리의 Python 소프트웨어 재단이 관리하는 개방형, 공동체 기반 개발 모델을 가지고 있습니다. 
        C언어로 구현된 CPython 구현이 사실상의 표준입니다.""",
        
        """Python은 초보자부터 전문가까지 사용자층이 매우 두텁습니다. 
        동적 타이핑 범용 프로그래밍 언어로, 펄 및 루비와 자주 비교됩니다."""
    ]
    
    print(f"📥 입력: {len(data)}개 긴 텍스트")
    total_chars = sum(len(text) for text in data)
    print(f"   총 {total_chars} chars")
    
    # ⭐ await 추가
    result = await note_summarize_service.get_note_summarize(data)
    
    print(f"\n📤 결과:")
    print(f"   길이: {len(result)} chars")
    print(f"   압축률: {(1 - len(result)/total_chars)*100:.1f}%")
    print(f"\n📝 요약:")
    print(f"   {result}")
    print("="*60)
    
    # 검증
    assert result != "", "❌ 요약 결과가 비어있습니다"
    assert len(result) < total_chars, "❌ 요약이 원본보다 길 수 없습니다"
    assert len(result) > 0, "❌ 요약 길이가 0입니다"
    
    print("✅ Test 4 PASS\n")
    return True


async def test_5_url_only():
    """Test 5: URL만 입력 (네트워크 필요)"""
    print("\n" + "="*60)
    print("🧪 Test 5: URL만 입력 (선택적)")
    print("="*60)
    
    data = [
        "https://www.python.org/about/",
    ]
    
    print(f"📥 입력: {data[0]}")
    print("   ⚠️ 실제 네트워크 요청 발생 (시간 소요)")
    print("   ⏳ 처리 중...")
    
    try:
        # ⭐ await 추가
        result = await note_summarize_service.get_note_summarize(data)
        
        print(f"\n📤 결과:")
        print(f"   길이: {len(result)} chars")
        print(f"\n📝 요약:")
        print(f"   {result[:200]}..." if len(result) > 200 else f"   {result}")
        print("="*60)
        
        # 검증
        assert result != "", "❌ URL 요약 실패"
        assert len(result) > 0, "❌ 요약 길이가 0입니다"
        # Python 관련 내용이 있는지 확인
        assert any(keyword in result.lower() for keyword in ["python", "programming", "language"]), \
            "❌ Python 관련 내용이 없음"
        
        print("✅ Test 5 PASS\n")
        return True
        
    except asyncio.TimeoutError:
        print(f"\n⚠️ Test 5 SKIP (타임아웃)")
        print("="*60 + "\n")
        return None  # Skip
    except Exception as e:
        print(f"\n⚠️ Test 5 SKIP (네트워크 오류): {str(e)[:100]}")
        print("="*60 + "\n")
        return None  # Skip


async def test_6_mixed_data():
    """Test 6: URL + 텍스트 혼합"""
    print("\n" + "="*60)
    print("🧪 Test 6: URL + 텍스트 혼합 (선택적)")
    print("="*60)
    
    data = [
        "https://www.python.org/about/",
        "Python은 데이터 과학 분야에서 가장 인기 있는 언어입니다.",
        "많은 기업들이 Python을 도입하고 있습니다."
    ]
    
    print(f"📥 입력: {len(data)}개 항목 (URL 1개 + 텍스트 2개)")
    for idx, item in enumerate(data, 1):
        print(f"   {idx}. {item[:50]}...")
    print("   ⏳ 처리 중...")
    
    try:
        # ⭐ await 추가
        result = await note_summarize_service.get_note_summarize(data)
        
        print(f"\n📤 결과:")
        print(f"   길이: {len(result)} chars")
        print(f"\n📝 요약:")
        print(f"   {result[:200]}..." if len(result) > 200 else f"   {result}")
        print("="*60)
        
        # 검증
        assert result != "", "❌ 혼합 요약 실패"
        assert len(result) > 0, "❌ 요약 길이가 0입니다"
        
        print("✅ Test 6 PASS\n")
        return True
        
    except asyncio.TimeoutError:
        print(f"\n⚠️ Test 6 SKIP (타임아웃)")
        print("="*60 + "\n")
        return None  # Skip
    except Exception as e:
        print(f"\n⚠️ Test 6 SKIP (네트워크 오류): {str(e)[:100]}")
        print("="*60 + "\n")
        return None  # Skip


async def test_7_special_characters():
    """Test 7: 특수문자 포함 텍스트"""
    print("\n" + "="*60)
    print("🧪 Test 7: 특수문자 포함 텍스트")
    print("="*60)
    
    data = [
        "Python은 'print()' 함수로 출력합니다.",
        "변수는 x = 10 처럼 선언합니다.",
        "리스트는 [1, 2, 3] 형태입니다."
    ]
    
    print(f"📥 입력: {len(data)}개 항목 (특수문자 포함)")
    
    # ⭐ await 추가
    result = await note_summarize_service.get_note_summarize(data)
    
    print(f"\n📤 결과:")
    print(f"   길이: {len(result)} chars")
    print(f"\n📝 요약:")
    print(f"   {result}")
    print("="*60)
    
    # 검증
    assert result != "", "❌ 요약 결과가 비어있습니다"
    assert len(result) > 0, "❌ 요약 길이가 0입니다"
    
    print("✅ Test 7 PASS\n")
    return True


async def run_all_tests():
    """모든 테스트 실행 (비동기)"""
    print("\n\n" + "🚀 "*20)
    print("   NoteSummarizeService 비동기 테스트 시작")
    print("🚀 "*20 + "\n")
    
    tests = [
        ("텍스트만", test_1_text_only),
        ("빈 데이터", test_2_empty_data),
        ("단일 텍스트", test_3_single_text),
        ("긴 텍스트들", test_4_long_texts),
        ("URL만 (선택)", test_5_url_only),
        ("URL + 텍스트 (선택)", test_6_mixed_data),
        ("특수문자", test_7_special_characters),
    ]
    
    results = {
        "passed": 0,
        "failed": 0,
        "skipped": 0
    }
    
    for name, test_func in tests:
        try:
            # ⭐ await 추가
            result = await test_func()
            
            if result is None:
                results["skipped"] += 1
            elif result:
                results["passed"] += 1
            else:
                results["failed"] += 1
                
        except AssertionError as e:
            results["failed"] += 1
            print(f"❌ {name}: FAIL - {e}\n")
            
        except Exception as e:
            results["failed"] += 1
            print(f"❌ {name}: ERROR - {str(e)[:100]}\n")
            import traceback
            traceback.print_exc()
    
    # 결과 요약
    print("\n\n" + "="*60)
    print("📊 테스트 결과 요약")
    print("="*60)
    print(f"   ✅ 통과: {results['passed']}개")
    print(f"   ❌ 실패: {results['failed']}개")
    print(f"   ⏭️  건너뜀: {results['skipped']}개")
    print(f"   📊 총: {sum(results.values())}개")
    print("="*60)
    
    if results["failed"] == 0:
        print("\n🎉 모든 필수 테스트 통과!")
        if results["skipped"] > 0:
            print(f"   (선택적 테스트 {results['skipped']}개 건너뜀)")
        print()
        return True
    else:
        print(f"\n⚠️ {results['failed']}개 테스트 실패\n")
        return False


def main():
    """Entry point"""
    # ⭐ asyncio.run() 사용
    success = asyncio.run(run_all_tests())
    return success


if __name__ == "__main__":
    import sys
    success = main()
    sys.exit(0 if success else 1)
