import * as THREE from 'three';
import { SVGLoader } from 'three/addons/loaders/SVGLoader.js';

/**
 * 순수 Three.js 3D 로고 스피너
 * React 없이 Three.js만 사용하여 iframe 환경에서 안정적으로 작동
 */

// 씬, 카메라, 렌더러 초기화
const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(50, 1, 0.1, 1000);
camera.position.z = 100;

const renderer = new THREE.WebGLRenderer({
  antialias: true,
  alpha: true, // 투명 배경
});

renderer.setSize(window.innerWidth, window.innerHeight);
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));

const rootElement = document.getElementById('root');
if (rootElement) {
  // 로딩 스피너 제거
  rootElement.innerHTML = '';
  rootElement.appendChild(renderer.domElement);
}

// 조명 설정 (3-Point Lighting)
const ambientLight = new THREE.AmbientLight(0xffffff, 0.5);
scene.add(ambientLight);

const keyLight = new THREE.DirectionalLight(0xffffff, 1);
keyLight.position.set(10, 10, 5);
scene.add(keyLight);

const fillLight = new THREE.DirectionalLight(0xffffff, 0.5);
fillLight.position.set(-10, -5, -5);
scene.add(fillLight);

// 3D 메쉬 그룹
const logoGroup = new THREE.Group();
scene.add(logoGroup);

// SVG 로드 및 ExtrudeGeometry 생성
const svgLoader = new SVGLoader();
const svgUrl = chrome.runtime.getURL('Logo.svg');

svgLoader.load(
  svgUrl,
  (data) => {
    console.log('✅ SVG loaded successfully');

    // SVG paths → Shapes 변환
    const shapes: THREE.Shape[] = [];
    data.paths.forEach((path) => {
      const pathShapes = SVGLoader.createShapes(path);
      shapes.push(...pathShapes);
    });

    console.log(`📐 Created ${shapes.length} shapes`);

    // ExtrudeGeometry 설정
    const extrudeSettings: THREE.ExtrudeGeometryOptions = {
      depth: 15,
      bevelEnabled: true,
      bevelThickness: 2,
      bevelSize: 1,
      bevelSegments: 3,
    };

    // 모든 shapes를 하나의 geometry로 병합
    const geometry = new THREE.ExtrudeGeometry(shapes, extrudeSettings);

    // 중앙 정렬
    geometry.center();
    geometry.computeBoundingBox();

    // 크기 정규화
    const bbox = geometry.boundingBox!;
    const size = Math.max(
      bbox.max.x - bbox.min.x,
      bbox.max.y - bbox.min.y,
      bbox.max.z - bbox.min.z,
    );
    const scale = 50 / size;
    geometry.scale(scale, scale, scale);
    geometry.center();

    console.log(`🎨 Geometry scaled to ${scale.toFixed(2)}`);

    // 재질 생성
    const material = new THREE.MeshStandardMaterial({
      color: 0x6366f1, // Indigo
      metalness: 0.4,
      roughness: 0.3,
      flatShading: false,
    });

    // 메쉬 생성 및 추가
    const mesh = new THREE.Mesh(geometry, material);
    logoGroup.add(mesh);

    console.log('🎉 3D logo added to scene');
  },
  (xhr) => {
    const percent = (xhr.loaded / xhr.total) * 100;
    console.log(`⏳ Loading SVG: ${percent.toFixed(0)}%`);
  },
  (error) => {
    console.error('❌ Failed to load SVG:', error);
  },
);

// 애니메이션 루프
function animate() {
  requestAnimationFrame(animate);

  // 회전 애니메이션
  logoGroup.rotation.y += 0.01; // Y축 회전
  logoGroup.rotation.x += 0.003; // X축 회전 (느리게)

  renderer.render(scene, camera);
}

// 애니메이션 시작
animate();

// 윈도우 리사이즈 대응
window.addEventListener('resize', () => {
  const width = window.innerWidth;
  const height = window.innerHeight;

  camera.aspect = width / height;
  camera.updateProjectionMatrix();

  renderer.setSize(width, height);
});

console.log('🚀 Three.js spinner initialized');
