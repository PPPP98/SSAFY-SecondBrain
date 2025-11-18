# main.py
"""FastMCP 서버: 개인 노트 검색"""
import logging
import os
from typing import Optional

from dotenv import load_dotenv
from fastmcp import FastMCP
from pydantic import Field

from services.search_service import SearchService

# 환경 변수 로드
load_dotenv()

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 환경 변수
API_BASE_URL = os.getenv("API_BASE_URL")
API_KEY = os.getenv("API_KEY")

if not API_KEY or not API_BASE_URL:
    raise ValueError("환경변수 API_BASE_URL과 API_KEY가 필요합니다")

# FastMCP 서버 초기화
mcp = FastMCP("Personal Notes Search Server: Second Brain")

# 검색 서비스 인스턴스
search_service = SearchService(api_base_url=API_BASE_URL, api_key=API_KEY)


# ==========================
# MCP 도구 등록
# ==========================


@mcp.tool(
    name="search_personal_notes",
    description="""
    개인 노트를 자연어로 검색합니다.
    
    검색 방법:
    1. 유사도 검색을 위한 쿼리가 존재하면, 유사도 검색 실시
    2. 쿼리가 제공되지 않았으면, 시간 범위로 필터링한 노트 제공
    3. 시간과 쿼리 둘 다 제공시 시간 범위 내에 유사한 노트 검색 후 제공
    4. 둘 다 제공하지 않을 시 검색 불가
    
    예시:
    - "어제 작성한 노트 바탕으로 찾아줘" : 시간 필터
    - "오늘 작성한 파이썬 관련 노트로 대답해줘" : 시간 필터 + 의미 검색
    - "지식 그래프 구축 방법론에 대해 정리한 노트 찾아줘" : 의미 검색
    """,
)
async def search_personal_notes(
    query: Optional[str] = Field(
        default=None,
        description="검색할 내용을 자연어로 입력하세요. 예: '지난주 작성한 머신러닝 관련 노트', 'React Hooks 사용법'",
    ),
    start: Optional[str] = Field(
        default=None,
        description="검색 시작 날짜 (ISO 8601 형식: YYYY-MM-DDTHH:MM:SS+09:00)",
    ),
    end: Optional[str] = Field(
        default=None,
        description="검색 종료 날짜 (ISO 8601 형식: YYYY-MM-DDTHH:MM:SS+09:00)",
    ),
) -> str:
    """MCP 도구: 개인 노트 검색"""
    return await search_service.search_notes(query=query, start=start, end=end)


# ==========================
# 서버 실행
# ==========================

if __name__ == "__main__":
    logger.info("🚀 FastMCP 서버 시작")
    logger.info(f"📡 API URL: {API_BASE_URL}")
    logger.info(f"🔑 API Key: {API_KEY[:10]}..." if API_KEY else "❌ API Key 없음")

    mcp.run(transport="stdio")
