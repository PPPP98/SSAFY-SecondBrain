# services/search_service.py
"""개인 노트 검색 서비스"""
import httpx
import logging
from typing import Optional

logger = logging.getLogger(__name__)


class SearchService:
    """노트 검색 서비스"""

    def __init__(self, api_base_url: str, api_key: str):
        self.api_base_url = api_base_url
        self.api_key = api_key

    async def search_notes(
        self,
        query: Optional[str] = None,
        start: Optional[str] = None,
        end: Optional[str] = None,
    ) -> str:
        """
        개인 노트 검색

        Args:
            query: 검색 쿼리
            start: 시작 날짜 (ISO 8601)
            end: 종료 날짜 (ISO 8601)

        Returns:
            str: 검색 결과 텍스트
        """
        if not query and not (start or end):
            logger.warning("검색 조건이 확인되지 않았습니다")
            return "검색을 위해 쿼리 또는 시간 범위를 제공해주세요."

        try:
            # 페이로드 구성
            payload = {}

            if query:
                payload["query"] = query
                logger.info(f"💬 쿼리: {query}")

            if start or end:
                payload["timespan"] = {}
                if start:
                    payload["timespan"]["start"] = start
                if end:
                    payload["timespan"]["end"] = end
                logger.info(f"📅 시간 필터: {payload['timespan']}")

            # API 호출
            async with httpx.AsyncClient(timeout=60.0) as client:
                response = await client.post(
                    f"{self.api_base_url}/ai/api/v1/agents",
                    json=payload,
                    headers={"X-API-Key": self.api_key, "Content-Type": "application/json"},
                )
                response.raise_for_status()
                result = response.json()

            # 응답 처리
            if not result.get("success"):
                logger.error(f"검색 실패: {result}")
                return "검색에 실패했습니다. 다시 시도해주세요."

            documents = result.get("documents", [])

            # 결과 없음
            if not documents:
                logger.info("⚠️ 검색 결과 없음")
                return self._format_no_results(query, start, end)

            # 결과 포맷팅
            logger.info(f"✅ {len(documents)}개 노트 발견")
            return self._format_results(documents, query, start, end)

        except httpx.TimeoutException:
            logger.error("⏰ 요청 타임아웃")
            return "검색 요청이 시간 초과되었습니다. 네트워크 연결을 확인하고 다시 시도해주세요."

        except httpx.HTTPStatusError as e:
            logger.error(f"❌ HTTP 에러: {e.response.status_code} - {e.response.text}")
            return self._handle_http_error(e)

        except httpx.RequestError as e:
            logger.error(f"❌ 네트워크 에러: {e}")
            return f"네트워크 오류가 발생했습니다: {str(e)}"

        except Exception as e:
            logger.error(f"❌ 예상치 못한 에러: {e}", exc_info=True)
            return f"검색 중 예상치 못한 오류가 발생했습니다: {str(e)}"

    def _format_no_results(
        self, query: Optional[str], start: Optional[str], end: Optional[str]
    ) -> str:
        """검색 결과 없음 메시지 포맷팅"""
        search_info = f"쿼리: '{query}'" if query else ""
        time_info = ""

        if start and end:
            time_info = f", 기간: {start} ~ {end}"
        elif start:
            time_info = f", 시작: {start}"
        elif end:
            time_info = f", 종료: {end}"

        return f"검색 결과가 없습니다. ({search_info}{time_info})"

    def _format_results(
        self,
        documents: list,
        query: Optional[str],
        start: Optional[str],
        end: Optional[str],
    ) -> str:
        """검색 결과 포맷팅"""
        formatted_results = []

        for i, doc in enumerate(documents, 1):
            note_id = doc.get("noteId", doc.get("note_id", "N/A"))
            title = doc.get("title", "제목 없음")
            content = doc.get("content", "내용 없음")
            created_at = doc.get("createdAt", doc.get("created_at", "N/A"))
            updated_at = doc.get("updatedAt", doc.get("updated_at", "N/A"))
            similarity_score = doc.get("similarity_score")

            # 유사도 정보
            similarity_info = ""
            if similarity_score is not None:
                similarity_info = f"**유사도**: {similarity_score:.2%}\n"

            note_info = f"""
            ## 📝 노트 {i}: {title}

            **노트 ID**: {note_id}
            **작성일**: {created_at}
            **수정일**: {updated_at}
            {similarity_info}
            ### 내용
            {content}

            ---
            """
            formatted_results.append(note_info)

        # 요약 생성
        summary = self._generate_summary(len(documents), query, start, end)

        # 최종 응답
        response_text = f"# 검색 결과\n\n{summary}\n\n"
        response_text += "\n".join(formatted_results)

        return response_text

    def _generate_summary(
        self, count: int, query: Optional[str], start: Optional[str], end: Optional[str]
    ) -> str:
        """검색 결과 요약 생성"""
        if query and (start or end):
            time_range = f"{start} ~ {end}" if start and end else (start or end)
            return (
                f"'{query}'에 대해 {time_range} 기간 내 {count}개의 노트를 찾았습니다."
            )
        elif query:
            return f"'{query}'에 대해 {count}개의 노트를 찾았습니다."
        else:
            time_range = f"{start} ~ {end}" if start and end else (start or end)
            return f"{time_range} 기간 동안 작성된 {count}개의 노트를 찾았습니다."

    def _handle_http_error(self, error: httpx.HTTPStatusError) -> str:
        """HTTP 에러 처리"""
        status_code = error.response.status_code

        error_messages = {
            401: "인증에 실패했습니다. API 키를 확인해주세요.",
            400: "잘못된 요청입니다. 검색 조건을 확인해주세요.",
            404: "검색 서비스를 찾을 수 없습니다. 서버 주소를 확인해주세요.",
            500: "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
        }

        return error_messages.get(
            status_code, f"검색 중 오류가 발생했습니다. (HTTP {status_code})"
        )
