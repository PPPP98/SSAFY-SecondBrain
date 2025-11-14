from pydantic import BaseModel, Field, ConfigDict
from typing import List, Optional


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
    """Pre-Filter 출력 스키마"""

    timespan: Optional[TimeFilter] = Field(default=None, description="시간 범위 필터")
    tags: List[str] = Field(default_factory=list, description="태그 리스트")
    exact_title: Optional[str] = Field(default=None, description="정확한 제목")
    note_id: Optional[str] = Field(default=None, description="노트 ID")
    is_simple_lookup: bool = Field(
        default=False, description="필터만으로 검색 가능한가"
    )
    cleaned_query: str = Field(description="필터를 제거한 순수 검색어")
