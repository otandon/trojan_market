import { createContext, useCallback, useContext, useMemo, useState } from 'react';

const ToastContext = createContext(null);

const STYLES = {
  success: 'bg-green-600 text-white',
  error: 'bg-red-600 text-white',
  info: 'bg-gray-800 text-white',
};

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const dismiss = useCallback((id) => {
    setToasts((list) => list.filter((t) => t.id !== id));
  }, []);

  const show = useCallback((message, type = 'info', durationMs = 4000) => {
    const id = Date.now() + Math.random();
    setToasts((list) => [...list, { id, message, type }]);
    if (durationMs > 0) {
      setTimeout(() => dismiss(id), durationMs);
    }
    return id;
  }, [dismiss]);

  const value = useMemo(() => ({
    show,
    success: (m, d) => show(m, 'success', d),
    error: (m, d) => show(m, 'error', d),
    info: (m, d) => show(m, 'info', d),
    dismiss,
  }), [show, dismiss]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="pointer-events-none fixed bottom-4 right-4 z-50 flex w-full max-w-sm flex-col gap-2">
        {toasts.map((t) => (
          <div
            key={t.id}
            role="alert"
            className={`pointer-events-auto flex items-start justify-between gap-3 rounded-md px-4 py-3 text-sm shadow-lg ${STYLES[t.type] || STYLES.info}`}
          >
            <span className="flex-1 whitespace-pre-line">{t.message}</span>
            <button
              type="button"
              onClick={() => dismiss(t.id)}
              className="text-white/80 hover:text-white"
              aria-label="Dismiss"
            >
              ✕
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used inside a ToastProvider');
  return ctx;
}

export function apiErrorMessage(err, fallback = 'Something went wrong') {
  return err?.response?.data?.error || err?.message || fallback;
}
