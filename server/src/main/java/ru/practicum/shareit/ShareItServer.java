package ru.practicum.shareit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class ShareItServer {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(System.getenv().getOrDefault("TZ", "Europe/Moscow")));
		SpringApplication.run(ShareItServer.class, args);
	}

}
