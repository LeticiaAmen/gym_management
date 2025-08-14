package com.gym.gym_management.model;


import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Entidad JPA que representa a un usuario del sistema (administrador o cliente).
 *
 * Características:
 * - Implementa UserDetails para integrarse con Spring Security.
 * - Contiene credenciales (email y contraseña) y el rol asignado (Role enum).
 * - Soporta un patrón Builder para facilitar su creación.
 *
 * Relación con los requerimientos:
 * - "Usuarios y Roles": define el acceso a la aplicación según el rol (USER o CLIENT).
 * - Utilizada en el proceso de autenticación (login) y para verificar permisos en endpoints protegidos.
 * - Asociada a Client en una relación 1:1 cuando el usuario es un cliente del gimnasio.
 */
@Entity
@Table(name = "users")
public class User implements UserDetails {

    /**
     * Identificador único del usuario.
     * Generado automáticamente (autoincremental).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // correo que sirve como nombre de usuario. debe ser unico y no nulo
    @Column(nullable = false, unique = true)
    private String email;

    //contraseña encriptada del usuario
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    public User() {
    }

    public User(Long id, String email, String password, Role role) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
    }

   // === MÉTODOS DE UserDetails (Spring Security) ===

    /**
     * Devuelve la lista de autoridades (permisos) del usuario.
     * Spring Security requiere que los roles tengan el prefijo "ROLE_".
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

// === PATRÓN BUILDER (creación fluida de instancias) ===

    /**
     * Inicia la construcción de un objeto User usando el patrón Builder.
     */
    public static UserBuilder builder() {
        return new UserBuilder();

    }

    /**
     * Clase interna que implementa el patrón Builder para User.
     */
    public static class UserBuilder {
        private Long id;
        private String email;
        private String password;
        private Role role;

        public UserBuilder() {
        }

        public UserBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public User build() {
            User user = new User();
            user.setId(this.id);
            user.setEmail(this.email);
            user.setPassword(this.password);
            user.setRole(this.role);
            return user;
        }
    }


    //Getter y setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
