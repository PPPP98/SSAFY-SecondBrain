from typing_extensions import TypedDict, List

class State(TypedDict):
    """
    요약 Agent Graph 상태 정의
    ---
    ### data
    - 사용자가 저장하고자 하는 페이지 url or text
    ### content
    - 처리한 데이터 정리(LLM 주입용)
    ### result
    - 요약 결과
    """
    data: List[str]
    content: List[str]
    result : str
