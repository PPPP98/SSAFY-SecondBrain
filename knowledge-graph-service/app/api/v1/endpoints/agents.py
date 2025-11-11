from fastapi import APIRouter, Header, HTTPException, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from typing import Optional, List
import logging
import requests

from app.services.note_summarize_service import note_summarize_service

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/agents", tags=["agents"])

security = HTTPBearer()


# --- request help func ---
def _call_external_service(auth_token: str, payload: dict) -> dict:
    URL: str = "https://api.brainsecond.site/api/notes"
    headers = {
        "Authorization": f"Bearer {auth_token}",
        "Content-Type": "application/json",
    }
    json: dict = {
        "title": payload.get("title", ""),
        "content": payload.get("result", ""),
    }
    try:
        response = requests.post(URL, json=json, headers=headers)
        response.raise_for_status()
        return response.json()

    except Exception as e:
        logger.error(f"External service call failed: {e}")
        raise HTTPException(status_code=500, detail="External service call failed")


@router.post(
    "/summarize",
    summary="요약",
    description="url, text 데이터 LLM을 활용해서 요약 저장",
)
async def note_summarize(
    data: List[str],
    credentials: HTTPAuthorizationCredentials = Depends(security),
):
    if not credentials:
        raise HTTPException(status_code=401, detail="JWT missing")
    
    authorization = credentials.credentials

    result = await note_summarize_service.get_note_summarize(data)
    if not result:
        raise HTTPException(status_code=400, detail="empty data")
    
    logger.debug("✅ Note summarize completed")
    response = _call_external_service(authorization, result)

    if response.get("success") is not True:
        raise HTTPException(status_code=500, detail="Failed to save Create note")
    # return result
    logger.debug("✅ Note saved to external service")
    return response
