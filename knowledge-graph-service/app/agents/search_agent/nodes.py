from datetime import datetime
from .state import State
from .models import Models
from .prompts import Prompts
from .utils.time_utils import get_time_context
from .utils.neo4j_query_builder import (
    build_time_filter_cypher,
    build_similarity_search_cypher,
)
from app.db.neo4j_client import neo4j_client
from app.services.embedding_service import embedding_service
from typing import Any

import logging
import asyncio
import json

logger = logging.getLogger(__name__)
SEARCH_LIMIT = 10


class Nodes:
    """ """

    @staticmethod
    async def pre_filter_node(state: State) -> State:
        """
        0단계: Pre-Filter

        작업:
        1. 시간 범위 추출
        2. 검색 타입 결정 (simple_lookup | similarity)
        3. 쿼리 재작성 (similarity용, 풍부한 검색)
        """

        try:
            logger.debug(f"🔍 Pre-Filter - user: {state.get('user_id')}")

            # 시각 정보
            time_context = get_time_context()

            # LLM 모델
            models = Models()
            llm = models.get_prefilter_model()

            # 프롬프트
            prompt_text = Prompts.PRE_FILTER_PROMPT.format(
                query=state["query"],
                current_datetime=time_context["current_datetime"],
                weekday_korean=time_context["weekday_korean"],
                week_number=time_context["week_number"],
            )

            # LLM 호출
            logger.debug(f"💬 분석: {state['query']}")
            result = await llm.ainvoke(prompt_text)

            # 필터 구성
            filters = {}
            if result.timespan:
                filters["timespan"] = {
                    "start": result.timespan.start,
                    "end": result.timespan.end,
                    "description": result.timespan.description,
                }
                logger.debug(f"📅 시간: {result.timespan.description}")

            # 로깅
            logger.debug(f"🔀 타입: {result.search_type}")
            if result.search_type == "similarity" and result.query:
                logger.debug(f"✏️  재작성: {result.query}")

            # State 업데이트
            return {
                **state,
                "original_query": state["query"],
                "query": result.query if result.query else state["query"],
                "filters": filters,
                "search_type": result.search_type,
            }

        except Exception as e:
            logger.error(f"❌ Pre-filter 에러: {str(e)}")
            import traceback

            traceback.print_exc()

            # 기본값: similarity
            return {
                **state,
                "original_query": state.get("query", ""),
                "filters": {},
                "search_type": "similarity",
            }

    @staticmethod
    async def simple_lookup_node(state: State) -> State:
        """
        Simple Lookup 노드: 시간 필터로 Neo4j 검색

        작업:
        1. 시간 필터 기반 Cypher 쿼리 생성
        2. Neo4j 검색 실행
        3. 최대 10개 결과를 state["documents"]에 저장

        Returns:
            documents: 검색된 노트 리스트 (최대 search_limit개)
        """

        try:
            logger.debug("🔍 Simple Lookup 시작")

            # 파라미터 추출
            user_id = state.get("user_id")
            timespan = state.get("filters", {}).get("timespan")

            if not user_id:
                logger.error("user_id가 없습니다")
                raise ValueError("user_id가 필요합니다")

            # Cypher 쿼리 생성
            cypher, params = build_time_filter_cypher(
                user_id=user_id,
                timespan=timespan,
                limit=SEARCH_LIMIT,
            )

            logger.debug(f"📝 Cypher:\n{cypher}")
            logger.debug(f"📦 Params: {params}")

            # Neo4j 검색
            with neo4j_client.get_session() as session:
                result = session.run(cypher, params)
                records = list(result)

            # 결과 포맷팅
            documents = []
            for record in records:
                doc = {
                    "note_id": record["note_id"],
                    "title": record["title"],
                }

                if record["created_at"]:
                    doc["created_at"] = record["created_at"].isoformat()

                if record["updated_at"]:
                    doc["updated_at"] = record["updated_at"].isoformat()

                documents.append(doc)

            logger.debug(
                f"✅ Simple Lookup 완료: {len(documents)}개 " f"(최대 {SEARCH_LIMIT}개)"
            )

            if timespan:
                logger.debug(f"📅 시간 범위: {timespan.get('description', 'N/A')}")

            return {
                **state,
                "documents": documents,
            }

        except Exception as e:
            logger.error(f"❌ Simple Lookup 에러: {str(e)}")
            import traceback

            traceback.print_exc()

            return {
                **state,
                "documents": [],
            }

    @staticmethod
    async def similarity_search_node(state: State) -> State:
        """
        Similarity Search 노드: 벡터 유사도 검색

        작업:
        1. 재작성된 쿼리 임베딩 (EmbeddingService 사용)
        2. Neo4j 벡터 검색 (Top-3)
        3. 결과를 state["documents"]에 저장

        Returns:
            documents: 유사도 높은 노트 Top-3
        """

        try:
            logger.debug("🔍 Similarity Search 시작")

            # 1. 파라미터 추출
            query = state.get("query", "")
            user_id = state.get("user_id")
            timespan = state.get("filters", {}).get("timespan")

            if not user_id:
                logger.error("user_id가 없습니다")
                raise ValueError("user_id가 필요합니다")

            if not query:
                logger.warning("검색 쿼리가 비어있습니다")
                return {**state, "documents": []}

            logger.debug(f"💬 검색 쿼리: {query}")

            # 2. 쿼리 임베딩 (EmbeddingService 사용)
            logger.debug("📊 임베딩 생성 중...")
            query_embedding, token_count = embedding_service.generate_embedding(query)

            logger.debug(
                f"✅ 임베딩 완료 (차원: {len(query_embedding)}, 토큰: {token_count})"
            )

            # 3. Cypher 쿼리 생성
            cypher, params = build_similarity_search_cypher(
                embedding=query_embedding,
                user_id=user_id,
                timespan=timespan,
                limit=SEARCH_LIMIT,
            )

            logger.debug(f"📝 Cypher 쿼리:\n{cypher}")
            logger.debug(f"📦 파라미터 (임베딩 제외): user_id={user_id}, limit={10}")

            # 4. Neo4j 벡터 검색
            logger.debug("🔎 Neo4j 벡터 검색 중...")
            with neo4j_client.get_session() as session:
                result = session.run(cypher, params)
                records = list(result)

            # 5. 결과 포맷팅
            documents = []
            for record in records:
                doc = {
                    "note_id": record["note_id"],
                    "title": record["title"],
                    "similarity_score": float(record["similarity_score"]),
                }

                if record["created_at"]:
                    doc["created_at"] = record["created_at"].isoformat()

                if record["updated_at"]:
                    doc["updated_at"] = record["updated_at"].isoformat()

                documents.append(doc)

            logger.debug(f"✅ Similarity Search 완료: {len(documents)}개 ")

            if timespan:
                logger.debug(f"📅 시간 범위: {timespan.get('description', 'N/A')}")

            # 유사도 점수 로깅
            if documents:
                logger.debug("📊 유사도 점수:")
                for i, doc in enumerate(documents, 1):
                    logger.debug(
                        f"  [{i}] {doc['title']}: {doc['similarity_score']:.3f}"
                    )
            else:
                logger.warning("⚠️  검색 결과 없음")

            return {
                **state,
                "documents": documents,
            }

        except Exception as e:
            logger.error(f"❌ Similarity Search 에러: {str(e)}")
            import traceback

            traceback.print_exc()

            return {
                **state,
                "documents": [],
            }
