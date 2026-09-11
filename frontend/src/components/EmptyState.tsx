interface EmptyStateProps {
  title: string;
  description: string;
  action?: { label: string; onClick: () => void };
}

export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded border border-dashed border-hairline px-6 py-14 text-center">
      <div className="flex h-10 w-10 items-center justify-center rounded-full border border-hairline text-faint">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <circle cx="12" cy="12" r="9" />
          <path d="M12 8v4l2.5 2.5" strokeLinecap="round" />
        </svg>
      </div>
      <div>
        <p className="font-display text-sm font-medium text-ink">{title}</p>
        <p className="mt-1 max-w-xs text-sm text-muted">{description}</p>
      </div>
      {action && (
        <button
          onClick={action.onClick}
          className="mt-2 rounded border border-hairline bg-raised px-3 py-1.5 text-sm font-medium text-ink transition hover:border-amber/40 hover:text-amber"
        >
          {action.label}
        </button>
      )}
    </div>
  );
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded border border-coral/30 bg-coral/5 px-6 py-14 text-center">
      <div className="flex h-10 w-10 items-center justify-center rounded-full border border-coral/30 text-coral">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <path d="M12 3l9 16H3l9-16z" strokeLinejoin="round" />
          <path d="M12 10v4" strokeLinecap="round" />
          <circle cx="12" cy="17" r="0.5" fill="currentColor" />
        </svg>
      </div>
      <div>
        <p className="font-display text-sm font-medium text-ink">Something didn't load</p>
        <p className="mt-1 max-w-xs text-sm text-muted">{message}</p>
      </div>
      {onRetry && (
        <button
          onClick={onRetry}
          className="mt-2 rounded border border-hairline bg-raised px-3 py-1.5 text-sm font-medium text-ink transition hover:border-coral/40 hover:text-coral"
        >
          Try again
        </button>
      )}
    </div>
  );
}

export function LoadingRows({ count = 4 }: { count?: number }) {
  return (
    <div className="space-y-2">
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="h-12 animate-pulse rounded border border-hairline bg-raised/60" />
      ))}
    </div>
  );
}
