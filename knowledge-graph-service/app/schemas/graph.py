"""그래프 시각화 스키마"""

from pydantic import BaseModel, Field
from typing import List, Optional


class GraphNode(BaseModel):
    """그래프 노드"""

    id: str = Field(..., description="노트 ID")
    title: str = Field(..., description="노트 제목")
    created_at: str = Field(..., description="생성 시간 (ISO format)")


class GraphLink(BaseModel):
    """그래프 링크"""

    source: str = Field(..., description="출발 노트 ID")
    target: str = Field(..., description="도착 노트 ID")
    score: float = Field(..., ge=0.0, le=1.0, description="유사도 점수")


class GraphStats(BaseModel):
    """그래프 통계"""

    total_nodes: int = Field(..., description="전체 노드 수")
    total_links: int = Field(..., description="전체 링크 수")
    avg_connections: float = Field(..., description="평균 연결 수")


class GraphVisualizationResponse(BaseModel):
    """3D 그래프 시각화 응답"""

    user_id: str = Field(..., description="사용자 ID")
    nodes: List[GraphNode] = Field(default_factory=list, description="노드 리스트")
    links: List[GraphLink] = Field(default_factory=list, description="링크 리스트")
    stats: Optional[GraphStats] = None

    model_config = {
        "json_schema_extra": {
            "example": {
                "user_id": "user-123",
                "nodes": [
                    {
                        "id": "note-1",
                        "title": "Neo4j 기초",
                        "created_at": "2025-11-03T12:00:00.000Z",
                    },
                    {
                        "id": "note-2",
                        "title": "그래프 DB",
                        "created_at": "2025-11-03T13:00:00.000Z",
                    },
                ],
                "links": [{"source": "note-1", "target": "note-2", "score": 0.85}],
                "stats": {"total_nodes": 2, "total_links": 1, "avg_connections": 0.5},
            }
        }
    }


class NeighborNode(BaseModel):
    """이웃 노드"""

    center_id: str = Field(..., description="중심 노트 ID")
    center_title: str = Field(..., description="중심 노트 제목")
    neighbor_id: str = Field(..., description="이웃 노트 ID")
    neighbor_title: str = Field(..., description="이웃 노트 제목")
    distance: int = Field(..., ge=1, description="거리 (1단계, 2단계 등)")


class NeighborGraphResponse(BaseModel):
    """이웃 그래프 응답"""

    center_note_id: str = Field(..., description="중심 노트 ID")
    neighbors: List[NeighborNode] = Field(
        default_factory=list, description="이웃 노드 리스트"
    )
