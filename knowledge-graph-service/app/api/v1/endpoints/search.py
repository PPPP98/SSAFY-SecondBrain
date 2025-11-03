"""검색 엔드포인트"""

from fastapi import APIRouter, Header, Query, HTTPException
import logging

from app.crud import note as note_crud
from app.schemas.note import NoteListResponse
from app.core.constants import NoteConfig, ErrorConfig
from app.api.v1.dependencies import get_user_id

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/search", tags=["search"])


# ===== 제목 검색 =====
@router.get(
    "/by-title",
    response_model=NoteListResponse,
    summary="제목 검색",
    description="제목으로 노트를 검색합니다",
)
async def search_by_title(
    x_user_id: str = Header(..., alias="X-User-ID"),
    title: str = Query(..., min_length=1, description="검색 제목"),
    limit: int = Query(
        default=NoteConfig.DEFAULT_SEARCH_LIMIT,
        ge=1,
        le=NoteConfig.MAX_SEARCH_LIMIT,
        description="최대 개수",
    ),
) -> NoteListResponse:
    """
    제목 검색 API

    **쿼리 파라미터:**
    - title: 검색할 제목 (필수)
    - limit: 최대 개수 (기본: 20, 최대: 100)

    **응답:**
    - NoteListResponse (notes 배열)
    """
    try:
        user_id = get_user_id(x_user_id)

        logger.debug(f"🔍 제목 검색: {user_id} - '{title}'")

        # 검색
        notes = note_crud.get_note_by_title(
            user_id=user_id,
            title=title,
            limit=limit,
        )

        logger.debug(f"✅ 검색 완료: {len(notes)}개")

        return NoteListResponse(
            user_id=user_id,
            notes=notes,
            total=len(notes),
            limit=limit,
            skip=0,
        )

    except Exception as e:
        logger.error(f"❌ 제목 검색 실패: {e}")
        raise HTTPException(status_code=500, detail=str(e))
