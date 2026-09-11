package com.scheduler.soap.gen;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"name", "cronExpression", "payload", "maxRetries"})
@XmlRootElement(name = "submitJobRequest", namespace = "http://scheduler.com/soap/jobs")
public class SubmitJobRequest {

    @XmlElement(namespace = "http://scheduler.com/soap/jobs", required = true)
    private String name;

    @XmlElement(namespace = "http://scheduler.com/soap/jobs", required = true)
    private String cronExpression;

    @XmlElement(namespace = "http://scheduler.com/soap/jobs")
    private String payload;

    @XmlElement(namespace = "http://scheduler.com/soap/jobs")
    private Integer maxRetries;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
}
