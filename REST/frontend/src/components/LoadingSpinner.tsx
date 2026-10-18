interface LoadingSpinnerProps {
  /** Optional caption shown under the spinner. */
  caption?: string;
}

export function LoadingSpinner({ caption }: LoadingSpinnerProps) {
  return (
    <div className="text-center py-5">
      <div className="spinner-border text-primary" role="status">
        <span className="visually-hidden">Loading...</span>
      </div>
      {caption && <p className="mt-3 text-muted">{caption}</p>}
    </div>
  );
}
