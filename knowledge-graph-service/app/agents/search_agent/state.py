from typing_extensions import Optional, TypedDict, List, Dict


class State(TypedDict, total=False):
    """
    검색 Agent Graph 상태 정의
    ---
    상태 필드:
    - user_id: int
    - query: str
    - original_query: str
    - filters: Dict[str, any]
    - search_type: str
    - documents: List[Dict]
    - response: Optional[str]

    """

    # === 사용자 정보 ===
    user_id: int
    # === 입력 ===
    query: str            # 정리된 질문
    original_query: str   # 원본 질문
    # ======
    filters: Dict[str, any] # {"timespan": {...}}
    search_type: str
    documents: List[Dict]
    # === result ===
    response: Optional[str]