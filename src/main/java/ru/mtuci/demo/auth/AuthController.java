package ru.mtuci.demo.auth;

import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import ru.mtuci.demo.configuration.JwtTokenProvider;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/auth")
@RestController
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtProvider;

//    @PostMapping("/login")
//    public ResponseEntity<String> authenticateUser(@RequestBody AuthRequestDTO request) {
//        try {
//            var auth = authenticationManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
//            if (!auth.isAuthenticated())
//                throw new Exception();
//            return ResponseEntity.ok(jwtProvider.createToken(auth.getName(),
//                    auth.getAuthorities().stream().collect(Collectors.toSet())));
//        } catch (Exception e) {
//            return ResponseEntity.status(401).body("Invalid credentials");
//        }
//    }
//}
@PostMapping("/login")
public ResponseEntity<String> authenticateUser(@RequestBody AuthRequestDTO request) {
    try {
        log.info("Authenticating user: {}", request.getUsername());
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        if (!auth.isAuthenticated()) {
            log.warn("Authentication failed for user: {}", request.getUsername());
            throw new Exception();
        }
        String token = jwtProvider.createToken(auth.getName(),
                auth.getAuthorities().stream().collect(Collectors.toSet()));
        log.info("Authentication successful for user: {}", request.getUsername());
        return ResponseEntity.ok(token);
    } catch (Exception e) {
        log.error("Authentication error for user: {}", request.getUsername(), e);
        return ResponseEntity.status(401).body("Invalid credentials");
    }
}
}