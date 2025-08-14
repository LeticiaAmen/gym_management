package com.gym.gym_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.SQLOutput;
/**
 * Punto de entrada principal de la aplicación de gestión de gimnasio.
 * Desde aquí se inicializa el contexto de Spring Boot.
 */
@SpringBootApplication
public class GymManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(GymManagementApplication.class, args);
	}

}
