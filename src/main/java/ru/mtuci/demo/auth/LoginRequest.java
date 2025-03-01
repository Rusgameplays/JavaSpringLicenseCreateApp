package ru.mtuci.demo.auth;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class LoginRequest {
    private String email;
    private String password;
    private String deviceId;
}
