"""그래프 시각화 엔드포인트"""

from fastapi import APIRouter, Header, HTTPException, Query
import logging

from app.services.graph_service import graph_service
from app.schemas.graph import GraphVisualizationResponse, NeighborGraphResponse

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/graph", tags=["graph"])


@router.get(
    "/visualization",
    response_model=GraphVisualizationResponse,
    summary="3D 그래프 시각화 데이터",
    description="사용자의 전체 노트 그래프를 3D 시각화하기 위한 데이터 반환",
)
async def get_graph_visualization(
    user_id: str = Header(..., alias="X-User-ID")
) -> GraphVisualizationResponse:
    """
    3D 그래프 시각화 데이터 조회

    프론트엔드에서 3d-force-graph 라이브러리로 사용할 수 있도록
    노드와 링크 데이터를 반환합니다.

    **응답 형식**:
    ```
    {
      "user_id": "user-123",
      "nodes": [
        {"id": "note-id", "title": "제목", "created_at": "2025-11-03T12:00:00Z"}
      ],
      "links": [
        {"source": "note-id-1", "target": "note-id-2", "score": 0.85}
      ],
      "stats": {
        "total_nodes": 10,
        "total_links": 15,
        "avg_connections": 1.5
      }
    }
    ```
    """
    try:
        graph_data = graph_service.get_graph_with_metadata(user_id)

        return GraphVisualizationResponse(
            user_id=graph_data["user_id"],
            nodes=graph_data["nodes"],
            links=graph_data["links"],
            stats=graph_data["stats"],
        )

    except Exception as e:
        logger.error(f"❌ 그래프 조회 실패: {user_id}: {e}")
        raise HTTPException(status_code=500, detail="그래프 조회 실패")


@router.get(
    "/neighbors/{note_id}",
    response_model=NeighborGraphResponse,
    summary="이웃 노드 조회",
    description="특정 노트와 연결된 이웃 노드들을 조회",
)
async def get_neighbors(
    note_id: str,
    user_id: str = Header(..., alias="X-User-ID"),
    depth: int = Query(1, ge=1, le=3, description="탐색 깊이"),
) -> NeighborGraphResponse:
    """
    이웃 노드 조회

    **경로 파라미터**:
    - note_id: 중심 노트 ID

    **쿼리 파라미터**:
    - depth: 탐색 깊이 (1 = 직접 연결, 2 = 2단계 등)

    **응답 형식**:
    ```
    {
      "center_note_id": "note-123",
      "neighbors": [
        {
          "center_id": "note-123",
          "center_title": "중심 제목",
          "neighbor_id": "note-456",
          "neighbor_title": "이웃 제목",
          "distance": 1
        }
      ]
    }
    ```
    """
    try:
        neighbor_data = graph_service.get_graph_neighbors(user_id, note_id, depth)

        return NeighborGraphResponse(
            center_note_id=neighbor_data["center_note_id"],
            neighbors=neighbor_data["neighbors"],
        )

    except Exception as e:
        logger.error(f"❌ 이웃 조회 실패: {user_id} - {note_id}: {e}")
        raise HTTPException(status_code=500, detail="이웃 조회 실패")
