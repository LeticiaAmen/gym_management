package com.gym.gym_management.authentication;


import com.gym.gym_management.configuration.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthenticationController(AuthenticationService authenticationService, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register (
            @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login (@RequestBody AuthenticationRequest request) {
        try{
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            String token = jwtService.generateToken(authentication);
            return ResponseEntity.ok(new AuthenticationResponse(token));
        } catch (AuthenticationException ex){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
