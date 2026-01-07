package com.hospitalfinder.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.admin.SpringApplicationAdminJmxAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
	SpringApplicationAdminJmxAutoConfiguration.class,
	JmxAutoConfiguration.class,
	WebSocketServletAutoConfiguration.class
})
@EnableScheduling
@EnableAsync
public class HospitalBookingBackendApplication {

	private static final Logger logger = LoggerFactory.getLogger(HospitalBookingBackendApplication.class);
	
	private final Environment env;

	public HospitalBookingBackendApplication(Environment env) {
		this.env = env;
	}

	public static void main(String[] args) {

		SpringApplication.run(HospitalBookingBackendApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		int port = env.getProperty("server.port", Integer.class, 9000);
		logger.info("============================================================");
		logger.info("APPLICATION STARTED SUCCESSFULLY AND READY TO ACCEPT REQUESTS");
		logger.info("Server listening on port: {}", port);
		logger.info("============================================================");
	}

}
