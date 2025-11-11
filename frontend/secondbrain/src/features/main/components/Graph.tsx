import { useMemo, useCallback } from 'react';
import ForceGraph3D from 'react-force-graph-3d';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { useGraphVisualization } from '@/features/main/hooks/useGraphVisualization';
import { useSearchPanelStore } from '@/features/main/stores/searchPanelStore';

export const Graph = () => {
  const { data: graphData, isLoading, isError } = useGraphVisualization();
  const highlightedNodeIds = useSearchPanelStore((state) => state.highlightedNodeIds);

  const nodeColorCallback = useCallback(
    (node: { id: string }) => {
      // 검색 중이 아니면 모든 노드 흰색
      if (highlightedNodeIds.size === 0) {
        return '#FFFFFF';
      }
      // node.id는 string, highlightedNodeIds는 number를 담고 있으므로 변환 필요
      const nodeIdNumber = Number(node.id);
      return highlightedNodeIds.has(nodeIdNumber) ? '#00FFFF' : '#666666';
    },
    [highlightedNodeIds],
  );

  // 검색 결과 노드는 크기도 크게 표시
  const nodeValCallback = useCallback(
    (node: { id: string }) => {
      if (highlightedNodeIds.size === 0) {
        return 8; // 기본 크기
      }
      const nodeIdNumber = Number(node.id);
      return highlightedNodeIds.has(nodeIdNumber) ? 15 : 8;
    },
    [highlightedNodeIds],
  );
  const linkWidthCallback = useCallback((link: { score: number }) => link.score * 2, []);
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
        nodeVal={nodeValCallback}
        linkWidth={linkWidthCallback}
        backgroundColor="#192030"
        nodeRelSize={8}
        onNodeClick={handleNodeClick}
        showNavInfo={false}
      />
    </div>
  );
};
