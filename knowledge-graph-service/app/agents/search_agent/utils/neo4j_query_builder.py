from typing import Dict, Optional, List, Tuple


def build_time_filter_cypher(
    user_id: int,
    timespan: Optional[Dict[str, str]] = None,
    limit: int = 10,
) -> tuple[str, Dict]:
    """
    시간 필터 기반 Cypher 쿼리 생성

    Args:
        user_id: 사용자 ID
        timespan: {"start": "ISO", "end": "ISO", "description": "..."}
        limit: 반환할 노트 개수

    Returns:
        (cypher_query, parameters)
    """

    # 기본 쿼리
    cypher = """
    MATCH (n:Note)
    WHERE n.user_id = $user_id
    """

    params = {"user_id": user_id, "limit": limit}

    # 시간 필터 추가
    if timespan:
        if timespan.get("start"):
            cypher += "\n  AND n.created_at >= datetime($start)"
            params["start"] = timespan["start"]

        if timespan.get("end"):
            cypher += "\n  AND n.created_at <= datetime($end)"
            params["end"] = timespan["end"]

    # 정렬 및 제한
    cypher += """
    RETURN n.note_id AS note_id,
           n.title AS title,
           n.created_at AS created_at,
           n.updated_at AS updated_at
    ORDER BY n.created_at DESC
    LIMIT $limit
    """

    return cypher, params


def build_similarity_search_cypher(
    embedding: List[float],
    user_id: int,
    timespan: Optional[Dict[str, str]] = None,
    limit: int = 10,
) -> Tuple[str, Dict]:
    """
    벡터 유사도 검색 Cypher 쿼리 생성 (Similarity Search용)
    
    Args:
        embedding: 쿼리 임베딩 벡터
        user_id: 사용자 ID
        timespan: 시간 필터 (선택)
        limit: 반환할 노트 개수 (기본 3)
    
    Returns:
        (cypher_query, parameters)
    """
    
    cypher = """
    CALL db.index.vector.queryNodes(
        'note_embeddings',
        $limit,
        $embedding
    )
    YIELD node, score
    WHERE node.user_id = $user_id
    """
    
    params = {
        "embedding": embedding,
        "user_id": user_id,
        "limit": limit
    }
    
    # 시간 필터 추가
    if timespan:
        if timespan.get("start"):
            cypher += "\n  AND node.created_at >= datetime($start)"
            params["start"] = timespan["start"]
        
        if timespan.get("end"):
            cypher += "\n  AND node.created_at <= datetime($end)"
            params["end"] = timespan["end"]
    
    # 결과 반환
    cypher += """
    RETURN node.note_id AS note_id,
           node.title AS title,
           node.created_at AS created_at,
           node.updated_at AS updated_at,
           score AS similarity_score
    ORDER BY score DESC
    """
    
    return cypher, params