import ForceGraph3D from 'react-force-graph-3d';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { useGraphVisualization } from '@/features/main/hooks/useGraphVisualization';
import { useSearchPanelStore } from '@/features/main/stores/searchPanelStore';
import type { GraphNode } from '@/features/main/types/graph';

// 컴포넌트 외부에 순수 함수 정의
// React 공식: 단순 계산 함수는 메모이제이션 불필요
const getNodeColor = (node: GraphNode, highlightedNodeIds: Set<number>) => {
  if (highlightedNodeIds.size === 0) return '#FFFFFF';
  const nodeIdNumber = Number(node.id);
  return highlightedNodeIds.has(nodeIdNumber) ? '#00FFFF' : '#666666';
};

const getNodeVal = (node: GraphNode, highlightedNodeIds: Set<number>) => {
  if (highlightedNodeIds.size === 0) return 8;
  const nodeIdNumber = Number(node.id);
  return highlightedNodeIds.has(nodeIdNumber) ? 15 : 8;
};

const calculateLinkWidth = (link: { score: number }) => link.score * 2;
const calculateParticleWidth = (link: { score: number }) => link.score * 1.5;

export const Graph = () => {
  const { data: graphData, isLoading, isError } = useGraphVisualization();
  const highlightedNodeIds = useSearchPanelStore((state) => state.highlightedNodeIds);

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
        graphData={graphData}
        nodeLabel="title"
        nodeColor={(node) => getNodeColor(node as GraphNode, highlightedNodeIds)}
        nodeVal={(node) => getNodeVal(node as GraphNode, highlightedNodeIds)}
        linkWidth={calculateLinkWidth}
        linkDirectionalParticles={2}
        linkDirectionalParticleWidth={calculateParticleWidth}
        backgroundColor="#192030"
        nodeRelSize={8}
        onNodeClick={(node) => console.info('선택된 노드:', node)}
        showNavInfo={false}
      />
    </div>
  );
};
