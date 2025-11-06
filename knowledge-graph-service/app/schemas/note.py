from pydantic import BaseModel, Field, ConfigDict
from typing import List, Optional
from datetime import datetime


class NoteCreate(BaseModel):
    """노트 생성 요청"""

    note_id: int = Field(..., description="노트 ID (Spring Boot에서 생성)")
    title: str = Field(..., min_length=1, max_length=500, description="노트 제목")
    content: str = Field(..., min_length=1, description="노트 내용(임베딩 생성용)")

    model_config = ConfigDict(
        # just for swagger UI
        json_schema_extra={
            "example": {
                "note_id": 123,
                "title": "Neo4j 기초",
                "content": "Neo4j는 그래프 데이터베이스입니다.",
            }
        }
    )


class NoteResponse(BaseModel):
    """노트 응답"""

    user_id: int
    note_id: int
    title: str
    created_at: datetime
    updated_at: Optional[datetime] = None

    model_config = ConfigDict(from_attributes=True)


class SimilarNote(BaseModel):
    """유사 노트"""

    note_id: int
    title: str
    similarity_score: float = Field(..., ge=0.0, le=1.0)
    created_at: datetime


class NoteDetailResponse(NoteResponse):
    """노트 상세 (유사 노트 포함)"""

    similar_notes: List[SimilarNote] = Field(default_factory=list)


class EmbeddingResponse(BaseModel):
    """임베딩 생성 응답"""

    user_id: int
    note_id: int
    embedding_dimension: int
    linked_notes_count: int


class GraphStats(BaseModel):
    """그래프 통계"""

    user_id: int
    total_notes: int
    total_relationships: int
    avg_connections: float


class NoteListResponse(BaseModel):
    """노트 목록 응답"""

    user_id: int
    notes: List[NoteResponse]
    total: int
    limit: int
    skip: int
