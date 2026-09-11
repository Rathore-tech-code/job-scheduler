package com.scheduler.soap.gen;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"jobId", "name", "status", "nextRunAt"})
@XmlRootElement(name = "getJobStatusResponse", namespace = "http://scheduler.com/soap/jobs")
public class GetJobStatusResponse {

    @XmlElement(namespace = "http://scheduler.com/soap/jobs", required = true)
    private String jobId;

    @XmlElement(namespace = "http://scheduler.com/soap/jobs", required = true)
    private String name;

    @XmlElement(namespace = "http://scheduler.com/soap/jobs", required = true)
    private String status;

    @XmlElement(namespace = "http://scheduler.com/soap/jobs")
    private String nextRunAt;

    public GetJobStatusResponse() {}

    public GetJobStatusResponse(String jobId, String name, String status, String nextRunAt) {
        this.jobId = jobId;
        this.name = name;
        this.status = status;
        this.nextRunAt = nextRunAt;
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNextRunAt() { return nextRunAt; }
    public void setNextRunAt(String nextRunAt) { this.nextRunAt = nextRunAt; }
}
