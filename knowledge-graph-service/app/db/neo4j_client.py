from neo4j import GraphDatabase, Driver, Session
from app.core.config import get_settings
import logging
from typing import Optional

# log, env
logger = logging.getLogger(__name__)
settings = get_settings()


class Neo4jClient:
    """Neo4j 데이터베이스 클라이언트"""

    def __init__(self):
        self._driver: Optional[Driver] = None
        self._connect()

    def _connect(self):
        """Neo4j 연결"""
        try:
            self._driver = GraphDatabase.driver(
                settings.neo4j_uri,
                auth=(settings.neo4j_user, settings.neo4j_password),
            )
            # 연결 확인
            self._driver.verify_connectivity()
            logger.info("✅ Neo4j 연결 성공")
        except Exception as e:
            logger.error(f"❌ Neo4j 연결 실패: {e}")
            raise

    @property
    def driver(self) -> Driver:
        """드라이버 반환"""
        if not self._driver:
            self._connect()
        return self._driver

    def get_session(self) -> Session:
        """세션 생성"""
        return self.driver.session()

    def close(self):
        """연결 종료"""
        if self._driver:
            self._driver.close()
            logger.info("Neo4j 연결 종료")

    def verify_connection(self):
        """연결 상태 확인"""
        try:
            with self.get_session() as session:
                result = session.run("RETURN 1 AS num")
                record = result.single()
                return record["num"] == 1
        except Exception as e:
            logger.error(f"연결 확인 실패: {e}")
            return False


# 싱글톤 인스턴스
neo4j_client = Neo4jClient()

