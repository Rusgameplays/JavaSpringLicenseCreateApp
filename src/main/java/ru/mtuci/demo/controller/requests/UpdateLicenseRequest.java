package ru.mtuci.demo.controller.requests;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class UpdateLicenseRequest {
    private String licenseKey;
    private String deviceMac;

}