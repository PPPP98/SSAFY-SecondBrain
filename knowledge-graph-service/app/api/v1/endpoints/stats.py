"""통계 엔드포인트"""
from fastapi import APIRouter, Header, HTTPException
import logging

from app.crud import note as note_crud
from app.schemas.note import GraphStats
from app.api.v1.dependencies import get_user_id

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/stats", tags=["stats"])


# ===== 통계 조회 =====
@router.get(
    "",
    response_model=GraphStats,
    summary="그래프 통계",
    description="사용자의 그래프 통계를 조회합니다",
)
async def get_stats(
    x_user_id: str = Header(..., alias="X-User-ID"),
) -> GraphStats:
    """
    통계 조회 API
    
    **응답:**
    - GraphStats (total_notes, total_relationships, avg_connections)
    """
    try:
        user_id = get_user_id(x_user_id)
        
        logger.info(f"📊 통계 조회: {user_id}")
        
        # 통계 조회
        stats = note_crud.get_stats(user_id=user_id)
        
        logger.info(f"✅ 통계 조회 완료")
        
        return GraphStats(**stats)
    
    except Exception as e:
        logger.error(f"❌ 통계 조회 실패: {e}")
        raise HTTPException(status_code=500, detail=str(e))
