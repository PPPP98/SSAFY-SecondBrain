from pydantic import BaseModel, Field, ConfigDict
from typing import List, Optional, Dict


class LLMResponse(BaseModel):
    """
    LLM 응답 스키마
    - title: 노트 제목
    - result: 요약된 텍스트
    """

    title: str = Field(..., description="노트 제목")
    result: str = Field(..., description="요약된 텍스트")


class NoteSummarizeRequest(BaseModel):
    """
    노트 요약 요청 스키마
    - data: 요약할 노트의 URL 또는 텍스트 리스트
    """

    data: List[str] = Field(..., description="요약할 노트의 URL 또는 텍스트 리스트")

    model_config = ConfigDict(
        json_schema_extra={
            "example": {
                "data": [
                    "https://example.com/note1",
                    "https://example.com/note2",
                    "Some text content to summarize.",
                ]
            }
        }
    )


class DocumentSchema(BaseModel):
    """검색된 문서 스키마"""

    note_id: int = Field(description="노트 ID")
    title: str = Field(description="노트 제목")
    created_at: Optional[str] = Field(default=None, description="생성일")
    updated_at: Optional[str] = Field(default=None, description="수정일")
    similarity_score: Optional[float] = Field(default=None, description="유사도 점수")


class SearchResponse(BaseModel):
    """검색 응답 스키마"""

    success: bool = Field(description="성공 여부")
    response: str = Field(description="응답 메시지")
    documents: List[DocumentSchema] = Field(description="검색된 문서 목록")

    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "response": "React Hook 사용법에 대한 노트 3개를 찾았습니다[1][2][3].",
                "documents": [
                    {
                        "note_id": "note_123",
                        "title": "React Hooks 기본",
                        "created_at": "2024-11-10T10:00:00+09:00",
                        "similarity_score": 0.95,
                    }
                ],
            }
        }


class SearchErrorResponse(BaseModel):
    """에러 응답 스키마"""

    success: bool = Field(default=False, description="성공 여부")
    error: str = Field(description="에러 메시지")
    error_code: Optional[str] = Field(default=None, description="에러 코드")


class TimeFilter(BaseModel):
    """
    시간 필터 스키마
    """

    start: Optional[str] = Field(
        default=None, description="시작 시간 (ISO 8601 형식: YYYY-MM-DDTHH:MM:SS+09:00)"
    )
    end: Optional[str] = Field(
        default=None, description="종료 시간 (ISO 8601 형식: YYYY-MM-DDTHH:MM:SS+09:00)"
    )
    description: Optional[str] = Field(
        default=None, description="시간 표현에 대한 설명"
    )


class PreFilterOutput(BaseModel):
    """Pre-Filter 출력 스키마 (통합)"""

    # 시간 필터
    timespan: Optional[TimeFilter] = Field(
        default=None,
        description="시간 범위 필터",
    )
    # 검색 타입
    search_type: str = Field(
        description="검색 타입: simple_lookup | similarity | direct_answer",
    )
    # Similarity 분기용
    query: str = Field(
        default="",
        description="재작성된 쿼리 (search_type=similarity일 때 사용)",
    )


class SimpleLookupOutput(BaseModel):
    """Simple Lookup 출력"""

    documents: List[Dict] = Field(description="검색된 노트 리스트")
    count: int = Field(description="검색된 노트 개수")


class RelevanceCheckOutput(BaseModel):
    """연관성 체크 출력"""

    is_relevant: bool = Field(description="질문과 관련이 있는가")


class MCPSearchResponse(BaseModel):
    """검색 응답 스키마"""

    success: bool = Field(description="성공 여부")
    documents: List[Dict] = Field(description="검색된 문서 정보")

    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "documents": [
                    {
                        "note_id": 123,
                        "title": "React Hooks 기본",
                        "content": "내용",
                        "created_at": "2024-11-10T10:00:00+09:00",
                        "updated_at": "2024-11-10T10:00:00+09:00",
                    },
                ],
            }
        }


class MCPSearchErrorResponse(BaseModel):
    """에러 응답 스키마"""

    success: bool = Field(default=False, description="성공 여부")
    error: str = Field(description="에러 메시지")
    error_code: Optional[str] = Field(default=None, description="에러 코드")

class MCPSearchRequest(BaseModel):
    """
    MCP 요청 스키마
    """

    query: Optional[str] = Field(default=None, description="질문 쿼리")
    timespan: Optional[TimeFilter] = Field(default=None, description="검색 기간 설정")

