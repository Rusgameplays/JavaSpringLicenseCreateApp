package ru.mtuci.demo.auth;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class RefreshRequest {
    private String refreshToken;
}
