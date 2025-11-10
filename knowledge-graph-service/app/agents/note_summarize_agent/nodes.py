import asyncio
import trafilatura
import json
import logging
from typing import Any
from .state import State
from .models import Models
from .prompts import Prompts

logger = logging.getLogger(__name__)


class Nodes:
    """
    ## 노트 요약 Agent 노드
    - data 추출
    - 추출 데이터 요약
    
    """

    @staticmethod
    async def _extract_content(url: str) -> str:
        """
        ## url에서 본문 추출 헬퍼 함수
        : trafilatura 라이브러리 사용
        ### Returns:
            str: 추출 내용
        """
        try:
            logger.debug(f"🌐 Fetching URL: {url}")
            downloaded = await asyncio.to_thread(
                trafilatura.fetch_url,
                url,
            )
            if not downloaded:
                logger.warning(f"trafilatura No response")
                return ""

            result = await asyncio.to_thread(
                trafilatura.extract,
                downloaded,
                output_format="json",
                include_comments=False,
                include_links=False,
                with_metadata=True,
            )
            if not result:
                logger.warning("trafilatura No content")
                return ""

            parsed = json.loads(result)
            title = parsed.get("title", "Untitled")
            text = parsed.get("text", "")

            if not text:
                logger.warning("trafilatura Empty text")
                return ""

            content = f"## {title}\n\n{text}"
            logger.debug(f"✅ ({len(text)} chars)")

            return content

        except Exception as e:
            logger.error(f"error: {e}")
            pass

    @staticmethod
    def _is_url(text: str) -> bool:
        """URL 여부 판단"""
        return text.startswith("http://") or text.startswith("https://")

    @staticmethod
    async def extract_node(state: State) -> dict[str, Any]:
        """
        Node 1: data → content 변환

        - URL (http/https): Trafilatura로 본문 추출
        - 텍스트: 그대로 사용
        """
        logger.debug(f"\n🔄 [EXTRACT] Processing {len(state['data'])} items")

        contents = []

        for idx, item in enumerate(state["data"], 1):
            logger.debug(f"   [{idx}/{len(state['data'])}]")

            if Nodes._is_url(item):
                # URL 처리
                logger.debug(f"      URL: {item[:60]}...")
                content = await Nodes._extract_content(item)

                if content:
                    contents.append(item + content)
                    logger.debug(f"      ✅ Extracted")
                else:
                    logger.warning(f"      ⚠️ Failed")
            else:
                # 텍스트 처리
                logger.debug(f"      TEXT: {item[:60]}...")
                contents.append(item)
                logger.debug(f"      ✅ Added ({len(item)} chars)")

        logger.debug(f"\n📊 Result: {len(contents)}/{len(state['data'])} items")

        return {"content": contents}

    @staticmethod
    async def summarize_node(state: State) -> dict[str, Any]:
        """
        Node 2: content → result 변환 (LLM 요약)
        """
        logger.debug(f"\n📝 [SUMMARIZE]")

        if not state["content"]:
            logger.warning("   ⚠️ No content to summarize")
            return {"result": ""}

        # 모든 content 결합
        combined = "\n\n---\n\n".join(state["content"])

        logger.debug(f"   Total: {len(combined):,} chars")
        logger.debug(f"   Calling LLM...")

        try:
            # Models 클래스 사용
            model_manager = Models()
            llm = model_manager.summarize_model()

            # ainvoke() 사용
            response = await llm.ainvoke(Prompts.SYSTEMPROMPT.format(content=combined))

            # content 속성
            result = response.content

            logger.debug(f"   ✅ Summary: {len(result)} chars")

            return {"result": result}

        except Exception as e:
            logger.error(f"   ❌ LLM Error: {e}")
            return {"result": ""}

