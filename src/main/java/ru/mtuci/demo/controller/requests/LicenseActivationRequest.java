package ru.mtuci.demo.controller.requests;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class LicenseActivationRequest {
    private String key;
    private String name;
    private String mac;
    private Long userId;
}
