import logging
from langgraph.graph import StateGraph, START, END

from .state import State
from .nodes import Nodes

logger = logging.getLogger(__name__)


class Graph:
    @staticmethod
    def create_search_graph():
        """
        검색 Agent Graph 생성

        플로우:
        1. Pre-Filter (3-way 분기)
        - direct_answer → Generate Response
        - simple_lookup → Generate Response
        - similarity → Relevance Check → Generate Response
        2. Generate Response → END

        Returns:
            CompiledGraph: 컴파일된 LangGraph
        """
        builder = StateGraph(State)
        # ========================================
        # 노드 추가
        # ========================================

        builder.add_node("pre_filter", Nodes.pre_filter_node)
        builder.add_node("simple_lookup", Nodes.simple_lookup_node)
        builder.add_node("similarity_search", Nodes.similarity_search_node)
        builder.add_node("relevance_check", Nodes.relevance_check_node)
        builder.add_node("generate_response", Nodes.generate_response_node)
        # ========================================
        # 시작점 설정
        # ========================================
        builder.add_edge(START, "pre_filter")

        # ========================================
        # 조건부 라우팅 (Pre-Filter → 3-way 분기)
        # ========================================

        def route_search_type(state: State) -> str:
            """
            Pre-Filter 결과에 따라 분기

            Returns:
                "direct_answer" | "simple_lookup" | "similarity"
            """
            search_type = state.get("search_type", "direct_answer")

            logger.debug(f"🔀 라우팅: {search_type}")

            # 유효성 검사
            valid_types = ["direct_answer", "simple_lookup", "similarity"]
            if search_type not in valid_types:
                logger.warning(
                    f"⚠️  알 수 없는 search_type: {search_type}, 기본값(direct_answer) 사용"
                )
                return "similarity"

            return search_type

        builder.add_conditional_edges(
            "pre_filter",
            route_search_type,
            {
                "direct_answer": "generate_response",
                "simple_lookup": "simple_lookup",
                "similarity": "similarity_search",
            },
        )

        # ========================================
        # Simple Lookup → Generate Response
        # ========================================

        builder.add_edge("simple_lookup", "generate_response")
        # ========================================
        # Similarity → Relevance Check → Generate Response
        # ========================================

        builder.add_edge("similarity_search", "relevance_check")
        builder.add_edge("relevance_check", "generate_response")
        # ========================================
        # Generate Response → END
        # ========================================

        builder.add_edge("generate_response", END)

        # ========================================
        # 그래프 컴파일
        # ========================================

        logger.info("🔧 그래프 컴파일 중...")
        graph = builder.compile()
        logger.info("✅ 그래프 컴파일 완료")

        return graph

    @staticmethod
    def create_mcp_search_graph():
        """ """
        builder = StateGraph(State)

        # ========================================
        # 노드 추가
        # ========================================
        builder.add_node("check_search_type", Nodes.check_search_type)
        builder.add_node("simple_lookup", Nodes.simple_lookup_node)
        builder.add_node("similarity_search", Nodes.similarity_search_node)
        builder.add_node("relevance_check", Nodes.relevance_check_node)
        # ========================================
        # 시작점 설정
        # ========================================
        builder.add_edge(START, "check_search_type")
        # ========================================
        # 조건부 라우팅 (check_search_type → 2-way 분기)
        # ========================================
        def route_search_type(state: State) -> str:
            """
            check_search_type
            
            Returns:
                "simple_lookup" | "similarity"
            """
            search_type = state.get("search_type", "end")

            return search_type
        
        builder.add_conditional_edges(
            "check_search_type",
            route_search_type,
            {
                "simple_lookup": "simple_lookup",
                "similarity": "similarity_search",
                "end": END,
            }
        )
        builder.add_edge("simple_lookup", END)

        builder.add_edge("similarity_search", "relevance_check")
        builder.add_edge("relevance_check", END)

        graph = builder.compile()

        return graph
