package com.gym.gym_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
/**
 * Punto de entrada principal de la aplicación de gestión de gimnasio.
 * Desde aquí se inicializa el contexto de Spring Boot.
 */
// @SpringBootApplication combina configuración, auto-configuración y descubrimiento
// de componentes: es el punto donde Spring Boot prepara el contexto de la aplicación.
@SpringBootApplication
// @EnableScheduling habilita la ejecución de métodos anotados con @Scheduled.
@EnableScheduling
public class GymManagementApplication {

	public static void main(String[] args) {
		// Crea el contexto de Spring, registra los beans y arranca el servidor web.
		SpringApplication.run(GymManagementApplication.class, args);
	}

}
