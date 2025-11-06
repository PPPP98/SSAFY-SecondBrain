from pydantic import BaseModel, Field
from typing import Optional
from enum import Enum


class EventType(str, Enum):
    """이벤트 타입"""

    NOTE_CREATED = "note.created"
    NOTE_UPDATED = "note.updated"
    NOTE_DELETED = "note.deleted"


class NoteCreatedEvent(BaseModel):
    """노트 생성 이벤트 (Spring Boot → FastAPI)"""

    event_type: EventType = EventType.NOTE_CREATED
    note_id: int = Field(..., description="노트 ID (UUID)")
    user_id: int = Field(..., description="사용자 ID")
    title: str = Field(..., description="노트 제목")
    content: str = Field(..., description="노트 내용")

    class Config:
        json_schema_extra = {
            "example": {
                "event_type": "note.created",
                "note_id": 1241251512,
                "user_id": 123,
                "title": "Python 기초",
                "content": "Python은 읽기 쉬운 프로그래밍 언어입니다...",
            }
        }


class NoteUpdatedEvent(BaseModel):
    """노트 수정 이벤트"""

    event_type: EventType = EventType.NOTE_UPDATED
    note_id: int = Field(..., description="노트 ID")
    user_id: int = Field(..., description="사용자 ID")
    title: Optional[str] = Field(None, description="노트 제목 (선택)")
    content: Optional[str] = Field(None, description="노트 내용 (선택)")

    class Config:
        json_schema_extra = {
            "example": {
                "event_type": "note.updated",
                "note_id": 550e8400446655440000,
                "user_id": 123,
                "title": "Python 심화",
                "content": None,
            }
        }


class NoteDeletedEvent(BaseModel):
    """노트 삭제 이벤트"""

    event_type: EventType = EventType.NOTE_DELETED
    note_id: int = Field(..., description="노트 ID")
    user_id: int = Field(..., description="사용자 ID")

    class Config:
        json_schema_extra = {
            "example": {
                "event_type": "note.deleted",
                "note_id": 550e8400446655440000,
                "user_id": 123,
            }
        }
