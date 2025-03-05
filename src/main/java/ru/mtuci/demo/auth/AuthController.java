package ru.mtuci.demo.auth;

import java.util.HashSet;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;


import lombok.RequiredArgsConstructor;
import ru.mtuci.demo.configuration.JwtTokenProvider;
import ru.mtuci.demo.services.UserService;
import ru.mtuci.demo.exception.UserAlreadyCreate;


@RequiredArgsConstructor
@RequestMapping("/auth")
@RestController
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtProvider;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            String accessToken = jwtProvider.createAccessToken(request.getEmail(),
                    new HashSet<>(authentication.getAuthorities()));

            String refreshToken = jwtProvider.createRefreshToken(request.getEmail(), request.getDeviceId(),new HashSet<>(authentication.getAuthorities()));

            return ResponseEntity.ok(new LoginResponse(request.getEmail(), accessToken, refreshToken));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect email or password");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Authentication failed");
        }
    }


    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reg")
    public ResponseEntity<?> register(@RequestBody RegRequest request) {
        try {
            userService.create(request.getEmail(), request.getName(), request.getPassword());
            return ResponseEntity.ok("Successful");
        } catch (UserAlreadyCreate ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ex.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        try {
            String newAccessToken = jwtProvider.refreshAccessToken(request.getRefreshToken());
            return ResponseEntity.ok(newAccessToken);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
        }
    }


}