import React from 'react';
import './PrimaryButton.css';

interface PrimaryButtonProps {
  title: string;
  onClick: () => void;
  isLoading?: boolean;
  type?: 'button' | 'submit';
  className?: string;
}

const PrimaryButton: React.FC<PrimaryButtonProps> = ({
  title,
  onClick,
  isLoading = false,
  type = 'button',
  className = '',
}) => {
  return (
    <button
      className={`primary-btn ${className}`}
      type={type}
      onClick={onClick}
      disabled={isLoading}
      aria-busy={isLoading}
    >
      {isLoading ? (
        <span className="primary-btn__spinner" aria-hidden="true" />
      ) : (
        <span className="primary-btn__label">{title}</span>
      )}
    </button>
  );
};

export default PrimaryButton;
