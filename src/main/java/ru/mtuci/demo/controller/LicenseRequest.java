package ru.mtuci.demo.controller;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
class LicenseRequest {
    private Long productId;
    private Long ownerId;
    private Long licenseTypeId;

}
