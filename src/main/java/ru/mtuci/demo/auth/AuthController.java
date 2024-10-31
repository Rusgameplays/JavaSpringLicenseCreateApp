package ru.mtuci.demo.auth;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import lombok.RequiredArgsConstructor;
import ru.mtuci.demo.configuration.JwtTokenProvider;
import ru.mtuci.demo.services.UserService;
import ru.mtuci.demo.services.impl.UserAlreadyCreate;


@RequiredArgsConstructor
@RequestMapping("/auth")
@RestController
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtProvider;
    private final UserService userService;

    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody SignInRequest request) {
        try {
            return ResponseEntity.ok(new SignInResponse(request.getEmail(), jwtProvider.createToken(request.getEmail(),
                    authenticationManager
                            .authenticate(
                                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()))
                            .getAuthorities().stream().collect(Collectors.toSet()))));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid password");
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody RegRequest request) {
        try {
            userService.create(request.getEmail(), request.getName(), request.getPassword());
            return ResponseEntity.ok("User created");
        } catch (UserAlreadyCreate ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ex.getMessage());
        }
    }
}