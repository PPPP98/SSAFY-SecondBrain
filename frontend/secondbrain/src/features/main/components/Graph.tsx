import ForceGraph3D from 'react-force-graph-3d';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { useGraphVisualization } from '@/features/main/hooks/useGraphVisualization';

// 컴포넌트 외부에 상수와 함수 정의
// React 공식: 단순 상수/계산은 메모이제이션 불필요
const NODE_COLOR = '#FFFFFF';
const calculateLinkWidth = (link: { score: number }) => link.score * 2;
const calculateParticleWidth = (link: { score: number }) => link.score * 1.5;
const logNodeClick = (node: unknown) => {
  // TODO: 노드 클릭 시 상세 정보 표시 기능 구현
  console.info('선택된 노드:', node);
};

export const Graph = () => {
  const { data: graphData, isLoading, isError } = useGraphVisualization();

  // React 공식: 단순 null 체크는 useMemo 불필요
  const displayData = graphData || { nodes: [], links: [] };

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
        graphData={displayData}
        nodeLabel="title"
        nodeColor={NODE_COLOR}
        linkWidth={calculateLinkWidth}
        linkDirectionalParticles={2}
        linkDirectionalParticleWidth={calculateParticleWidth}
        backgroundColor="#192030"
        nodeRelSize={8}
        onNodeClick={logNodeClick}
        showNavInfo={false}
      />
    </div>
  );
};
