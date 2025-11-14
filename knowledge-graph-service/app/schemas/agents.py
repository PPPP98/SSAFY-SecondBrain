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
        description="검색 타입: simple_lookup | similarity",
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