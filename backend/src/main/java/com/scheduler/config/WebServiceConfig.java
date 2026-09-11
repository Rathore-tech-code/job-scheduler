package com.scheduler.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

/**
 * Exposes the SOAP endpoint at /soap/jobs and auto-generates a WSDL at
 * /soap/jobs.wsdl from the XSD, so legacy clients can point a SOAP
 * client generator (wsimport, etc.) straight at it.
 */
@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
            org.springframework.web.context.WebApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/soap/*");
    }

    @Bean(name = "jobs")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema jobsSchema) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName("JobPort");
        wsdl11Definition.setLocationUri("/soap/jobs");
        wsdl11Definition.setTargetNamespace("http://scheduler.com/soap/jobs");
        wsdl11Definition.setSchema(jobsSchema);
        return wsdl11Definition;
    }

    @Bean
    public XsdSchema jobsSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/job-service.xsd"));
    }
}
