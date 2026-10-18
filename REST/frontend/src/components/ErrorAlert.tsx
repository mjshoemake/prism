interface ErrorAlertProps {
  message: string;
  className?: string;
}

export function ErrorAlert({ message, className = '' }: ErrorAlertProps) {
  return (
    <div className={`alert alert-danger ${className}`} role="alert">
      <i className="fas fa-exclamation-triangle me-2" />
      {message}
    </div>
  );
}
