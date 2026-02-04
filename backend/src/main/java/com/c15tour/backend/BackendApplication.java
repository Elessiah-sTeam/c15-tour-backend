package com.c15tour.backend;

import com.c15tour.backend.service.RoutingService;
import com.c15tour.model.Coordinates;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	CommandLineRunner run(RoutingService service)
	{
		List<Coordinates> coordinatesList = new ArrayList<>();
		Coordinates c1 = new Coordinates(-1.525438,47.258878);
		Coordinates c2 = new Coordinates(-1.516375,47.254050);
		coordinatesList.add(c1);
		coordinatesList.add(c2);

		return args -> {
			System.out.println(service.calculateRoute(coordinatesList));
		};
	}
}
