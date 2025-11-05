// 3D 시각화 데이터
export interface GraphNode {
  created_at: string;
  id: string;
  title: string;
}

export interface GraphLink {
  score: number;
  source: string;
  target: string;
}

export interface GraphVisualizationResponse {
  user_id: string;
  nodes: GraphNode[];
  links: GraphLink[];
  stats: {
    total_nodes: number;
    total_links: number;
    avg_connections: number;
  };
}

// 이웃 노드 조회
export interface GraphNeighbor {
  center_id: string;
  center_title: string;
  neighbor_id: string;
  neighbor_title: string;
  distance: number;
}

export interface GraphNeighborRequest {
  node_id: string;
  depth: number;
}

export interface GraphNeighborResponse {
  center_note_id: string;
  neighbors: GraphNeighbor[];
}

// 통계
export interface GraphStatsResponse {
  user_id: string;
  total_notes: number;
  total_relationships: number;
  avg_connections: number;
}
