package com.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class SchedulerApplication {

    public static void main(String[] args) {
        // Force the JVM-wide default TimeZone to the canonical IANA id
        // *before* Spring builds the DataSource. On some JVM/OS/tzdata
        // combinations, the "India" zone resolves to the deprecated
        // pre-1996 alias "Asia/Calcutta" instead of "Asia/Kolkata". The
        // Postgres JDBC driver (pgjdbc) always sends
        // TimeZone.getDefault().getID() as a parameter in the connection
        // startup packet, and PostgreSQL builds linked against current
        // tzdata reject that obsolete alias immediately with "FATAL:
        // invalid value for parameter \"TimeZone\"" -- before any SQL runs.
        //
        // This is the ONLY verified-effective fix. Datasource/URL-level
        // overrides (a "timezone" connection property, "options=-c
        // TimeZone=...") were tested directly against a real PostgreSQL
        // instance and do NOT work: pgjdbc unconditionally derives its own
        // startup-packet value from the JVM default regardless of either,
        // which is a currently open pgjdbc bug (pgjdbc/pgjdbc#3642). So the
        // JVM default must be correct before any connection is opened.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

        SpringApplication.run(SchedulerApplication.class, args);
    }
}
