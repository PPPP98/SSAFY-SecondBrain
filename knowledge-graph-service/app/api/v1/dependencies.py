"""API v1 의존성 (공통 헬퍼 함수)"""
from fastapi import Header, HTTPException
import logging

logger = logging.getLogger(__name__)


def get_user_id(x_user_id: int = Header(..., alias="X-User-ID")) -> int:
    """
    Header에서 user_id 추출
    
    **사용:**
    ```
    async def my_endpoint(user_id: str = Depends(get_user_id)):
        # user_id 사용
    ```
    """
    if not x_user_id:
        logger.warning("❌ X-User-ID Header 없음")
        raise HTTPException(
            status_code=400,
            detail="X-User-ID Header가 필요합니다"
        )
    return x_user_id
