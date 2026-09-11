import type { ExecutionStatus, JobStatus } from '../types';

const EXEC_STYLES: Record<ExecutionStatus, string> = {
  QUEUED: 'text-slate bg-slate/10 border-slate/30',
  RUNNING: 'text-amber bg-amber/10 border-amber/30',
  RETRYING: 'text-amber bg-amber/10 border-amber/30',
  SUCCEEDED: 'text-mint bg-mint/10 border-mint/30',
  FAILED: 'text-coral bg-coral/10 border-coral/30',
  SKIPPED_CYCLE: 'text-faint bg-faint/10 border-faint/30',
  WAITING_ON_DEPENDENCY: 'text-slate bg-slate/10 border-slate/30'
};

const JOB_STYLES: Record<JobStatus, string> = {
  ACTIVE: 'text-mint bg-mint/10 border-mint/30',
  PAUSED: 'text-amber bg-amber/10 border-amber/30',
  DISABLED: 'text-faint bg-faint/10 border-faint/30'
};

const LABELS: Record<string, string> = {
  QUEUED: 'Queued',
  RUNNING: 'Running',
  RETRYING: 'Retrying',
  SUCCEEDED: 'Succeeded',
  FAILED: 'Failed',
  SKIPPED_CYCLE: 'Skipped',
  WAITING_ON_DEPENDENCY: 'Waiting',
  ACTIVE: 'Active',
  PAUSED: 'Paused',
  DISABLED: 'Disabled'
};

export function ExecutionStatusBadge({ status }: { status: ExecutionStatus }) {
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-sm border px-2 py-0.5 text-xs font-medium ${EXEC_STYLES[status]}`}>
      <span className="h-1.5 w-1.5 rounded-full bg-current" />
      {LABELS[status]}
    </span>
  );
}

export function JobStatusBadge({ status }: { status: JobStatus }) {
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-sm border px-2 py-0.5 text-xs font-medium ${JOB_STYLES[status]}`}>
      <span className="h-1.5 w-1.5 rounded-full bg-current" />
      {LABELS[status]}
    </span>
  );
}
