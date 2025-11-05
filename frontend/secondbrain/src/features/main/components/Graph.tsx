import { useState, useEffect } from 'react';
import ForceGraph3D from 'react-force-graph-3d';
import { graphAPI } from '@/features/main/services/graphService';
import type { GraphVisualizationResponse } from '@/features/main/types/graph';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';

export const Graph = () => {
  const [graphData, setGraphData] = useState<GraphVisualizationResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchGraphView = async () => {
      try {
        setIsLoading(true);
        const response = await graphAPI.getGraphVisualization();
        console.log('response', response);
        setGraphData(response);
      } catch (error) {
        console.error('그래프 데이터를 불러오는데 실패했습니다:', error); // toast 알림으로 변경
      } finally {
        setIsLoading(false);
      }
    };

    void fetchGraphView();
  }, []);

  if (isLoading) {
    return <LoadingSpinner message="그래프 로딩 중..." />;
  }

  if (!graphData || graphData.nodes.length === 0) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-lg text-gray-600">표시할 노드가 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="h-screen w-full">
      <ForceGraph3D
        graphData={{
          nodes: graphData.nodes,
          links: graphData.links,
        }}
        nodeLabel="title"
        nodeColor={() => '#FFFFFF'}
        linkLabel={(link) => `연결 강도: ${link.score}`}
        linkWidth={(link) => link.score * 2}
        linkDirectionalParticles={2}
        linkDirectionalParticleWidth={(link) => link.score * 1.5}
        backgroundColor="#192030"
        nodeRelSize={6}
        onNodeClick={(node) => {
          console.log('클릭한 노드:', node);
        }}
      />
    </div>
  );
};
