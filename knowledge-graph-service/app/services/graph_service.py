"""그래프 시각화 서비스"""
from typing import Tuple, List, Dict
import logging
from app.db.neo4j_client import neo4j_client

logger = logging.getLogger(__name__)


class GraphVisualizationService:
    """그래프 시각화 비즈니스 로직"""
    
    @staticmethod
    def get_graph_nodes_and_links(user_id: str) -> Tuple[List[Dict], List[Dict]]:
        """
        사용자의 그래프 노드와 링크 조회
        
        Args:
            user_id: 사용자 ID
        
        Returns:
            (노드 리스트, 링크 리스트)
        """
        query_nodes = """
        MATCH (n:Note {user_id: $user_id})
        RETURN n.note_id AS id,
               n.title AS title,
               n.created_at AS created_at
        """
        
        query_links = """
        MATCH (a:Note {user_id: $user_id})-[r:SIMILAR_TO]-(b:Note {user_id: $user_id})
        RETURN a.note_id AS source,
               b.note_id AS target,
               r.score AS score
        """
        
        with neo4j_client.get_session() as session:
            # 노드 조회
            result_nodes = session.run(query_nodes, {"user_id": user_id})
            nodes = []
            for record in result_nodes:
                node_dict = dict(record)
                # DateTime 변환
                if node_dict.get("created_at"):
                    node_dict["created_at"] = node_dict["created_at"].iso_format()
                nodes.append(node_dict)
            
            # 링크 조회
            result_links = session.run(query_links, {"user_id": user_id})
            links = [dict(record) for record in result_links]
            
            logger.info(f"✅ 그래프 조회: {user_id} - {len(nodes)}개 노드, {len(links)}개 링크")
            
            return nodes, links
    
    @staticmethod
    def get_graph_with_metadata(user_id: str) -> Dict:
        """
        상세 그래프 데이터 (메타데이터 포함)
        
        Args:
            user_id: 사용자 ID
        
        Returns:
            그래프 데이터 (노드, 링크, 통계)
        """
        nodes, links = GraphVisualizationService.get_graph_nodes_and_links(user_id)
        
        # 통계
        stats_query = """
        MATCH (n:Note {user_id: $user_id})
        OPTIONAL MATCH (n)-[r:SIMILAR_TO]-()
        WITH count(DISTINCT n) AS total_nodes,
             count(DISTINCT r) AS total_links
        RETURN total_nodes, total_links
        """
        
        with neo4j_client.get_session() as session:
            result = session.run(stats_query, {"user_id": user_id})
            record = result.single()
            stats = {
                "total_nodes": record["total_nodes"] if record else 0,
                "total_links": record["total_links"] if record else 0,
                "avg_connections": (record["total_links"] / record["total_nodes"]) 
                    if record and record["total_nodes"] > 0 else 0
            }
        
        return {
            "user_id": user_id,
            "nodes": nodes,
            "links": links,
            "stats": stats
        }
    
    @staticmethod
    def get_graph_neighbors(user_id: str, note_id: str, depth: int = 1) -> Dict:
        """
        특정 노트 주변의 이웃 노드 조회
        
        Args:
            user_id: 사용자 ID
            note_id: 중심 노트 ID
            depth: 탐색 깊이 (1 = 직접 연결, 2 = 2단계)
        
        Returns:
            이웃 그래프 데이터
        """
        query = f"""
        MATCH (center:Note {{note_id: $note_id, user_id: $user_id}})
        MATCH path=(center)-[:SIMILAR_TO*1..{depth}]-(neighbor:Note {{user_id: $user_id}})
        WITH center, neighbor, apoc.path.length(path) AS distance
        RETURN DISTINCT 
            center.note_id AS center_id,
            center.title AS center_title,
            neighbor.note_id AS neighbor_id,
            neighbor.title AS neighbor_title,
            distance
        """
        
        with neo4j_client.get_session() as session:
            result = session.run(query, {"user_id": user_id, "note_id": note_id})
            neighbors = [dict(record) for record in result]
            
            logger.info(f"✅ 이웃 조회: {user_id} - {note_id} - {len(neighbors)}개")
            
            return {
                "center_note_id": note_id,
                "neighbors": neighbors
            }


# 싱글톤 인스턴스
graph_service = GraphVisualizationService()
