import { useEffect, useState, useCallback } from 'react';
import { api, clearToken, ApiError } from '../api/client';
import type { CreateJobRequest, Execution, Job } from '../types';
import { useExecutionStream } from '../hooks/useExecutionStream';
import { StatStrip } from './StatStrip';
import { JobList } from './JobList';
import { JobForm } from './JobForm';
import { DagView } from './DagView';
import { ExecutionFeed } from './ExecutionFeed';
import { ErrorState } from './EmptyState';

export function Dashboard({ username, onLogout }: { username: string; onLogout: () => void }) {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [recentExecutions, setRecentExecutions] = useState<Execution[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [selectedJobId, setSelectedJobId] = useState<string | undefined>(undefined);

  const { events: liveEvents, connected } = useExecutionStream();

  const loadAll = useCallback(async () => {
    setError(null);
    try {
      const [jobList, executions] = await Promise.all([api.listJobs(), api.recentExecutions()]);
      setJobs(jobList);
      setRecentExecutions(executions);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not reach the scheduler API.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadAll();
    const interval = setInterval(loadAll, 8000); // periodic resync alongside the live WS feed
    return () => clearInterval(interval);
  }, [loadAll]);

  // Merge live WebSocket events on top of the periodic poll for the feed.
  const feed = liveEvents.length > 0 ? liveEvents : recentExecutions;

  async function handleCreateJob(payload: CreateJobRequest) {
    await api.createJob(payload);
    await loadAll();
  }

  async function handleTrigger(jobId: string) {
    try {
      await api.triggerJob(jobId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to trigger job.');
    }
  }

  async function handleToggleStatus(job: Job) {
    const next = job.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE';
    try {
      await api.setJobStatus(job.id, next);
      await loadAll();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to update job status.');
    }
  }

  return (
    <div className="min-h-screen bg-base font-body text-ink">
      <header className="border-b border-hairline">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-full border border-amber/30 text-amber">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <circle cx="12" cy="12" r="3" />
                <path d="M12 2v4M12 18v4M22 12h-4M6 12H2M19 5l-2.8 2.8M7.8 16.2 5 19M19 19l-2.8-2.8M7.8 7.8 5 5" strokeLinecap="round" />
              </svg>
            </div>
            <div>
              <h1 className="font-display text-sm font-semibold leading-none text-ink">Orbit</h1>
              <p className="text-xs leading-none text-faint mt-1">Job scheduler console</p>
            </div>
          </div>
          <div className="flex items-center gap-4">
            <span className="text-sm text-muted">{username}</span>
            <button
              onClick={() => {
                clearToken();
                onLogout();
              }}
              className="text-sm text-muted transition hover:text-ink"
            >
              Sign out
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl space-y-6 px-6 py-6">
        {error ? (
          <ErrorState message={error} onRetry={loadAll} />
        ) : (
          <>
            <StatStrip jobs={jobs} executions={[...recentExecutions, ...liveEvents]} />

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
              <div className="space-y-4 lg:col-span-2">
                <div className="flex items-center justify-between">
                  <h2 className="font-display text-sm font-medium text-ink">Jobs</h2>
                  <button
                    onClick={() => setShowForm(true)}
                    className="rounded bg-amber px-3 py-1.5 text-xs font-medium text-base transition hover:bg-amber/90"
                  >
                    New job
                  </button>
                </div>
                <JobList
                  jobs={jobs}
                  loading={loading}
                  selectedJobId={selectedJobId}
                  onSelect={setSelectedJobId}
                  onTrigger={handleTrigger}
                  onToggleStatus={handleToggleStatus}
                  onCreateClick={() => setShowForm(true)}
                />

                <div>
                  <h2 className="mb-3 font-display text-sm font-medium text-ink">Workflow graph</h2>
                  <div className="rounded border border-hairline bg-panel p-4">
                    <DagView jobs={jobs} selectedJobId={selectedJobId} onSelect={setSelectedJobId} />
                  </div>
                </div>
              </div>

              <div>
                <ExecutionFeed executions={feed} liveConnected={connected} />
              </div>
            </div>
          </>
        )}
      </main>

      {showForm && (
        <JobForm existingJobs={jobs} onSubmit={handleCreateJob} onClose={() => setShowForm(false)} />
      )}
    </div>
  );
}
