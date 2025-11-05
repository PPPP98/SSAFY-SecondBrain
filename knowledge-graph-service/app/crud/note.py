from app.db.neo4j_client import neo4j_client
from typing import Optional, List, Dict
import logging
from app.core.config import get_settings
from app.core.constants import NoteConfig, ValidationConfig


settings = get_settings()

logger = logging.getLogger(__name__)


# ===== 헬퍼 함수 =====
def _convert_datetime(record: Dict) -> Dict:
    """Neo4j DateTime을 ISO format string으로 변환"""
    if record.get("created_at"):
        record["created_at"] = record["created_at"].iso_format()

    if record.get("updated_at"):
        record["updated_at"] = record["updated_at"].iso_format()

    return record


# ===== 노트 생성 =====
def create_note(
    note_id: str,
    user_id: str,
    title: str,
    embedding: List[float],
) -> str:
    """
    노트 생성 (note_id, user_id는 Spring Boot에서 제공)

    Args:
        note_id: 노트 ID
        user_id: 사용자 ID
        title: 노트 제목
        embedding: 임베딩 벡터 (1536차원)

    Returns:
        생성된 노트 ID (입력받은 것 그대로)
    """
    query = """
    CREATE (n:Note {
        note_id: $note_id,
        user_id: $user_id,
        title: $title,
        embedding: $embedding,
        created_at: datetime(),
        updated_at: datetime()
    })
    RETURN n.note_id AS note_id
    """

    with neo4j_client.get_session() as session:
        result = session.run(
            query,
            {
                "note_id": note_id,
                "user_id": user_id,
                "title": title,
                "embedding": embedding,
            },
        )

        result.single()  # 결과 소비
        logger.debug(f"✅ 노트 생성: {user_id} - {note_id} - {title}")
        return note_id


# ===== 노트 조회 =====
def get_note(
    user_id: str,
    note_id: str,
) -> Optional[Dict]:
    """
    노트 조회 (유저별)

    Args:
        user_id: 사용자 ID
        note_id: 노트 ID

    Returns:
        노트 정보 또는 None
    """
    query = """
    MATCH (n:Note {note_id: $note_id, user_id: $user_id})
    RETURN n.note_id AS note_id,
           n.user_id AS user_id,
           n.title AS title,
           n.created_at AS created_at,
           n.updated_at AS updated_at
    """

    with neo4j_client.get_session() as session:
        result = session.run(
            query,
            {
                "user_id": user_id,
                "note_id": note_id,
            },
        )
        record = result.single()

        if record:
            note_dict = dict(record)
            note_dict = _convert_datetime(note_dict)  
            logger.debug(f"✅ 노트 조회: {user_id} - {note_id}")
            return note_dict

        logger.warning(f"⚠️ 노트 없음: {user_id} - {note_id}")
        return None


# ===== 노트 목록 조회 =====
def get_all_notes(
    user_id: str,
    limit: Optional[int] = None,
    skip: int = 0,
) -> tuple[List[Dict], int]:
    """
    모든 노트 조회 (페이지네이션)

    Args:
        user_id: 사용자 ID
        limit: 최대 개수 (None이면 기본값)
        skip: 건너뛸 개수

    Returns:
        (노트 목록, 전체 개수)
    """
    # 기본값 설정
    if limit is None:
        limit = NoteConfig.DEFAULT_PAGE_LIMIT

    # 상한선 제한
    if limit > NoteConfig.MAX_PAGE_LIMIT:
        limit = NoteConfig.MAX_PAGE_LIMIT

    if skip < 0:
        skip = 0

    count_query = """
    MATCH (n:Note {user_id: $user_id})
    RETURN count(n) AS total
    """

    query = """
    MATCH (n:Note {user_id: $user_id})
    RETURN n.note_id AS note_id,
           n.user_id AS user_id,
           n.title AS title,
           n.created_at AS created_at,
           n.updated_at AS updated_at
    ORDER BY n.created_at DESC
    SKIP $skip
    LIMIT $limit
    """

    with neo4j_client.get_session() as session:
        count_result = session.run(count_query, {"user_id": user_id})
        total = count_result.single()["total"]

        result = session.run(
            query,
            {
                "user_id": user_id,
                "limit": limit,
                "skip": skip,
            },
        )
        # 👇 DateTime 변환 추가
        notes = [_convert_datetime(dict(record)) for record in result]

        if NoteConfig.ENABLE_QUERY_LOGGING:
            logger.debug(
                f"✅ 노트 목록 조회: {user_id} - {len(notes)}개 (전체: {total})"
            )

        return notes, total


# ===== 노트 삭제 =====
def delete_note(
    user_id: str,
    note_id: str,
) -> bool:
    """
    노트 삭제 (관계 포함, 유저별)

    Args:
        user_id: 사용자 ID
        note_id: 노트 ID

    Returns:
        삭제 성공 여부
    """
    query = """
    MATCH (n:Note {note_id: $note_id, user_id: $user_id})
    DETACH DELETE n
    RETURN count(n) AS deleted
    """

    with neo4j_client.get_session() as session:
        result = session.run(
            query,
            {
                "user_id": user_id,
                "note_id": note_id,
            },
        )
        record = result.single()

        deleted = record["deleted"] if record else 0
        if deleted > 0:
            logger.debug(f"✅ 노트 삭제: {user_id} - {note_id}")
            return True

        logger.warning(f"⚠️ 삭제 실패 (노트 없음): {user_id} - {note_id}")
        return False


# ===== 유사 노트 조회 =====
def get_similar_notes(
    user_id: str,
    note_id: str,
) -> List[Dict]:
    """
    유사 노트 조회 (같은 유저만, 유저별)

    Args:
        user_id: 사용자 ID
        note_id: 노트 ID

    Returns:
        유사 노트 목록
    """
    # 연결할 노트 최대 개수 불러오기
    limit = settings.max_relationships

    query = """
    MATCH (n:Note {note_id: $note_id, user_id: $user_id})
    MATCH (n)-[r:SIMILAR_TO]-(similar:Note {user_id: $user_id})
    RETURN similar.note_id AS note_id,
           similar.title AS title,
           r.score AS similarity_score,
           similar.created_at AS created_at
    ORDER BY r.score DESC
    LIMIT $limit
    """

    with neo4j_client.get_session() as session:
        result = session.run(
            query,
            {
                "user_id": user_id,
                "note_id": note_id,
                "limit": limit,
            },
        )
        similar_notes = [_convert_datetime(dict(record)) for record in result]
        logger.debug(
            f"✅ 유사 노트 조회: {user_id} - {note_id} - {len(similar_notes)}개"
        )
        return similar_notes


# ===== 통계 조회 =====
def get_stats(
    user_id: str,
) -> Dict:
    """
    그래프 통계 (유저별)

    Args:
        user_id: 사용자 ID

    Returns:
        통계 정보
    """
    query = """
    MATCH (n:Note {user_id: $user_id})
    OPTIONAL MATCH (n)-[r:SIMILAR_TO]-()
    WITH count(DISTINCT n) AS total_notes,
         count(DISTINCT r) AS total_rels
    RETURN total_notes,
           total_rels / 2 AS total_relationships,
           CASE 
             WHEN total_notes > 0 
             THEN toFloat(total_rels) / total_notes 
             ELSE 0.0 
           END AS avg_connections
    """

    with neo4j_client.get_session() as session:
        result = session.run(query, {"user_id": user_id})
        record = result.single()

        if record:
            stats = {
                "user_id": user_id,
                "total_notes": record["total_notes"],
                "total_relationships": int(record["total_relationships"]),
                "avg_connections": float(record["avg_connections"]),
            }
            logger.debug(f"✅ 통계 조회: {user_id}")
            return stats

        return {
            "user_id": user_id,
            "total_notes": 0,
            "total_relationships": 0,
            "avg_connections": 0.0,
        }


# ===== 노트 개수 =====
def count_user_notes(user_id: str) -> int:
    """
    유저의 총 노트 개수 조회

    Args:
        user_id: 사용자 ID

    Returns:
        노트 개수
    """
    query = """
    MATCH (n:Note {user_id: $user_id})
    RETURN count(n) AS total
    """

    with neo4j_client.get_session() as session:
        result = session.run(query, {"user_id": user_id})
        record = result.single()
        total = record["total"] if record else 0
        logger.debug(f"✅ 노트 개수 조회: {user_id} - {total}개")
        return total


# ===== 제목 검색 =====
def get_note_by_title(
    user_id: str,
    title: str,
    limit: Optional[int] = None,
) -> List[Dict]:
    """
    제목으로 노트 검색

    Args:
        user_id: 사용자 ID
        title: 검색 제목
        limit: 최대 개수 (None이면 기본값)

    Returns:
        노트 목록
    """
    if limit is None:
        limit = NoteConfig.DEFAULT_SEARCH_LIMIT

    if limit > NoteConfig.MAX_SEARCH_LIMIT:
        limit = NoteConfig.MAX_SEARCH_LIMIT

    query = """
    MATCH (n:Note {user_id: $user_id})
    WHERE n.title CONTAINS $title
    RETURN n.note_id AS note_id,
           n.user_id AS user_id,
           n.title AS title,
           n.created_at AS created_at,
           n.updated_at AS updated_at
    ORDER BY n.created_at DESC
    LIMIT $limit
    """

    with neo4j_client.get_session() as session:
        result = session.run(
            query,
            {
                "user_id": user_id,
                "title": title,
                "limit": limit,
            },
        )
        notes = [_convert_datetime(dict(record)) for record in result]

        if NoteConfig.ENABLE_QUERY_LOGGING:
            logger.debug(f"✅ 제목 검색: {user_id} - '{title}' - {len(notes)}개")

        return notes

# 2025/11/05 update for CRUD worker
# ===== 노트 수정 =====
def update_note(
    user_id: str,
    note_id: str,
    title: Optional[str] = None,
    embedding: Optional[List[float]] = None,
) -> bool:
    """
    노트 수정 (제목 또는 임베딩)
    
    Args:
        user_id: 사용자 ID
        note_id: 노트 ID
        title: 새 제목 (None이면 변경 안 함)
        embedding: 새 임베딩 벡터 (None이면 변경 안 함)
    
    Returns:
        수정 성공 여부
    """
    # SET 절 동적 생성
    set_clauses = ["n.updated_at = datetime()"]
    params = {
        "user_id": user_id,
        "note_id": note_id,
    }
    
    if title is not None:
        set_clauses.append("n.title = $title")
        params["title"] = title
    
    if embedding is not None:
        set_clauses.append("n.embedding = $embedding")
        params["embedding"] = embedding
    
    query = f"""
    MATCH (n:Note {{note_id: $note_id, user_id: $user_id}})
    SET {', '.join(set_clauses)}
    RETURN count(n) AS updated
    """
    
    with neo4j_client.get_session() as session:
        result = session.run(query, params)
        record = result.single()
        
        updated = record["updated"] if record else 0
        if updated > 0:
            logger.debug(f"✅ 노트 수정: {user_id} - {note_id}")
            return True
        
        logger.warning(f"⚠️  수정 실패 (노트 없음): {user_id} - {note_id}")
        return False


# ===== 유사도 관계 삭제 =====
def delete_relationships(
    user_id: str,
    note_id: str,
) -> int:
    """
    특정 노트의 모든 SIMILAR_TO 관계 삭제
    
    Args:
        user_id: 사용자 ID
        note_id: 노트 ID
    
    Returns:
        삭제된 관계 개수
    """
    query = """
    MATCH (n:Note {note_id: $note_id, user_id: $user_id})
    MATCH (n)-[r:SIMILAR_TO]-()
    DELETE r
    RETURN count(r) AS deleted
    """
    
    with neo4j_client.get_session() as session:
        result = session.run(
            query,
            {
                "user_id": user_id,
                "note_id": note_id,
            },
        )
        record = result.single()
        
        deleted = record["deleted"] if record else 0
        logger.debug(f"✅ 관계 삭제: {user_id} - {note_id} - {deleted}개")
        return deleted
