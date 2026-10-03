package com.myfinance.track;

import org.springframework.boot.SpringApplication;

public class TestTrackApplication {

	public static void main(String[] args) {
		SpringApplication.from(TrackApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
