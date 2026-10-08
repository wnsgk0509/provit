import React, { useEffect, useRef } from 'react';
import { 
  Info, 
  CheckCircle2, 
  AlertTriangle, 
  AlertCircle, 
  HelpCircle, 
  X 
} from 'lucide-react';
import './AppModal.css';

/**
 * 💡 Provit 커스텀 모달 컴포넌트
 */
export function AppModal({
  isOpen,
  title,
  message,
  type = 'info',
  confirmText = '확인',
  cancelText = '취소',
  showCancel = false,
  isDestructive = false,
  onConfirm,
  onCancel,
}) {
  const confirmBtnRef = useRef(null);

  useEffect(() => {
    if (!isOpen) return;

    // 포커스 이동
    confirmBtnRef.current?.focus();

    // 키보드 이벤트 핸들러 (ESC: 닫기/취소, Enter: 확인)
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        onCancel?.();
      } else if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        onConfirm?.();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onConfirm, onCancel]);

  if (!isOpen) return null;

  const renderIcon = () => {
    switch (type) {
      case 'success':
        return <CheckCircle2 size={30} strokeWidth={2.2} />;
      case 'warning':
        return <AlertTriangle size={30} strokeWidth={2.2} />;
      case 'danger':
      case 'error':
        return <AlertCircle size={30} strokeWidth={2.2} />;
      case 'confirm':
        return isDestructive ? (
          <AlertTriangle size={30} strokeWidth={2.2} />
        ) : (
          <HelpCircle size={30} strokeWidth={2.2} />
        );
      case 'info':
      default:
        return <Info size={30} strokeWidth={2.2} />;
    }
  };

  const getIconWrapClass = () => {
    if (isDestructive) return 'danger';
    if (type === 'confirm') return 'confirm';
    if (type === 'error') return 'danger';
    return type;
  };

  return (
    <div 
      className="provit-modal-backdrop"
      onClick={(e) => {
        if (e.target === e.currentTarget) {
          onCancel?.();
        }
      }}
      role="dialog"
      aria-modal="true"
    >
      <div className="provit-modal-card">
        <button 
          type="button"
          className="provit-modal-close-btn"
          onClick={onCancel}
          aria-label="닫기"
        >
          <X size={18} />
        </button>

        <div className={`provit-modal-icon-wrap ${getIconWrapClass()}`}>
          {renderIcon()}
        </div>

        {title && <h3 className="provit-modal-title">{title}</h3>}

        <div className="provit-modal-message">
          {message}
        </div>

        <div className="provit-modal-actions">
          {showCancel && (
            <button
              type="button"
              className="provit-modal-btn cancel"
              onClick={onCancel}
            >
              {cancelText}
            </button>
          )}
          <button
            ref={confirmBtnRef}
            type="button"
            className={`provit-modal-btn ${isDestructive ? 'danger' : 'primary'}`}
            onClick={onConfirm}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
}

/**
 * 💡 Provit 플로팅 토스트 컨테이너
 */
export function AppToastContainer({ toasts, onDismiss }) {
  if (!toasts || toasts.length === 0) return null;

  return (
    <div className="provit-toast-container" role="status" aria-live="polite">
      {toasts.map((toast) => {
        const iconType = toast.type || 'info';
        return (
          <div 
            key={toast.id} 
            className={`provit-toast-item ${iconType}`}
          >
            <div className="provit-toast-icon">
              {iconType === 'success' && <CheckCircle2 size={20} strokeWidth={2.2} />}
              {iconType === 'warning' && <AlertTriangle size={20} strokeWidth={2.2} />}
              {iconType === 'danger' && <AlertCircle size={20} strokeWidth={2.2} />}
              {iconType === 'info' && <Info size={20} strokeWidth={2.2} />}
            </div>
            <div className="provit-toast-content">
              {toast.message}
            </div>
            <button
              type="button"
              className="provit-toast-close"
              onClick={() => onDismiss(toast.id)}
              aria-label="알림 닫기"
            >
              <X size={14} />
            </button>
          </div>
        );
      })}
    </div>
  );
}

export default AppModal;
