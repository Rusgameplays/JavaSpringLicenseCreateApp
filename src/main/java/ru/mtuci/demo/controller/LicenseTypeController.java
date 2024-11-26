package ru.mtuci.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.mtuci.demo.model.LicenseType;
import ru.mtuci.demo.services.LicenseTypeService;

import java.util.List;
@RequiredArgsConstructor
@RestController
@RequestMapping("/license-types")
public class LicenseTypeController {

    private final LicenseTypeService licenseTypeService;

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/add")
    public ResponseEntity<String> addLicenseType(@RequestBody LicenseType licenseType) {
        try {
            LicenseType createdLicenseType = licenseTypeService.addLicenseType(licenseType);
            return ResponseEntity.ok("Тип лицензии успешно создан с ID: " + createdLicenseType.getId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Ошибка: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при создании типа лицензии: " + e.getMessage());
        }
    }
}
