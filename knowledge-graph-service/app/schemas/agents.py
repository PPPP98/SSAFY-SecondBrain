from pydantic import BaseModel, Field, ConfigDict
from typing import List


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