package com.scheduler.soap.gen;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"jobId"})
@XmlRootElement(name = "getJobStatusRequest", namespace = "http://scheduler.com/soap/jobs")
public class GetJobStatusRequest {

    @XmlElement(namespace = "http://scheduler.com/soap/jobs", required = true)
    private String jobId;

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
}
