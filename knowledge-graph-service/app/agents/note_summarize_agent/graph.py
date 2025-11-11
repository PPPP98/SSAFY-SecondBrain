import logging
from langgraph.graph import StateGraph, START, END

from .state import State
from .nodes import Nodes

logger = logging.getLogger(__name__)


class Graph:
    """
    ## Graph build
    - create_summarization_graph()
    Flow:
        START → extract → summarize → END
               (data)    (content)
    노드:
        - extract: URL 크롤링 + 텍스트 수집
        - summarize: LLM 통합 요약
    """

    @staticmethod
    def create_summarization_graph():
        logger.debug("Building summarization graph...")

        builder = StateGraph(State)
        # add node
        builder.add_node("extract", Nodes.extract_node)
        builder.add_node("summarize", Nodes.summarize_node)
        # edge
        builder.add_edge(START, "extract")
        builder.add_edge("extract", "summarize")
        builder.add_edge("summarize", END)

        graph = builder.compile()

        logger.info("✅ Graph built successfully")

        return graph
