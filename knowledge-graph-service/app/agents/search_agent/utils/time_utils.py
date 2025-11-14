# app/utils/time_utils.py
from datetime import datetime
from zoneinfo import ZoneInfo
from typing import Dict

def get_korean_weekday(date: datetime) -> str:
    """요일을 한글로 반환"""
    weekdays = ["월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일"]
    return weekdays[date.weekday()]

def get_time_context() -> Dict[str, str]:
    """
    현재 시각 정보를 상세하게 구성
    
    Returns:
        Dict: 시간 컨텍스트 정보
    """
    kst = ZoneInfo("Asia/Seoul")
    now = datetime.now(kst)
    
    # ISO 8601 with timezone
    current_datetime = now.isoformat()
    
    # 한글 날짜 (2025년 11월 14일)
    current_date_full = now.strftime("%Y년 %m월 %d일")
    
    # 한글 시간 (오후 12시 23분)
    hour_12 = now.hour if now.hour <= 12 else now.hour - 12
    if hour_12 == 0:
        hour_12 = 12
    am_pm = "오전" if now.hour < 12 else "오후"
    current_time_full = f"{am_pm} {hour_12}시 {now.minute:02d}분"
    
    # 요일 (목요일)
    weekday_korean = get_korean_weekday(now)
    
    # 주차 (46주차)
    week_number = now.isocalendar()[1]
    
    # 시간대
    timezone_info = "Asia/Seoul (KST, UTC+9)"
    
    return {
        "current_datetime": current_datetime,
        "current_date_full": current_date_full,
        "current_time_full": current_time_full,
        "weekday_korean": weekday_korean,
        "week_number": week_number,
        "timezone": timezone_info
    }
