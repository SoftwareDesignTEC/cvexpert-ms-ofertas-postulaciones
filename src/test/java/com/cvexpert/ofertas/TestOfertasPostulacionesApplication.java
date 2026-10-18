package com.cvexpert.ofertas;

import org.springframework.boot.SpringApplication;

public class TestOfertasPostulacionesApplication {

	public static void main(String[] args) {
		SpringApplication.from(OfertasPostulacionesApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
