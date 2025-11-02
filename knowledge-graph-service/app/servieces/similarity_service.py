from app.db.neo4j_client import neo4j_client
from app.core.config import get_settings
from app.core.constants import NoteConfig, VectorConfig, ErrorConfig
from typing import List, Dict
import logging

logger = logging.getLogger(__name__)
settings = get_settings()


class SimilarityService:
    """유사 노트 연결"""

    def __init__(self):
        """서비스 초기화"""
        # Constants에서 기본값
        self.default_similarity_limit = NoteConfig.DEFAULT_SIMILARITY_LIMIT
        self.max_similarity_limit = NoteConfig.MAX_SIMILARITY_LIMIT

        # Config에서 환경 설정값
        self.similarity_threshold = settings.similarity_threshold
        self.max_relationships = settings.max_relationships

        # Vector 관련
        self.vector_index_name = VectorConfig.INDEX_NAME
        self.embedding_dimension = VectorConfig.EMBEDDING_DIMENSION
        self.vector_search_limit = VectorConfig.VECTOR_SEARCH_LIMIT

        logger.info(
            f"🔧 SimilarityService 초기화: "
            f"threshold={self.similarity_threshold}, "
            f"max_relationships={self.max_relationships}"
        )

    def find_similar_notes(
        self,
        user_id: str,
        note_id: str,
        embedding: List[float],
        limit: int = None,
    ) -> List[Dict]:
        """
        유사 노트 찾기 (벡터 유사도 검색)

        Args:
            user_id: 사용자 ID
            note_id: 현재 노트 ID (제외 대상)
            embedding: 임베딩 벡터 (1536차원)
            limit: 최대 개수 (None이면 기본값, 상한선 적용)

        Returns:
            유사 노트 목록 [{note_id, title, similarity_score, created_at}, ...]

        Raises:
            Exception: 벡터 검색 실패
        """
        # 기본값 설정
        if limit is None:
            limit = self.default_similarity_limit

        # 상한선 제한
        if limit > self.max_similarity_limit:
            logger.warning(
                f"⚠️  limit 초과: {limit} > {self.max_similarity_limit}, "
                f"{self.max_similarity_limit}로 조정"
            )
            limit = self.max_similarity_limit

        query = f"""
        CALL db.index.vector.queryNodes('{self.vector_index_name}', $vector_limit, $embedding)
        YIELD node AS similar_note, score
        WHERE similar_note.user_id = $user_id
          AND similar_note.note_id <> $note_id
          AND score >= $threshold
        RETURN similar_note.note_id AS note_id,
               similar_note.title AS title,
               score AS similarity_score,
               similar_note.created_at AS created_at
        ORDER BY score DESC
        LIMIT $limit
        """

        with neo4j_client.get_session() as session:
            try:
                result = session.run(
                    query,
                    {
                        "user_id": user_id,
                        "note_id": note_id,
                        "embedding": embedding,
                        "vector_limit": self.vector_search_limit,  # 벡터 인덱스에서 먼저 max개 추출
                        "limit": limit,  # 그 중에서 limit개만 반환
                        "threshold": self.similarity_threshold,
                    },
                )

                similar_notes = [dict(record) for record in result]

                if NoteConfig.ENABLE_QUERY_LOGGING:
                    logger.info(
                        f"✅ 유사 노트 검색: {user_id} - {note_id} - "
                        f"{len(similar_notes)}개 발견 (threshold={self.similarity_threshold})"
                    )

                return similar_notes

            except Exception as e:
                logger.error(f"❌ 유사 노트 검색 실패: {e}")
                raise Exception(ErrorConfig.DATABASE_ERROR)

    def create_similarity_relationships(
        self,
        user_id: str,
        note_id: str,
        embedding: List[float],
    ) -> int:
        """
        유사 노트와 SIMILAR_TO 관계 생성

        최대 max_relationships개까지만 생성

        Args:
            user_id: 사용자 ID
            note_id: 현재 노트 ID
            embedding: 임베딩 벡터

        Returns:
            생성된 관계 개수

        Raises:
            Exception: 관계 생성 실패
        """
        # 1. max_relationships개까지 유사 노트 찾기
        similar_notes = self.find_similar_notes(
            user_id=user_id,
            note_id=note_id,
            embedding=embedding,
            limit=self.max_relationships,
        )

        if not similar_notes:
            logger.info(f"ℹ️  유사 노트 없음: {user_id} - {note_id}")
            return 0

        # 2. SIMILAR_TO 관계 생성 (쌍방향)
        count = 0
        for similar_note in similar_notes:
            try:
                query = """
                MATCH (n:Note {note_id: $note_id, user_id: $user_id})
                MATCH (similar:Note {note_id: $similar_note_id, user_id: $user_id})
                MERGE (n)-[r:SIMILAR_TO {score: $score}]-(similar)
                RETURN count(r) AS created
                """

                with neo4j_client.get_session() as session:
                    result = session.run(
                        query,
                        {
                            "user_id": user_id,
                            "note_id": note_id,
                            "similar_note_id": similar_note["note_id"],
                            "score": similar_note["similarity_score"],
                        },
                    )

                    record = result.single()
                    if record and record["created"] > 0:
                        count += 1

            except Exception as e:
                logger.warning(
                    f"⚠️  관계 생성 실패: {note_id} → {similar_note['note_id']}: {e}"
                )
                continue

        if NoteConfig.ENABLE_QUERY_LOGGING:
            logger.info(f"✅ 관계 생성: {user_id} - {note_id} - {count}개 관계")

        return count

    def delete_similarity_relationships(
        self,
        user_id: str,
        note_id: str,
    ) -> int:
        """
        유사 노트 관계 삭제 (노트 삭제 시 호출)

        Args:
            user_id: 사용자 ID
            note_id: 노트 ID

        Returns:
            삭제된 관계 개수

        Raises:
            Exception: 관계 삭제 실패
        """
        query = """
        MATCH (n:Note {note_id: $note_id, user_id: $user_id})-[r:SIMILAR_TO]-()
        DELETE r
        RETURN count(r) AS deleted
        """

        with neo4j_client.get_session() as session:
            try:
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
                    logger.info(f"✅ 관계 삭제: {user_id} - {note_id} - {deleted}개")

                return deleted

            except Exception as e:
                logger.error(f"❌ 관계 삭제 실패: {e}")
                raise Exception(ErrorConfig.DATABASE_ERROR)

    def get_related_notes_count(
        self,
        user_id: str,
        note_id: str,
    ) -> int:
        """
        특정 노트의 연결된 유사 노트 개수

        Args:
            user_id: 사용자 ID
            note_id: 노트 ID

        Returns:
            연결된 노트 개수

        Raises:
            Exception: 조회 실패
        """
        query = """
        MATCH (n:Note {note_id: $note_id, user_id: $user_id})-[r:SIMILAR_TO]-()
        RETURN count(DISTINCT r) AS count
        """

        with neo4j_client.get_session() as session:
            try:
                result = session.run(
                    query,
                    {
                        "user_id": user_id,
                        "note_id": note_id,
                    },
                )

                record = result.single()
                count = record["count"] if record else 0

                return count

            except Exception as e:
                logger.error(f"❌ 관계 개수 조회 실패: {e}")
                raise Exception(ErrorConfig.DATABASE_ERROR)

    def get_user_similarity_stats(self, user_id: str) -> Dict:
        """
        유저의 유사도 관계 통계

        Args:
            user_id: 사용자 ID

        Returns:
            통계 정보 {total_notes, total_relationships, avg_score}
        """
        query = """
        MATCH (n:Note {user_id: $user_id})
        OPTIONAL MATCH (n)-[r:SIMILAR_TO]-()
        WITH count(DISTINCT n) AS total_notes,
             count(DISTINCT r) AS total_rels,
             avg(r.score) AS avg_score
        RETURN total_notes,
               total_rels / 2 AS total_relationships,
               COALESCE(avg_score, 0.0) AS avg_score
        """

        with neo4j_client.get_session() as session:
            try:
                result = session.run(query, {"user_id": user_id})
                record = result.single()

                if record:
                    return {
                        "total_notes": record["total_notes"],
                        "total_relationships": int(record["total_relationships"]),
                        "avg_similarity_score": float(record["avg_score"]),
                    }

                return {
                    "total_notes": 0,
                    "total_relationships": 0,
                    "avg_similarity_score": 0.0,
                }

            except Exception as e:
                logger.error(f"❌ 통계 조회 실패: {e}")
                raise Exception(ErrorConfig.DATABASE_ERROR)


# 싱글톤 인스턴스
similarity_service = SimilarityService()
