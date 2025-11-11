import logging
from typing import List
from app.agents.note_summarize_agent.graph import Graph
from app.agents.note_summarize_agent.state import State

logger = logging.getLogger(__name__)


class NoteSummarizeService:
    """
    LLM을 활용하여 URL, 텍스트 데이터 요약

    - URL (http/https): 본문 크롤링 후 요약
    - 텍스트: 그대로 요약에 포함
    """

    def __init__(self):
        self.graph = Graph.create_summarization_graph()

    async def get_note_summarize(
        self,
        data: List[str],
    ) -> dict:
        """
        데이터를 요약하여 반환

        Args:
            data: URL 또는 텍스트 리스트

        Returns:
            str: 요약된 내용 (실패 시 빈 문자열)
        """
        if not data:
            logger.warning("⚠️ Empty data provided")
            return {}
        try:
            initial_state: State = {
                "data": data,
                "content": [],
                "result": "",
            }
            result = await self.graph.ainvoke(initial_state)
            if not result["result"]:
                logger.warning("⚠️ No result generated")
                return {}
            return {
                "title": result["title"],
                "result": result["result"],
            }
        except Exception as e:
            logger.error(f"error : {e}")
            return {}


note_summarize_service = NoteSummarizeService()
