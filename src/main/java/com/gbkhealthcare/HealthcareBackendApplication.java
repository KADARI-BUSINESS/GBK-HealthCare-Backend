package com.gbkhealthcare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.gbkhealthcare.config.LocalDatasourceSafetyInitializer;

@SpringBootApplication
public class HealthcareBackendApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(HealthcareBackendApplication.class);
        application.addInitializers(new LocalDatasourceSafetyInitializer());
        application.run(args);
    }
}
