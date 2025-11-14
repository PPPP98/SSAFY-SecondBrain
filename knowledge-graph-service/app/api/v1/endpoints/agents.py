from fastapi import APIRouter, HTTPException, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
import logging

from app.services.note_summarize_service import note_summarize_service
from app.services.agent_search_service import agent_search_service
from app.services.external_service import external_service

from app.schemas.agents import NoteSummarizeRequest


logger = logging.getLogger(__name__)

router = APIRouter(prefix="/agents", tags=["agents"])

security = HTTPBearer()


@router.post(
    "/summarize",
    summary="요약",
    description="url, text 데이터 LLM을 활용해서 요약 저장",
)
async def note_summarize(
    data: NoteSummarizeRequest,
    credentials: HTTPAuthorizationCredentials = Depends(security),
):
    if not credentials:
        raise HTTPException(status_code=401, detail="JWT missing")

    authorization = credentials.credentials

    result = await note_summarize_service.get_note_summarize(data.data)
    if not result:
        raise HTTPException(status_code=400, detail="empty data")

    logger.debug("✅ Note summarize completed")
    payload={
        "title": result.get("title", ""),
        "content": result.get("result", ""),
    }
    response = external_service.post_call_external_service(authorization, payload)

    if response.get("success") is not True:
        raise HTTPException(status_code=500, detail="Failed to save Create note")
    # return result
    logger.debug("✅ Note saved to external service")
    return response


@router.get(
    "/search",
    summary="에이전트 검색",
    description="LLM을 활용하여 지식 그래프 내에서 검색 수행",
)
async def agent_search(
    user_id: int,
    query: str,
):
    result = await agent_search_service.search(
        user_id=user_id,
        query=query,
    )

    logger.debug("✅ Agent search completed")
    return result