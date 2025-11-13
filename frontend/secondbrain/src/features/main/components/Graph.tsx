import { useNavigate } from '@tanstack/react-router';
import { useRef, useEffect } from 'react';
import ForceGraph3D from 'react-force-graph-3d';
import type { ForceGraphMethods } from 'react-force-graph-3d';
import * as THREE from 'three';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { useGraphVisualization } from '@/features/main/hooks/useGraphVisualization';
import { useSearchPanelStore } from '@/features/main/stores/searchPanelStore';
import type { GraphNode, GraphLink } from '@/features/main/types/graph';

const calculateLinkWidth = (link: { score: number }) => link.score * 2;
const calculateParticleWidth = (link: { score: number }) => link.score * 1.5;

// 네온 효과가 적용된 커스텀 노드 생성 함수 (컴포넌트 외부)
const createNodeThreeObject = (highlightedNodeIds: Set<number>) => (node: GraphNode) => {
  const nodeIdNumber = Number(node.id);
  const isHighlighted = highlightedNodeIds.has(nodeIdNumber);
  const hasHighlights = highlightedNodeIds.size > 0;

  const nodeSize = isHighlighted ? 7.5 : 4;
  const geometry = new THREE.SphereGeometry(nodeSize, 16, 16);

  if (isHighlighted) {
    // 하이라이트된 노드: 노란색 네온 효과
    const material = new THREE.MeshPhongMaterial({
      color: 0xfffae3,
      emissive: 0xfffae3,
      emissiveIntensity: 1.5,
      shininess: 200,
    });

    const mesh = new THREE.Mesh(geometry, material);

    // 강한 글로우 효과를 위한 외부 구체 추가
    const glowGeometry = new THREE.SphereGeometry(nodeSize * 1.3, 13, 13);
    const glowMaterial = new THREE.MeshBasicMaterial({
      color: 0xfffae3,
      transparent: true,
      opacity: 0.6,
      side: THREE.BackSide,
    });
    const glowMesh = new THREE.Mesh(glowGeometry, glowMaterial);

    mesh.add(glowMesh);
    return mesh;
  } else {
    // 일반 노드: 하이라이트가 있을 경우 투명도 적용
    const material = new THREE.MeshBasicMaterial({
      color: 0xffffff,
      transparent: hasHighlights,
      opacity: hasHighlights ? 0.3 : 1.0,
    });
    return new THREE.Mesh(geometry, material);
  }
};

export const Graph = () => {
  const navigate = useNavigate();
  const { data: graphData, isLoading, isError } = useGraphVisualization();
  const highlightedNodeIds = useSearchPanelStore((state) => state.highlightedNodeIds);
  const fgRef = useRef<ForceGraphMethods<GraphNode, GraphLink>>();

  useEffect(() => {
    if (fgRef.current) {
      const fg = fgRef.current;
      // 링크 거리를 100으로 설정
      const linkForce = fg.d3Force('link') as { distance: (d: number) => void } | undefined;
      if (linkForce) {
        linkForce.distance(100);
      }
      // 반발력 강화로 노드 간격 증가
      const chargeForce = fg.d3Force('charge') as { strength: (s: number) => void } | undefined;
      if (chargeForce) {
        chargeForce.strength(-200);
      }
    }
  }, [graphData]);

  const handleNodeClick = (node: GraphNode) => {
    void navigate({ to: '/notes/$noteId', params: { noteId: String(node.id) } });
  };

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
        ref={fgRef}
        graphData={graphData}
        nodeLabel="title"
        nodeThreeObject={createNodeThreeObject(highlightedNodeIds)}
        linkWidth={calculateLinkWidth}
        linkDirectionalParticleWidth={calculateParticleWidth}
        linkDirectionalParticles={0}
        backgroundColor="#10131A"
        onNodeClick={(node) => handleNodeClick(node as GraphNode)}
        showNavInfo={false}
        d3AlphaDecay={0.02}
        d3VelocityDecay={0.3}
      />
    </div>
  );
};
