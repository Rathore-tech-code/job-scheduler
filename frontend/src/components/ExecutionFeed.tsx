import type { Execution } from '../types';
import { ExecutionStatusBadge } from './StatusBadge';

interface ExecutionFeedProps {
  executions: Execution[];
  liveConnected: boolean;
}

export function ExecutionFeed({ executions, liveConnected }: ExecutionFeedProps) {
  return (
    <div className="rounded border border-hairline">
      <div className="flex items-center justify-between border-b border-hairline px-4 py-3">
        <h3 className="font-display text-sm font-medium text-ink">Live executions</h3>
        <span className="flex items-center gap-1.5 text-xs text-muted">
          <span className={`h-1.5 w-1.5 rounded-full ${liveConnected ? 'bg-mint' : 'bg-faint'}`} />
          {liveConnected ? 'Connected' : 'Reconnecting…'}
        </span>
      </div>

      {executions.length === 0 ? (
        <div className="px-4 py-10 text-center text-sm text-muted">
          Waiting for jobs to run — this feed updates in real time.
        </div>
      ) : (
        <ul className="max-h-96 divide-y divide-hairline/60 overflow-y-auto">
          {executions.map((e) => (
            <li key={`${e.id}-${e.status}`} className="flex items-center justify-between px-4 py-2.5">
              <div className="min-w-0">
                <p className="truncate text-sm text-ink">{e.jobName}</p>
                <p className="font-mono text-xs text-faint">
                  {e.workerId ?? '—'} · attempt {e.retryCount + 1}
                </p>
              </div>
              <ExecutionStatusBadge status={e.status} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
