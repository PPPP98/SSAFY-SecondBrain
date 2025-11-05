import { fastApiClient } from '@/api/client';
import type {
  GraphVisualizationResponse,
  GraphNeighborRequest,
  GraphNeighborResponse,
  GraphStatsResponse,
} from '@/features/main/types/graph';

const GRAPH_ENDPOINTS = {
  VISUALIZATION: '/graph/visualization',
  NEIGHBORS: '/graph/neighbors',
  STATS: '/stats',
} as const;

export const graphAPI = {
  getGraphVisualization: async (): Promise<GraphVisualizationResponse> => {
    const response = await fastApiClient.get<GraphVisualizationResponse>(
      GRAPH_ENDPOINTS.VISUALIZATION,
    );
    return response.data;
  },

  getGraphNeighbors: async (requestData: GraphNeighborRequest): Promise<GraphNeighborResponse> => {
    const response = await fastApiClient.get<GraphNeighborResponse>(
      `${GRAPH_ENDPOINTS.NEIGHBORS}/${requestData.node_id}`,
    );
    return response.data;
  },

  getGraphStats: async (): Promise<GraphStatsResponse> => {
    const response = await fastApiClient.get<GraphStatsResponse>(GRAPH_ENDPOINTS.STATS);
    return response.data;
  },
};
