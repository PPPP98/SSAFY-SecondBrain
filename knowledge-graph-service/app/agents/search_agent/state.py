from typing_extensions import Optional, TypedDict, List, Dict


class State(TypedDict, total=False):
    """
    검색 Agent Graph 상태 정의
    ---

    """

    # === 사용자 정보 ===
    user_id: int
    authorizations: str
    # === 입력 ===
    query: str            # 정리된 질문
    original_query: str   # 원본 질문
    # === Pre-Filter ===
    filters: Dict[str, any] # {"timespan": {...}}
    search_type: str

    documents: List[Dict]

    # === similarity ===
    embeddings: List[float]
    neo4j_vector: List[Dict]
    neo4j_graph: List[Dict]
    deduplicated: List[Dict]
    sim_candidates: List[str]
    sim_documents: List[Dict]
    sim_top3: List[Dict]
    sim_filtered: List[Dict]

    # === result ===
    response: Optional[str]