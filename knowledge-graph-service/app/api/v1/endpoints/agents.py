from fastapi import APIRouter, HTTPException, Depends, Query, status, Header
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
import logging

from app.services.note_summarize_service import note_summarize_service
from app.services.agent_search_service import agent_search_service
from app.services.external_service import external_service
from app.api.v1.dependencies import get_user_id

from app.schemas.agents import (
    NoteSummarizeRequest,
    SearchResponse,
    SearchErrorResponse,
    DocumentSchema,
)


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
    payload = {
        "title": result.get("title", ""),
        "content": result.get("result", ""),
    }
    response = await external_service.async_post_call_external_service(
        authorization, payload
    )

    if response.get("success") is not True:
        raise HTTPException(status_code=500, detail="Failed to save Create note")
    # return result
    logger.debug("✅ Note saved to external service")
    return response


@router.get(
    "/search",
    response_model=SearchResponse,
    responses={
        200: {"model": SearchResponse, "description": "응답 성공"},
        400: {"model": SearchErrorResponse, "description": "잘못된 요청"},
        500: {"model": SearchErrorResponse, "description": "서버 오류"},
    },
    summary="에이전트 검색",
    description="LLM을 활용하여 지식 그래프 내에서 검색 수행\nTOP_K : 3 으로 설정되어 있습니다. 수정 가능 테스트 확인 바람",
)
async def agent_search(
    x_user_id: int = Header(..., alias="X-User-ID"),
    query: str = Query(
        ...,
        description="검색 쿼리",
        min_length=1,
        max_length=500,
    ),
) -> SearchResponse:
    """
    에이전트 검색 엔드포인트

    Args:
        user_id: 사용자 ID (양수)
        query: 검색 쿼리 (1-500자)

    Returns:
        SearchResponse: 검색 결과

    Raises:
        HTTPException: 400 - 잘못된 요청
        HTTPException: 500 - 서버 오류
    """
    user_id = get_user_id(x_user_id)

    if not query:
        logger.warning(f"⚠️  빈 쿼리 - user_id: {user_id}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST, detail="검색 쿼리가 비어있습니다."
        )
    try:
        result = await agent_search_service.search(
            user_id=user_id,
            query=query,
        )
        # 결과 검증
        if result is None:
            logger.error(f"❌ 검색 결과 None - user_id: {user_id}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="검색 결과를 가져올 수 없습니다.",
            )
        # 응답 생성
        documents = [DocumentSchema(**doc) for doc in result.get("documents", [])]
        response = SearchResponse(
            success=True,
            response=result.get("response", ""),
            documents=documents,
        )

        return response
    except Exception as e:
        logger.error(f"검색 실패 - {e}")

        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"검색 중 오류가 발생했습니다.",
        )
