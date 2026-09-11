import type { Execution, Job } from '../types';

export function StatStrip({ jobs, executions }: { jobs: Job[]; executions: Execution[] }) {
  const active = jobs.filter((j) => j.status === 'ACTIVE').length;
  const running = executions.filter((e) => e.status === 'RUNNING' || e.status === 'RETRYING').length;
  const finished = executions.filter((e) => e.status === 'SUCCEEDED' || e.status === 'FAILED');
  const successRate = finished.length === 0
    ? null
    : Math.round((finished.filter((e) => e.status === 'SUCCEEDED').length / finished.length) * 100);

  const nextUp = jobs
    .filter((j) => j.status === 'ACTIVE' && j.nextRunAt)
    .sort((a, b) => new Date(a.nextRunAt!).getTime() - new Date(b.nextRunAt!).getTime())[0];

  const stats = [
    { label: 'Active jobs', value: String(active) },
    { label: 'Running now', value: String(running) },
    { label: 'Success rate', value: successRate === null ? '—' : `${successRate}%` },
    { label: 'Next up', value: nextUp ? nextUp.name : '—' }
  ];

  return (
    <div className="grid grid-cols-2 gap-px overflow-hidden rounded border border-hairline bg-hairline sm:grid-cols-4">
      {stats.map((s) => (
        <div key={s.label} className="bg-panel px-4 py-3">
          <p className="text-xs text-muted">{s.label}</p>
          <p className="mt-1 truncate font-display text-lg font-semibold text-ink">{s.value}</p>
        </div>
      ))}
    </div>
  );
}
