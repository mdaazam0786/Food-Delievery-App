/**
 * ConfirmDialog.tsx — Simple confirmation dialog for user actions.
 * Used for cart operations and other confirmations.
 */

import React from 'react';
import './ConfirmDialog.css';

// ─── Icons ────────────────────────────────────────────────────────────────────

const AlertIcon = () => (
  <svg width="40" height="40" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="12" cy="12" r="10" stroke="#F59E0B" strokeWidth="2"/>
    <line x1="12" y1="8" x2="12" y2="12" stroke="#F59E0B" strokeWidth="2.2" strokeLinecap="round"/>
    <circle cx="12" cy="16" r="1.2" fill="#F59E0B"/>
  </svg>
);

// ─── Component ────────────────────────────────────────────────────────────────

export interface ConfirmDialogProps {
  title?: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  onConfirm: () => void;
  onCancel: () => void;
  isDangerous?: boolean;
}

const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  title,
  message,
  confirmText = 'Yes',
  cancelText = 'No',
  onConfirm,
  onCancel,
  isDangerous = false,
}) => {
  return (
    <div className="cd-overlay" role="presentation" onClick={onCancel}>
      <div className="cd-modal" role="alertdialog" onClick={(e) => e.stopPropagation()}>
        <div className="cd-modal__icon">
          <AlertIcon />
        </div>

        {title && <h2 className="cd-modal__title">{title}</h2>}

        <p className="cd-modal__message">{message}</p>

        <div className="cd-modal__actions">
          <button
            className="cd-modal__btn cd-modal__btn--cancel"
            onClick={onCancel}
            autoFocus
          >
            {cancelText}
          </button>
          <button
            className={`cd-modal__btn cd-modal__btn--confirm ${isDangerous ? 'cd-modal__btn--danger' : ''}`}
            onClick={onConfirm}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmDialog;
