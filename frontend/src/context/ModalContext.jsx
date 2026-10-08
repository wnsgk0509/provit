import React, { createContext, useContext, useState, useCallback, useEffect, useRef } from 'react';
import { AppModal, AppToastContainer } from '../components/common/AppModal';

const ModalContext = createContext(null);

// 비-React 컨텍스트(일반 유틸리티 함수 등)에서도 접근 가능한 브릿지 참조
let globalModalBridge = {
  showAlert: (msg) => console.warn('[ModalContext] 아직 초기화되지 않음:', msg),
  showConfirm: (msg) => Promise.resolve(window.confirm ? false : false),
  showToast: (msg) => console.warn('[ModalContext] 아직 초기화되지 않음:', msg),
};

export function ModalProvider({ children }) {
  const [modalState, setModalState] = useState({
    isOpen: false,
    title: '',
    message: '',
    type: 'info',
    confirmText: '확인',
    cancelText: '취소',
    showCancel: false,
    isDestructive: false,
    resolve: null,
  });

  const [toasts, setToasts] = useState([]);
  const resolveRef = useRef(null);

  // 1. Alert 모달 오픈
  const showAlert = useCallback((message, options = {}) => {
    return new Promise((resolve) => {
      resolveRef.current = resolve;
      setModalState({
        isOpen: true,
        title: options.title || '안내',
        message: typeof message === 'string' ? message : String(message ?? ''),
        type: options.type || 'info',
        confirmText: options.confirmText || '확인',
        cancelText: '',
        showCancel: false,
        isDestructive: false,
        resolve,
      });
    });
  }, []);

  // 2. Confirm 모달 오픈 (확인: true, 취소/닫기: false 반환)
  const showConfirm = useCallback((message, options = {}) => {
    return new Promise((resolve) => {
      resolveRef.current = resolve;
      // 메시지나 타이틀에 '삭제', '탈퇴', '정지' 등 파괴적 단어가 포함되면 자동 위험 스타일
      const isAutoDestructive = options.isDestructive !== undefined 
        ? options.isDestructive 
        : (typeof message === 'string' && /삭제|탈퇴|정지|취소|경고/.test(message));

      setModalState({
        isOpen: true,
        title: options.title || '확인',
        message: typeof message === 'string' ? message : String(message ?? ''),
        type: options.type || 'confirm',
        confirmText: options.confirmText || '확인',
        cancelText: options.cancelText || '취소',
        showCancel: true,
        isDestructive: isAutoDestructive,
        resolve,
      });
    });
  }, []);

  // 3. Toast 알림 오픈
  const showToast = useCallback((message, options = {}) => {
    const id = Date.now() + Math.random().toString(36).substring(2, 6);
    const type = typeof options === 'string' ? options : (options.type || 'info');
    const duration = options.duration || 3000;

    const newToast = {
      id,
      message: typeof message === 'string' ? message : String(message ?? ''),
      type,
    };

    setToasts((prev) => [...prev, newToast]);

    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, duration);

    return id;
  }, []);

  const dismissToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  // 모달 닫기 및 응답 처리
  const handleConfirm = useCallback(() => {
    if (resolveRef.current) {
      resolveRef.current(true);
      resolveRef.current = null;
    }
    setModalState((prev) => ({ ...prev, isOpen: false }));
  }, []);

  const handleCancel = useCallback(() => {
    if (resolveRef.current) {
      resolveRef.current(false);
      resolveRef.current = null;
    }
    setModalState((prev) => ({ ...prev, isOpen: false }));
  }, []);

  // 전역 window.alert 및 브릿지 등록
  useEffect(() => {
    globalModalBridge.showAlert = showAlert;
    globalModalBridge.showConfirm = showConfirm;
    globalModalBridge.showToast = showToast;

    // 브라우저 기본 window.alert 완전 대체 (절대 시스템 팝업이 뜨지 않음)
    const originalAlert = window.alert;
    window.alert = (msg) => {
      showAlert(msg, { title: '알림' });
    };

    return () => {
      window.alert = originalAlert;
    };
  }, [showAlert, showConfirm, showToast]);

  return (
    <ModalContext.Provider value={{ showAlert, showConfirm, showToast }}>
      {children}
      <AppModal
        isOpen={modalState.isOpen}
        title={modalState.title}
        message={modalState.message}
        type={modalState.type}
        confirmText={modalState.confirmText}
        cancelText={modalState.cancelText}
        showCancel={modalState.showCancel}
        isDestructive={modalState.isDestructive}
        onConfirm={handleConfirm}
        onCancel={handleCancel}
      />
      <AppToastContainer toasts={toasts} onDismiss={dismissToast} />
    </ModalContext.Provider>
  );
}

/**
 * 💡 Hook: useModal
 * @returns {{ showAlert: Function, showConfirm: Function, showToast: Function }}
 */
export function useModal() {
  const context = useContext(ModalContext);
  if (!context) {
    throw new Error('useModal must be used within a ModalProvider');
  }
  return context;
}

export const modal = globalModalBridge;

export default ModalContext;
