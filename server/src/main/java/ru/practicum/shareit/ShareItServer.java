package ru.practicum.shareit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class ShareItServer {

	public static void main(String[] args) {
		String timeZone = System.getenv("TZ");
		if (timeZone != null && !timeZone.isBlank()) {
			TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
		}
		SpringApplication.run(ShareItServer.class, args);
	}

}
