from fastapi import APIRouter, HTTPException, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
import logging

from app.services.note_summarize_service import note_summarize_service
from app.schemas.agents import NoteSummarizeRequest
from app.services.external_service import external_service

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
