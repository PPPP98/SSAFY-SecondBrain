import { useMemo, useCallback } from 'react';
import ForceGraph3D from 'react-force-graph-3d';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { useGraphVisualization } from '@/features/main/hooks/useGraphVisualization';

export const Graph = () => {
  const { data: graphData, isLoading, isError } = useGraphVisualization();

  const nodeColorCallback = useCallback(() => '#FFFFFF', []);
  const linkWidthCallback = useCallback((link: { score: number }) => link.score * 2, []);
  const particleWidthCallback = useCallback((link: { score: number }) => link.score * 1.5, []);
  const handleNodeClick = useCallback((node: unknown) => {
    // TODO: 노드 클릭 시 상세 정보 표시 기능 구현
    console.info('선택된 노드:', node);
  }, []);

  // 성능 최적화: graphData 객체를 메모이제이션
  const memoizedGraphData = useMemo(() => {
    if (!graphData) return { nodes: [], links: [] };
    return {
      nodes: graphData.nodes,
      links: graphData.links,
    };
  }, [graphData]);

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (isError) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-lg text-gray-500">그래프 데이터를 불러오는데 실패했습니다.</p>
      </div>
    );
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
        graphData={memoizedGraphData}
        nodeLabel="title"
        nodeColor={nodeColorCallback}
        linkWidth={linkWidthCallback}
        linkDirectionalParticles={2}
        linkDirectionalParticleWidth={particleWidthCallback}
        backgroundColor="#192030"
        nodeRelSize={8}
        onNodeClick={handleNodeClick}
        showNavInfo={false}
      />
    </div>
  );
};
