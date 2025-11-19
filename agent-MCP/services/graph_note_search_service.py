# services/graph_note_search_service.py
"""연결 그래프 노트 서칭"""
import httpx
import logging
import asyncio

logger = logging.getLogger(__name__)


class GraphNoteSearchService:
    """
    노트 간 연결 그래프를 탐색하고 관련 노트 정보를 검색하는 서비스 클래스

    이 클래스는 특정 노트의 연결된 이웃 노트들을 탐색하고,
    각 노트의 상세 정보를 비동기적으로 가져와 포맷팅된 결과를 반환합니다.

    Attributes:
        api_base_url (str): API 서버의 기본 URL
        api_key (str): API 인증에 사용되는 키
        user_id (int | None): 인증된 사용자의 ID (초기값: None)
    """

    def __init__(self, api_base_url: str, api_key: str):
        self.api_base_url = api_base_url
        self.api_key = api_key
        self.user_id = None

    async def graph_note_search(
        self,
        note_id: int,
        depth: int = 1,
    ) -> str:
        """
        특정 노트와 연결된 노트들을 그래프 기반으로 검색합니다

        지정된 깊이만큼 노트 연결 그래프를 탐색하여 이웃 노트들의
        상세 정보를 가져오고 포맷팅된 결과를 반환합니다.

        Args:
            note_id (int): 검색 시작점이 되는 노트의 ID
            depth (int, optional): 그래프 탐색 깊이. 기본값은 1

        Returns:
            str: 포맷팅된 노트 검색 결과 문자열
                성공 시: 각 노트의 상세 정보가 Markdown 형식으로 정리된 문자열
                실패 시: 에러 메시지 문자열

        Raises:
            httpx.TimeoutException: 요청 시간 초과 시 (60초)
            httpx.HTTPStatusError: HTTP 에러 응답 시
            httpx.RequestError: 네트워크 요청 실패 시
            Exception: 기타 예상치 못한 에러 발생 시
        """
        if not note_id or note_id < 1:
            logger.error(f"잘못된 ID입니다.")
            return "잘못된 노트ID를 입력하였습니다."
        try:
            user_id = await self._get_user_id()

            async with httpx.AsyncClient(timeout=60.0) as client:
                response = await client.get(
                    url=f"{self.api_base_url}ai/api/v1/graph/neighbors/{note_id}",
                    params={"depth": depth},
                    headers={"X-User-ID": str(user_id)},
                )
                response.raise_for_status()
                graph_results: dict = response.json()

            neighbors_notes = self._get_neighbor_note_id(graph_results.get("neighbors"))

            if not neighbors_notes:
                logger.warning(f"연관 노트가 없습니다.")
                return "연관된 노트가 없습니다."

            tasks = [self._get_note_data(id) for id in neighbors_notes]
            results = await asyncio.gather(*tasks)

            return self._format_results(results)

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

    async def _get_user_id(self) -> int:
        """
        API 키를 검증하고 사용자 ID를 가져옵니다

        이미 user_id가 캐시되어 있으면 캐시된 값을 반환하고,
        없으면 API를 호출하여 새로 가져옵니다.

        Returns:
            int: 인증된 사용자의 ID

        Raises:
            httpx.HTTPStatusError: API 응답이 4xx 또는 5xx 상태 코드인 경우
            httpx.RequestError: 네트워크 요청 실패 시
        """
        if self.user_id:
            return self.user_id
        payload = {"apiKey": self.api_key}
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(
                url=f"{self.api_base_url}api/apikey/validate",
                json=payload,
                headers={
                    "Content-Type": "application/json",
                },
            )
            response.raise_for_status()
            result: dict = response.json()
            self.user_id = result.get("data", {}).get("userId", 0)
        return self.user_id

    def _get_neighbor_note_id(self, documents: list) -> list:
        """
        그래프 검색 결과에서 이웃 노트의 ID 목록을 추출합니다

        Args:
            documents (list): 그래프 API 응답의 neighbors 리스트
                각 항목은 'neighbor_id' 키를 포함하는 딕셔너리

        Returns:
            list: 이웃 노트 ID들의 리스트 (예: [123, 456, 789])
        """
        results = []
        for doc in documents:
            results.append(doc["neighbor_id"])
        return results

    async def _get_note_data(self, note_id: int) -> dict:
        """
        특정 노트의 상세 정보를 비동기적으로 가져옵니다

        Args:
            note_id (int): 조회할 노트의 ID

        Returns:
            dict: 노트의 상세 정보를 담은 딕셔너리
                - noteId/note_id: 노트 ID
                - title: 노트 제목
                - content: 노트 내용
                - createdAt/created_at: 생성 날짜
                - updatedAt/updated_at: 수정 날짜

        Raises:
            httpx.HTTPStatusError: API 응답이 4xx 또는 5xx 상태 코드인 경우
            httpx.RequestError: 네트워크 요청 실패 시
        """
        async with httpx.AsyncClient(timeout=60.0) as client:
            response = await client.get(
                url=f"{self.api_base_url}api/mcp/notes/{note_id}",
                headers={"X-API-Key": self.api_key},
            )
            response.raise_for_status()
            result = response.json()
        return result.get("data", {})

    def _format_results(
        self,
        documents: list,
    ) -> str:
        """검색 결과 포맷팅"""
        formatted_results = []

        for doc in documents:
            note_id = doc.get("noteId", doc.get("note_id", "N/A"))
            title = doc.get("title", "제목 없음")
            content = doc.get("content", "내용 없음")
            created_at = doc.get("createdAt", doc.get("created_at", "N/A"))
            updated_at = doc.get("updatedAt", doc.get("updated_at", "N/A"))

            note_info = f"""
            ## 📝 노트 {title}

            **노트 ID**: {note_id}
            **작성일**: {created_at}
            **수정일**: {updated_at}
            ### 내용
            {content}
            ---
            """
            formatted_results.append(note_info)

        response_text = "\n".join(formatted_results)

        return response_text

    def _handle_http_error(self, error: httpx.HTTPStatusError) -> str:
        """
        HTTP 상태 코드별 에러 메시지를 생성합니다

        Args:
            error (httpx.HTTPStatusError): HTTP 에러 응답 객체

        Returns:
            str: 상태 코드에 맞는 사용자 친화적인 에러 메시지
                - 401: 인증 실패
                - 400: 잘못된 요청
                - 404: 서비스를 찾을 수 없음
                - 500: 서버 내부 오류
                - 기타: 일반 에러 메시지
        """
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

