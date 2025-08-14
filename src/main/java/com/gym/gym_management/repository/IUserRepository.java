package com.gym.gym_management.repository;


import com.gym.gym_management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad User.
 *
 * Extiende JpaRepository, lo que proporciona métodos CRUD estándar:
 * - findAll(), findById(), save(), deleteById(), etc.
 *
 * Métodos personalizados:
 * - findByEmail(String email):
 *   Busca un usuario por su correo electrónico, retornando un Optional<User>.
 *
 * Relación con los requerimientos:
 * - "Usuarios y Roles": permite obtener un usuario para autenticarlo
 *   en el proceso de login y determinar su rol.
 * - Usado en ApplicationConfig para cargar datos de usuario en el UserDetailsService.
 */
@Repository
public interface IUserRepository extends JpaRepository<User, Long> {
    //Busca un usuario por su dirección de correo electrónico.
    //@return Optional que contiene el usuario si existe, o vacío si no se encuentra.
    Optional<User> findByEmail(String email);
}
