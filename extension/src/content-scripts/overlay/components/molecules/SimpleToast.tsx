import { useEffect, useState } from 'react';
import { CheckCircle, XCircle, Info, X } from 'lucide-react';

interface Toast {
  id: string;
  message: string;
  type: 'success' | 'error' | 'info';
}

// Global toast queue (outside React)
let toastQueue: Toast[] = [];
const listeners = new Set<() => void>();

function notifyListeners() {
  listeners.forEach((listener) => listener());
}

export function addToast(message: string, type: 'success' | 'error' | 'info' = 'info') {
  const toast: Toast = {
    id: `toast-${Date.now()}-${Math.random()}`,
    message,
    type,
  };

  toastQueue = [...toastQueue, toast];
  notifyListeners();

  // Auto remove after 4s
  setTimeout(() => {
    removeToast(toast.id);
  }, 4000);
}

function removeToast(id: string) {
  toastQueue = toastQueue.filter((t) => t.id !== id);
  notifyListeners();
}

/**
 * Toast Item Component
 */
function ToastItem({ toast, onClose }: { toast: Toast; onClose: () => void }) {
  const iconMap = {
    success: <CheckCircle className="h-5 w-5 text-green-500" />,
    error: <XCircle className="h-5 w-5 text-red-500" />,
    info: <Info className="h-5 w-5 text-blue-500" />,
  };

  const bgColorMap = {
    success: '#f0fdf4',
    error: '#fef2f2',
    info: '#eff6ff',
  };

  const borderColorMap = {
    success: '#86efac',
    error: '#fca5a5',
    info: '#93c5fd',
  };

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
        padding: '12px 16px',
        backgroundColor: bgColorMap[toast.type],
        border: `1px solid ${borderColorMap[toast.type]}`,
        borderRadius: '8px',
        boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
        minWidth: '300px',
        maxWidth: '400px',
        pointerEvents: 'auto',
        animation: 'slideDown 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
      }}
    >
      {iconMap[toast.type]}
      <span
        style={{
          flex: 1,
          fontSize: '14px',
          color: '#1f2937',
          fontWeight: 500,
        }}
      >
        {toast.message}
      </span>
      <button
        onClick={onClose}
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '4px',
          backgroundColor: 'transparent',
          border: 'none',
          cursor: 'pointer',
          borderRadius: '4px',
          transition: 'background-color 0.2s',
        }}
        onMouseEnter={(e) => {
          e.currentTarget.style.backgroundColor = 'rgba(0, 0, 0, 0.05)';
        }}
        onMouseLeave={(e) => {
          e.currentTarget.style.backgroundColor = 'transparent';
        }}
      >
        <X className="h-4 w-4 text-gray-500" />
      </button>
    </div>
  );
}

/**
 * Simple Toast Container for Shadow DOM
 * - No external dependencies (Sonner removed)
 * - Inline styles for Shadow DOM compatibility
 * - Global toast queue management
 */
export function SimpleToastContainer() {
  const [toasts, setToasts] = useState<Toast[]>([]);

  useEffect(() => {
    const listener = () => {
      setToasts([...toastQueue]);
    };

    listeners.add(listener);
    listener(); // Initial sync

    return () => {
      listeners.delete(listener);
    };
  }, []);

  if (toasts.length === 0) return null;

  return (
    <>
      {/* Inject keyframes for animations */}
      <style>
        {`
          @keyframes slideDown {
            from {
              opacity: 0;
              transform: translateY(-20px);
            }
            to {
              opacity: 1;
              transform: translateY(0);
            }
          }
        `}
      </style>

      <div
        style={{
          position: 'fixed',
          top: '20px',
          left: '50%',
          transform: 'translateX(-50%)',
          zIndex: 10000,
          display: 'flex',
          flexDirection: 'column',
          gap: '8px',
          pointerEvents: 'none',
        }}
      >
        {toasts.map((toast) => (
          <ToastItem key={toast.id} toast={toast} onClose={() => removeToast(toast.id)} />
        ))}
      </div>
    </>
  );
}

// Export as showToast for compatibility
export const showToast = addToast;
