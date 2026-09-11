export type JobStatus = 'ACTIVE' | 'PAUSED' | 'DISABLED';

export type ExecutionStatus =
  | 'QUEUED'
  | 'RUNNING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'RETRYING'
  | 'SKIPPED_CYCLE'
  | 'WAITING_ON_DEPENDENCY';

export interface Job {
  id: string;
  name: string;
  description: string | null;
  cronExpression: string;
  status: JobStatus;
  maxRetries: number;
  nextRunAt: string | null;
  createdAt: string;
  dependsOn: string[];
}

export interface Execution {
  id: string;
  jobId: string;
  jobName: string;
  status: ExecutionStatus;
  workerId: string | null;
  retryCount: number;
  queuedAt: string;
  startedAt: string | null;
  finishedAt: string | null;
}

export interface CreateJobRequest {
  name: string;
  description?: string;
  cronExpression: string;
  payload?: string;
  maxRetries?: number;
  dependsOnJobNames?: string[];
}

export interface AuthResponse {
  token: string;
  username: string;
  role: string;
}

export interface ApiError {
  message: string;
  timestamp: string;
}
