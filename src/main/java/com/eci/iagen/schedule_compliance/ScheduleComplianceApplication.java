package com.eci.iagen.schedule_compliance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

@SpringBootApplication
public class ScheduleComplianceApplication {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScheduleComplianceApplication.class);

	public static void main(String[] args) {
		// Load environment variables from .env file only if not already set
		try {
			Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
			dotenv.entries().forEach(entry -> {
				String key = entry.getKey();
				
				// Only set the system property if it is not already set
				logger.info("Setting system property: {}={}", key, entry.getValue());
				System.setProperty(key, entry.getValue());
			});
		} catch (DotenvException e) {
			logger.error("Error loading .env file", e);
		}
		SpringApplication.run(ScheduleComplianceApplication.class, args);
	}

}
