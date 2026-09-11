import type { Job } from '../types';
import { JobStatusBadge } from './StatusBadge';
import { EmptyState, LoadingRows } from './EmptyState';

interface JobListProps {
  jobs: Job[];
  loading: boolean;
  selectedJobId?: string;
  onSelect: (jobId: string) => void;
  onTrigger: (jobId: string) => void;
  onToggleStatus: (job: Job) => void;
  onCreateClick: () => void;
}

export function JobList({ jobs, loading, selectedJobId, onSelect, onTrigger, onToggleStatus, onCreateClick }: JobListProps) {
  if (loading) return <LoadingRows count={5} />;

  if (jobs.length === 0) {
    return (
      <EmptyState
        title="No jobs scheduled"
        description="Create your first job to see it picked up by the scheduling engine."
        action={{ label: 'New job', onClick: onCreateClick }}
      />
    );
  }

  return (
    <div className="overflow-hidden rounded border border-hairline">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="border-b border-hairline bg-raised/50 text-xs text-muted">
            <th className="px-4 py-2 font-medium">Job</th>
            <th className="px-4 py-2 font-medium">Schedule</th>
            <th className="px-4 py-2 font-medium">Status</th>
            <th className="px-4 py-2 font-medium">Next run</th>
            <th className="px-4 py-2 font-medium">Depends on</th>
            <th className="px-4 py-2"></th>
          </tr>
        </thead>
        <tbody>
          {jobs.map((job) => (
            <tr
              key={job.id}
              onClick={() => onSelect(job.id)}
              className={`cursor-pointer border-b border-hairline/60 last:border-0 transition hover:bg-raised/40 ${
                selectedJobId === job.id ? 'bg-raised/60' : ''
              }`}
            >
              <td className="px-4 py-3">
                <p className="font-medium text-ink">{job.name}</p>
                {job.description && <p className="mt-0.5 text-xs text-muted">{job.description}</p>}
              </td>
              <td className="px-4 py-3 font-mono text-xs text-muted">{job.cronExpression}</td>
              <td className="px-4 py-3">
                <JobStatusBadge status={job.status} />
              </td>
              <td className="px-4 py-3 font-mono text-xs text-muted">{formatTime(job.nextRunAt)}</td>
              <td className="px-4 py-3 text-xs text-muted">
                {job.dependsOn.length > 0 ? job.dependsOn.join(', ') : '—'}
              </td>
              <td className="px-4 py-3">
                <div className="flex justify-end gap-2" onClick={(e) => e.stopPropagation()}>
                  <button
                    onClick={() => onTrigger(job.id)}
                    className="rounded border border-hairline px-2 py-1 text-xs font-medium text-ink transition hover:border-amber/40 hover:text-amber"
                  >
                    Run now
                  </button>
                  <button
                    onClick={() => onToggleStatus(job)}
                    className="rounded border border-hairline px-2 py-1 text-xs font-medium text-ink transition hover:border-slate/40 hover:text-slate"
                  >
                    {job.status === 'ACTIVE' ? 'Pause' : 'Activate'}
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function formatTime(iso: string | null): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}
