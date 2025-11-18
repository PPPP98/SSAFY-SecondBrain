import React from 'react';
import ReactDOM from 'react-dom/client';
import { SidePanelApp } from './SidePanelApp';
import './styles/sidepanel.css';

/**
 * Side Panel 진입점
 * - Chrome Side Panel에서 실행되는 React 앱
 */
const root = document.getElementById('root');

if (root) {
  ReactDOM.createRoot(root).render(
    <React.StrictMode>
      <SidePanelApp />
    </React.StrictMode>,
  );
} else {
  console.error('[SidePanel] Root element not found');
}
