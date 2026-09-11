package com.scheduler.soap.gen;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"jobId", "status", "nextRunAt"})
@XmlRootElement(name = "submitJobResponse", namespace = "http://scheduler.com/soap/jobs")
public class SubmitJobResponse {

    @XmlElement(namespace = "http://scheduler.com/soap/jobs", required = true)
    private String jobId;

    @XmlElement(namespace = "http://scheduler.com/soap/jobs", required = true)
    private String status;

    @XmlElement(namespace = "http://scheduler.com/soap/jobs", required = true)
    private String nextRunAt;

    public SubmitJobResponse() {}

    public SubmitJobResponse(String jobId, String status, String nextRunAt) {
        this.jobId = jobId;
        this.status = status;
        this.nextRunAt = nextRunAt;
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNextRunAt() { return nextRunAt; }
    public void setNextRunAt(String nextRunAt) { this.nextRunAt = nextRunAt; }
}
