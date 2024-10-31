package ru.mtuci.demo.auth;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class SignInRequest {
    private String email;
    private String password;
}
