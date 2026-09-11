package com.scheduler.soap;

import com.scheduler.dto.CreateJobRequest;
import com.scheduler.dto.JobResponse;
import com.scheduler.entity.JobStatus;
import com.scheduler.exception.JobNotFoundException;
import com.scheduler.service.JobService;
import com.scheduler.soap.gen.*;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import java.util.List;
import java.util.UUID;

/**
 * SOAP surface for legacy/enterprise clients that can't speak REST/JSON.
 * Mirrors the two most common REST operations (submit a job, check its
 * status) over XML, backed by the same JobService used by JobController.
 */
@Endpoint
public class JobSoapEndpoint {

    private static final String NAMESPACE_URI = "http://scheduler.com/soap/jobs";

    private final JobService jobService;

    public JobSoapEndpoint(JobService jobService) {
        this.jobService = jobService;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "submitJobRequest")
    @ResponsePayload
    public SubmitJobResponse submitJob(@RequestPayload SubmitJobRequest request) {
        CreateJobRequest createRequest = new CreateJobRequest(
                request.getName(),
                "Submitted via SOAP",
                request.getCronExpression(),
                request.getPayload(),
                request.getMaxRetries(),
                List.of()
        );
        JobResponse created = jobService.createJob(createRequest, "soap-client");
        return new SubmitJobResponse(
                created.id().toString(),
                created.status().name(),
                created.nextRunAt() != null ? created.nextRunAt().toString() : ""
        );
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getJobStatusRequest")
    @ResponsePayload
    public GetJobStatusResponse getJobStatus(@RequestPayload GetJobStatusRequest request) {
        try {
            UUID id = UUID.fromString(request.getJobId());
            JobResponse job = jobService.getJob(id);
            return new GetJobStatusResponse(
                    job.id().toString(),
                    job.name(),
                    job.status().name(),
                    job.nextRunAt() != null ? job.nextRunAt().toString() : null
            );
        } catch (IllegalArgumentException e) {
            throw new JobNotFoundException("Invalid or unknown job id: " + request.getJobId());
        }
    }
}
