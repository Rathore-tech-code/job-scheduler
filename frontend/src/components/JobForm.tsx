import { useState, type FormEvent } from 'react';
import type { CreateJobRequest, Job } from '../types';

interface JobFormProps {
  existingJobs: Job[];
  onSubmit: (payload: CreateJobRequest) => Promise<void>;
  onClose: () => void;
}

const CRON_PRESETS = [
  { label: 'Every 5 minutes', value: '*/5 * * * *' },
  { label: 'Every hour', value: '0 * * * *' },
  { label: 'Daily at 2am', value: '0 2 * * *' },
  { label: 'Weekdays at 9am', value: '0 9 * * MON-FRI' }
];

export function JobForm({ existingJobs, onSubmit, onClose }: JobFormProps) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [cronExpression, setCronExpression] = useState(CRON_PRESETS[0].value);
  const [maxRetries, setMaxRetries] = useState(3);
  const [dependsOn, setDependsOn] = useState<string[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!name.trim() || !cronExpression.trim()) {
      setError('Name and schedule are required.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await onSubmit({
        name: name.trim(),
        description: description.trim() || undefined,
        cronExpression: cronExpression.trim(),
        maxRetries,
        dependsOnJobNames: dependsOn
      });
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create job.');
    } finally {
      setSubmitting(false);
    }
  }

  function toggleDependency(jobName: string) {
    setDependsOn((prev) => (prev.includes(jobName) ? prev.filter((n) => n !== jobName) : [...prev, jobName]));
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 px-4">
      <div className="w-full max-w-lg rounded border border-hairline bg-panel p-6 shadow-2xl">
        <div className="mb-5 flex items-start justify-between">
          <div>
            <h2 className="font-display text-lg font-semibold text-ink">New job</h2>
            <p className="mt-1 text-sm text-muted">Define a schedule and, optionally, upstream dependencies.</p>
          </div>
          <button onClick={onClose} className="text-muted transition hover:text-ink" aria-label="Close">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
              <path d="M6 6l12 12M18 6L6 18" strokeLinecap="round" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="mb-1 block text-xs font-medium text-muted">Name</label>
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="nightly-report"
              className="w-full rounded border border-hairline bg-raised px-3 py-2 text-sm text-ink placeholder:text-faint focus:border-amber/50 focus:outline-none"
            />
          </div>

          <div>
            <label className="mb-1 block text-xs font-medium text-muted">Description (optional)</label>
            <input
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Generates the nightly summary report"
              className="w-full rounded border border-hairline bg-raised px-3 py-2 text-sm text-ink placeholder:text-faint focus:border-amber/50 focus:outline-none"
            />
          </div>

          <div>
            <label className="mb-1 block text-xs font-medium text-muted">Schedule (cron)</label>
            <input
              value={cronExpression}
              onChange={(e) => setCronExpression(e.target.value)}
              className="w-full rounded border border-hairline bg-raised px-3 py-2 font-mono text-sm text-ink focus:border-amber/50 focus:outline-none"
            />
            <div className="mt-2 flex flex-wrap gap-1.5">
              {CRON_PRESETS.map((p) => (
                <button
                  type="button"
                  key={p.value}
                  onClick={() => setCronExpression(p.value)}
                  className={`rounded-sm border px-2 py-1 text-xs transition ${
                    cronExpression === p.value
                      ? 'border-amber/40 text-amber'
                      : 'border-hairline text-muted hover:text-ink'
                  }`}
                >
                  {p.label}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="mb-1 block text-xs font-medium text-muted">Max retries on failure</label>
            <input
              type="number"
              min={0}
              max={10}
              value={maxRetries}
              onChange={(e) => setMaxRetries(Number(e.target.value))}
              className="w-24 rounded border border-hairline bg-raised px-3 py-2 text-sm text-ink focus:border-amber/50 focus:outline-none"
            />
          </div>

          {existingJobs.length > 0 && (
            <div>
              <label className="mb-1 block text-xs font-medium text-muted">
                Depends on (must succeed first)
              </label>
              <div className="flex max-h-28 flex-wrap gap-1.5 overflow-y-auto rounded border border-hairline bg-raised p-2">
                {existingJobs.map((job) => (
                  <button
                    type="button"
                    key={job.id}
                    onClick={() => toggleDependency(job.name)}
                    className={`rounded-sm border px-2 py-1 text-xs transition ${
                      dependsOn.includes(job.name)
                        ? 'border-mint/40 text-mint'
                        : 'border-hairline text-muted hover:text-ink'
                    }`}
                  >
                    {job.name}
                  </button>
                ))}
              </div>
            </div>
          )}

          {error && <p className="text-sm text-coral">{error}</p>}

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded border border-hairline px-4 py-2 text-sm font-medium text-muted transition hover:text-ink"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="rounded bg-amber px-4 py-2 text-sm font-medium text-base transition hover:bg-amber/90 disabled:opacity-50"
            >
              {submitting ? 'Creating…' : 'Create job'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
