package com.mazurek.eventOrganizer;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@RequiredArgsConstructor
public class EventOrganizerApplication {



	public static void main(String[] args) {
		SpringApplication.run(EventOrganizerApplication.class, args);

	}

}
