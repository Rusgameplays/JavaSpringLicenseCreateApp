package ru.mtuci.demo.controller.requests;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class DeviceRequest {
    private String name;
    private String mac;
    private Long userId;
}

