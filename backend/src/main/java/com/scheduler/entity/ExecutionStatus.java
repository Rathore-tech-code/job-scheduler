package com.scheduler.entity;

public enum ExecutionStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    RETRYING,
    SKIPPED_CYCLE,
    WAITING_ON_DEPENDENCY
}
