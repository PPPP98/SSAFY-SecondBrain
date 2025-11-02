from app.db.neo4j_client import neo4j_client
import logging

logger = logging.getLogger(__name__)


def initialize_schema() -> bool:
    """Neo4j 스키마 초기화"""

    queries = [
        # 1. 제약조건: note_id는 고유해야 함
        """
        CREATE CONSTRAINT note_id_unique IF NOT EXISTS
        FOR (n:Note) REQUIRE n.note_id IS UNIQUE
        """,
        # 2. 인덱스: user_id (유저별 노트 검색용)
        """
        CREATE INDEX note_user_id IF NOT EXISTS
        FOR (n:Note) ON (n.user_id)
        """,
        # 3. 인덱스: created_at (시간순 정렬용)
        """
        CREATE INDEX note_created_at IF NOT EXISTS
        FOR (n:Note) ON (n.created_at)
        """,
        # 4. 인덱스: title (제목 검색용)
        """
        CREATE INDEX note_title IF NOT EXISTS
        FOR (n:Note) ON (n.title)
        """,
        # 5. 복합 인덱스: user_id + note_id (성능 최적화)
        """
        CREATE INDEX note_user_note_id IF NOT EXISTS
        FOR (n:Note) ON (n.user_id, n.note_id)
        """,
        # 6. 벡터 인덱스: embedding (유사도 검색용)
        """
        CREATE VECTOR INDEX note_embeddings IF NOT EXISTS
        FOR (n:Note) ON (n.embedding)
        OPTIONS {indexConfig: {
            `vector.dimensions`: 1536,
            `vector.similarity_function`: 'cosine'
        }}
        """,
    ]

    with neo4j_client.get_session() as session:
        try:
            for i, query in enumerate(queries, 1):
                session.run(query)
                logger.info(f"✅ 스키마 초기화 {i}/{len(queries)} 완료")

            logger.info("전체 스키마 초기화 완료")
            return True

        except Exception as e:
            logger.error(f"❌ 스키마 초기화 실패: {e}")
            return False


def check_indexes() -> None:
    """생성된 인덱스 확인"""
    with neo4j_client.get_session() as session:
        result = session.run("SHOW INDEXES")
        indexes = [record.data() for record in result]

        print("\n📋 생성된 인덱스:")
        print("-" * 80)
        for idx in indexes:
            name = idx.get("name", "N/A")
            index_type = idx.get("type", "N/A")
            state = idx.get("state", "N/A")
            print(f"  - {name:<30} | {index_type:<15} | {state}")
        print("-" * 80)


def check_constraints() -> None:
    """생성된 제약조건 확인"""
    with neo4j_client.get_session() as session:
        result = session.run("SHOW CONSTRAINTS")
        constraints = [record.data() for record in result]

        print("\n🔒 생성된 제약조건:")
        print("-" * 80)
        for const in constraints:
            name = const.get("name", "N/A")
            const_type = const.get("type", "N/A")
            print(f"  - {name:<30} | {const_type}")
        print("-" * 80)


def reset_database() -> None:
    """
    ⚠️ 주의: 모든 데이터 삭제!
    개발/테스트 환경에서만 사용
    """
    with neo4j_client.get_session() as session:
        try:
            # 모든 노드와 관계 삭제
            session.run("MATCH (n) DETACH DELETE n")
            logger.warning("⚠️ 모든 데이터 삭제 완료")
            return True
        except Exception as e:
            logger.error(f"❌ 데이터베이스 리셋 실패: {e}")
            return False


if __name__ == "__main__":
    # 로깅 설정
    logging.basicConfig(
        level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s"
    )

    print("\n" + "=" * 80)
    print("Neo4j 스키마 초기화")
    print("=" * 80)

    # 스키마 초기화
    if initialize_schema():
        # 인덱스 확인
        check_indexes()
        check_constraints()

        print("\n✅ 초기화 완료!")
    else:
        print("\n❌ 초기화 실패!")

    print("=" * 80 + "\n")
