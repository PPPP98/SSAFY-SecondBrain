from typing_extensions import TypedDict

class NoteSummarizeAgentState(TypedDict):
    """
    요약 Agent Graph 상태 정의
    ---
    ### data
    - 사용자가 저장하고자 하는 페이지 url or text
    ### result
    - 요약 결과
    """
    data: list[str]
    result : str


